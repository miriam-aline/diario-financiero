package com.diariofinanciero.service;

import com.diariofinanciero.domain.Categoria;
import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.Movimiento;
import com.diariofinanciero.domain.TipoCuenta;
import com.diariofinanciero.domain.TipoMovimiento;
import com.diariofinanciero.repository.MovimientoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TarjetaServiceTest {

    @Mock
    private CuentaService cuentaService;

    @Mock
    private MovimientoRepository movimientoRepository;

    @InjectMocks
    private TarjetaService tarjetaService;

    @Test
    @DisplayName("Debe omitir tarjetas sin fechas, calcular ciclos y priorizar alerta del ciclo cerrado próximo a pago")
    void resumenYAlertasPriorizandoCicloAnterior() {
        Cuenta sinFechas = new Cuenta("Sin Fechas", TipoCuenta.CREDITO, null, null);
        Cuenta tarjeta = new Cuenta("Tarjeta Oro", TipoCuenta.CREDITO, 15, 20);
        ReflectionTestUtils.setField(tarjeta, "id", 1L);

        when(cuentaService.listarPorTipo(TipoCuenta.CREDITO)).thenReturn(List.of(sinFechas, tarjeta));

        LocalDate hoy = LocalDate.of(2026, 5, 18); // Ciclo actual: 16 may - 15 jun (pago 20 jun); anterior: 16 abr - 15 may (pago 20 may -> 2 días)
        Categoria compras = new Categoria("Compras");
        Movimiento cargoActual = Movimiento.manual(LocalDate.of(2026, 5, 17), new BigDecimal("500.00"), TipoMovimiento.GASTO, compras, tarjeta, "Tienda");
        Movimiento cargoAnterior = Movimiento.manual(LocalDate.of(2026, 5, 10), new BigDecimal("1200.00"), TipoMovimiento.GASTO, compras, tarjeta, "Vuelo");
        Movimiento abonoAnterior = Movimiento.manual(LocalDate.of(2026, 5, 12), new BigDecimal("200.00"), TipoMovimiento.INGRESO, compras, tarjeta, "Reembolso");

        when(movimientoRepository.buscarPorCuentaEnPeriodo(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(movimientoRepository.buscarPorCuentaEnPeriodo(1L, LocalDate.of(2026, 4, 16), LocalDate.of(2026, 5, 15)))
                .thenReturn(List.of(cargoAnterior, abonoAnterior));
        when(movimientoRepository.buscarPorCuentaEnPeriodo(1L, LocalDate.of(2026, 5, 16), LocalDate.of(2026, 6, 15)))
                .thenReturn(List.of(cargoActual));

        List<ResumenTarjeta> resumenes = tarjetaService.obtenerResumenTarjetas(hoy, 5);
        assertEquals(1, resumenes.size());

        ResumenTarjeta resumen = resumenes.get(0);
        assertEquals(4, resumen.historialCiclos().size());
        assertEquals(new BigDecimal("500.00"), resumen.cicloActual().estimadoAPagar());
        assertEquals(new BigDecimal("1000.00"), resumen.cicloAnterior().estimadoAPagar());
        assertNotNull(resumen.proximoPagoAlerta());
        assertTrue(resumen.proximoPagoAlerta().cicloCerrado());
        assertEquals(2L, resumen.proximoPagoAlerta().diasParaPago());

        List<AlertaPagoTarjeta> alertas = tarjetaService.obtenerAlertasPagoProximo(hoy, 5);
        assertEquals(1, alertas.size());
        assertEquals(new BigDecimal("1000.00"), alertas.get(0).estimadoAPagar());
        assertTrue(alertas.get(0).esCicloCerrado());

        assertEquals(new BigDecimal("500.00"), tarjetaService.calcularTotalEstimadoProximoCorte(resumenes));
    }

    @Test
    @DisplayName("Debe dejar estimado en cero si abonos superan cargos y detectar alerta del ciclo actual cuando pago está próximo")
    void abonosSuperanCargosYAlertaCicloActual() {
        Cuenta tarjeta = new Cuenta("Nu", TipoCuenta.CREDITO, 15, 18);
        ReflectionTestUtils.setField(tarjeta, "id", 2L);
        when(cuentaService.listarPorTipo(TipoCuenta.CREDITO)).thenReturn(List.of(tarjeta));

        LocalDate hoy = LocalDate.of(2026, 5, 14); // Ciclo actual: 16 abr - 15 may, pago 18 may (4 días <= 5)
        Categoria compras = new Categoria("Compras");
        Movimiento cargo = Movimiento.manual(LocalDate.of(2026, 5, 12), new BigDecimal("100.00"), TipoMovimiento.GASTO, compras, tarjeta, "Compra");
        Movimiento abono = Movimiento.manual(LocalDate.of(2026, 5, 13), new BigDecimal("300.00"), TipoMovimiento.INGRESO, compras, tarjeta, "Pago extra");

        when(movimientoRepository.buscarPorCuentaEnPeriodo(eq(2L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(movimientoRepository.buscarPorCuentaEnPeriodo(2L, LocalDate.of(2026, 4, 16), LocalDate.of(2026, 5, 15)))
                .thenReturn(List.of(cargo, abono));

        List<ResumenTarjeta> resumenes = tarjetaService.obtenerResumenTarjetas(hoy, 5);
        ResumenTarjeta r = resumenes.get(0);

        assertEquals(new BigDecimal("0.00"), r.cicloActual().estimadoAPagar());
        assertTrue(r.cicloActual().pagoProximo());
        assertFalse(r.cicloActual().pagoHoy());
        assertFalse(r.cicloActual().cicloCerrado());
        assertEquals(r.cicloActual(), r.proximoPagoAlerta());

        // El día exacto de pago del ciclo cerrado (2026-05-18) marca pagoHoy = true en el ciclo anterior
        List<ResumenTarjeta> elDiaDePago = tarjetaService.obtenerResumenTarjetas(LocalDate.of(2026, 5, 18), 5);
        assertTrue(elDiaDePago.get(0).cicloAnterior().pagoHoy());

        // Con ventana 2 el 2026-05-01 no hay alerta próxima
        List<AlertaPagoTarjeta> sinAlertas = tarjetaService.obtenerAlertasPagoProximo(LocalDate.of(2026, 5, 1), 2);
        assertTrue(sinAlertas.isEmpty());
        assertNull(tarjetaService.obtenerResumenTarjetas(LocalDate.of(2026, 5, 1), 2).get(0).proximoPagoAlerta());
    }
}
