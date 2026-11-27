package com.diariofinanciero.service;

import com.diariofinanciero.domain.Categoria;
import com.diariofinanciero.domain.TipoMovimiento;
import com.diariofinanciero.repository.GastoDiarioProjection;
import com.diariofinanciero.repository.GastoPorCategoriaProjection;
import com.diariofinanciero.repository.MovimientoRepository;
import com.diariofinanciero.repository.ResumenMensualProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraficaServiceTest {

    @Mock
    private MovimientoRepository movimientoRepository;

    @Mock
    private CategoriaService categoriaService;

    @InjectMocks
    private GraficaService graficaService;

    @Test
    @DisplayName("Debe normalizar la ventana de meses a 6 por defecto o 12 si se solicita 12")
    void normalizarVentanaMeses() {
        assertEquals(6, graficaService.normalizarVentanaMeses(6));
        assertEquals(6, graficaService.normalizarVentanaMeses(3));
        assertEquals(12, graficaService.normalizarVentanaMeses(12));
    }

    @Test
    @DisplayName("Debe calcular dona por categoría, barras mensuales y serie diaria acumulada para mes pasado")
    void obtenerDatosMesPasado() {
        YearMonth mes = YearMonth.of(2025, 4);
        Categoria comida = new Categoria("Comida", new BigDecimal("1000.00"));
        ReflectionTestUtils.setField(comida, "id", 1L);
        Categoria transporte = new Categoria("Transporte");
        ReflectionTestUtils.setField(transporte, "id", 2L);
        when(categoriaService.listarTodas()).thenReturn(List.of(comida, transporte));

        GastoPorCategoriaProjection cat1 = mock(GastoPorCategoriaProjection.class);
        when(cat1.getCategoriaId()).thenReturn(1L);
        when(cat1.getTotal()).thenReturn(new BigDecimal("300.00"));

        GastoPorCategoriaProjection catDesconocida = mock(GastoPorCategoriaProjection.class);
        when(catDesconocida.getCategoriaId()).thenReturn(99L);
        when(catDesconocida.getTotal()).thenReturn(new BigDecimal("100.00"));

        when(movimientoRepository.sumarPorCategoriaEnPeriodo(TipoMovimiento.GASTO, mes.atDay(1), mes.atEndOfMonth()))
                .thenReturn(List.of(catDesconocida, cat1));

        ResumenMensualProjection ingMes = mock(ResumenMensualProjection.class);
        when(ingMes.getAnio()).thenReturn(2025);
        when(ingMes.getMes()).thenReturn(4);
        when(ingMes.getTipo()).thenReturn(TipoMovimiento.INGRESO);
        when(ingMes.getTotal()).thenReturn(new BigDecimal("2500.00"));

        ResumenMensualProjection gasMes = mock(ResumenMensualProjection.class);
        when(gasMes.getAnio()).thenReturn(2025);
        when(gasMes.getMes()).thenReturn(4);
        when(gasMes.getTipo()).thenReturn(TipoMovimiento.GASTO);
        when(gasMes.getTotal()).thenReturn(new BigDecimal("400.00"));

        when(movimientoRepository.sumarPorMesYTipo(mes.minusMonths(5).atDay(1), mes.atEndOfMonth()))
                .thenReturn(List.of(ingMes, gasMes));

        GastoDiarioProjection dia1 = mock(GastoDiarioProjection.class);
        when(dia1.getFecha()).thenReturn(LocalDate.of(2025, 4, 1));
        when(dia1.getTotal()).thenReturn(new BigDecimal("150.00"));

        when(movimientoRepository.sumarPorDiaEnPeriodo(TipoMovimiento.GASTO, mes.atDay(1), mes.atEndOfMonth()))
                .thenReturn(List.of(dia1));

        DatosGraficasMes datos = graficaService.obtenerDatos(mes, 6);

        assertEquals("2025-04", datos.mes());
        assertEquals(new BigDecimal("2500.00"), datos.totalIngresosMes());
        assertEquals(new BigDecimal("400.00"), datos.totalGastosMes());
        assertEquals(new BigDecimal("1000.00"), datos.presupuestoTotalMes());

        assertEquals(2, datos.gastoPorCategoria().size());
        assertEquals("Comida", datos.gastoPorCategoria().get(0).categoria());
        assertEquals(new BigDecimal("75.0"), datos.gastoPorCategoria().get(0).porcentaje());
        assertEquals("Sin categoría", datos.gastoPorCategoria().get(1).categoria());
        assertEquals(new BigDecimal("25.0"), datos.gastoPorCategoria().get(1).porcentaje());

        assertEquals(6, datos.ingresosVsGastosMensual().size());
        assertEquals(30, datos.gastoAcumuladoDiario().size());
        assertEquals(new BigDecimal("150.00"), datos.gastoAcumuladoDiario().get(0).acumulado());
        assertEquals(new BigDecimal("150.00"), datos.gastoAcumuladoDiario().get(29).acumulado());
    }

    @Test
    @DisplayName("Debe dejar acumulado en null para días futuros sin gasto cuando no hay presupuesto")
    void obtenerDatosMesFuturoSinPresupuesto() {
        YearMonth mesFuturo = YearMonth.now().plusMonths(2);
        when(categoriaService.listarTodas()).thenReturn(List.of());
        when(movimientoRepository.sumarPorCategoriaEnPeriodo(TipoMovimiento.GASTO, mesFuturo.atDay(1), mesFuturo.atEndOfMonth()))
                .thenReturn(List.of());
        when(movimientoRepository.sumarPorMesYTipo(mesFuturo.minusMonths(11).atDay(1), mesFuturo.atEndOfMonth()))
                .thenReturn(List.of());
        when(movimientoRepository.sumarPorDiaEnPeriodo(TipoMovimiento.GASTO, mesFuturo.atDay(1), mesFuturo.atEndOfMonth()))
                .thenReturn(List.of());

        DatosGraficasMes datos = graficaService.obtenerDatos(mesFuturo, 12);

        assertEquals(12, datos.ingresosVsGastosMensual().size());
        assertNull(datos.gastoAcumuladoDiario().get(0).acumulado());
        assertNull(datos.gastoAcumuladoDiario().get(0).presupuestoTotal());
    }
}
