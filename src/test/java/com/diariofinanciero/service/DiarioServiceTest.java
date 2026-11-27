package com.diariofinanciero.service;

import com.diariofinanciero.domain.Categoria;
import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.EntradaDiario;
import com.diariofinanciero.domain.Movimiento;
import com.diariofinanciero.domain.TipoCuenta;
import com.diariofinanciero.domain.TipoMovimiento;
import com.diariofinanciero.repository.EntradaDiarioRepository;
import com.diariofinanciero.repository.MovimientoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiarioServiceTest {

    @Mock
    private EntradaDiarioRepository entradaDiarioRepository;

    @Mock
    private MovimientoRepository movimientoRepository;

    @InjectMocks
    private DiarioService diarioService;

    @Test
    @DisplayName("Debe crear nueva entrada o actualizar la existente para la misma fecha")
    void guardarEntrada() {
        assertThrows(IllegalArgumentException.class, () -> diarioService.guardarEntrada(null, "Nota", 3, null));

        LocalDate fecha = LocalDate.of(2026, 5, 10);
        when(entradaDiarioRepository.findByFecha(fecha)).thenReturn(Optional.empty());
        when(entradaDiarioRepository.save(any(EntradaDiario.class))).thenAnswer(inv -> inv.getArgument(0));

        EntradaDiario nueva = diarioService.guardarEntrada(fecha, "Primera nota", 4, "ahorro");
        assertEquals("Primera nota", nueva.getNota());

        when(entradaDiarioRepository.findByFecha(fecha)).thenReturn(Optional.of(nueva));
        EntradaDiario actualizada = diarioService.guardarEntrada(fecha, "Nota editada", 5, "meta");
        assertEquals("Nota editada", actualizada.getNota());
        assertEquals(5, actualizada.getEstadoDeAnimo());
        assertEquals("meta", actualizada.getEtiquetas());
    }

    @Test
    @DisplayName("Debe obtener por fecha, por id y eliminar")
    void obtenerYEliminar() {
        assertTrue(diarioService.obtenerPorFecha(null).isEmpty());

        LocalDate fecha = LocalDate.of(2026, 5, 10);
        EntradaDiario entrada = new EntradaDiario(fecha, "Nota", 3, null);
        when(entradaDiarioRepository.findByFecha(fecha)).thenReturn(Optional.of(entrada));
        assertEquals(Optional.of(entrada), diarioService.obtenerPorFecha(fecha));

        when(entradaDiarioRepository.findById(1L)).thenReturn(Optional.of(entrada));
        when(entradaDiarioRepository.findById(99L)).thenReturn(Optional.empty());
        assertEquals(entrada, diarioService.obtenerPorId(1L));
        assertThrows(IllegalArgumentException.class, () -> diarioService.obtenerPorId(null));
        assertThrows(RecursoNoEncontradoException.class, () -> diarioService.obtenerPorId(99L));

        diarioService.eliminar(1L);
        verify(entradaDiarioRepository).delete(entrada);
    }

    @Test
    @DisplayName("Debe construir resumen semanal con totales, categoría más alta y días sin gasto")
    void obtenerResumenSemanal() {
        LocalDate lunes = LocalDate.of(2026, 5, 4);
        LocalDate domingo = LocalDate.of(2026, 5, 10);
        Categoria comida = new Categoria("Comida");
        Categoria transporte = new Categoria("Transporte");
        Cuenta efectivo = new Cuenta("Efectivo", TipoCuenta.EFECTIVO);

        Movimiento m1 = Movimiento.manual(lunes, new BigDecimal("100.00"), TipoMovimiento.GASTO, comida, efectivo, "Comida");
        Movimiento m2 = Movimiento.manual(lunes.plusDays(2), new BigDecimal("250.00"), TipoMovimiento.GASTO, comida, efectivo, "Cena");
        Movimiento m3 = Movimiento.manual(lunes.plusDays(2), new BigDecimal("50.00"), TipoMovimiento.GASTO, transporte, efectivo, "Taxi");
        Movimiento m4 = Movimiento.manual(lunes.plusDays(3), new BigDecimal("1000.00"), TipoMovimiento.INGRESO, comida, efectivo, "Bono");

        EntradaDiario nota = new EntradaDiario(lunes.plusDays(2), "Buen día", 4, "ahorro");

        when(movimientoRepository.buscarConFiltros(lunes, domingo, null, null))
                .thenReturn(List.of(m1, m2, m3, m4));
        when(entradaDiarioRepository.findByFechaBetweenOrderByFechaDesc(lunes, domingo))
                .thenReturn(List.of(nota));

        ResumenSemanalDiario resumen = diarioService.obtenerResumenSemanal(LocalDate.of(2026, 5, 7));

        assertEquals(lunes, resumen.inicioSemana());
        assertEquals(domingo, resumen.finSemana());
        assertEquals(new BigDecimal("400.00"), resumen.totalGastado());
        assertEquals(new BigDecimal("1000.00"), resumen.totalIngresado());
        assertEquals("Comida", resumen.categoriaMasAlta());
        assertEquals(new BigDecimal("350.00"), resumen.montoCategoriaMasAlta());
        assertEquals(7, resumen.diasEvaluados());
        assertEquals(5, resumen.diasSinGasto());
        assertEquals(1, resumen.notasSemana().size());
        assertEquals(3, resumen.dias().size());
    }

    @Test
    @DisplayName("Debe buscar entradas combinando texto, etiqueta y sus movimientos")
    void buscarEntradas() {
        when(entradaDiarioRepository.buscar("%cafe%", "%,ahorro,%")).thenReturn(List.of());
        assertTrue(diarioService.buscarEntradas("  Cafe ", "#Ahorro, otro").isEmpty());

        LocalDate fecha = LocalDate.of(2026, 5, 10);
        EntradaDiario entrada = new EntradaDiario(fecha, "Café con amigos", 5, "ahorro");
        Categoria comida = new Categoria("Comida");
        Cuenta efectivo = new Cuenta("Efectivo", TipoCuenta.EFECTIVO);
        Movimiento gasto = Movimiento.manual(fecha, new BigDecimal("80.00"), TipoMovimiento.GASTO, comida, efectivo, "Café");

        when(entradaDiarioRepository.buscar("%cafe%", null)).thenReturn(List.of(entrada));
        when(movimientoRepository.buscarConFiltros(fecha, fecha, null, null)).thenReturn(List.of(gasto));

        List<DiaDiario> resultados = diarioService.buscarEntradas("cafe", null);
        assertEquals(1, resultados.size());
        assertEquals(new BigDecimal("80.00"), resultados.get(0).totalGastos());
        assertEquals(new BigDecimal("-80.00"), resultados.get(0).balanceDia());
    }

    @Test
    @DisplayName("Debe listar etiquetas únicas ordenadas alfabéticamente")
    void listarEtiquetasUnicas() {
        when(entradaDiarioRepository.findAllByOrderByFechaDesc()).thenReturn(List.of(
                new EntradaDiario(LocalDate.of(2026, 5, 2), "A", 3, "viaje,ahorro"),
                new EntradaDiario(LocalDate.of(2026, 5, 1), "B", 4, "ahorro,comida")
        ));

        assertEquals(List.of("ahorro", "comida", "viaje"), diarioService.listarEtiquetasUnicas());
    }
}
