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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovimientoServiceTest {

    @Mock
    private MovimientoRepository movimientoRepository;
    @Mock
    private CategoriaService categoriaService;
    @Mock
    private CuentaService cuentaService;

    @InjectMocks
    private MovimientoService movimientoService;

    private final Categoria comida = new Categoria("Comida");
    private final Cuenta efectivo = new Cuenta("Efectivo", TipoCuenta.EFECTIVO);
    private final LocalDate fecha = LocalDate.of(2026, 5, 10);

    @Test
    @DisplayName("Debe listar usando FiltroMovimiento.vacio cuando el filtro es null")
    void listar() {
        List<Movimiento> esperados = List.of();
        when(movimientoRepository.buscarConFiltros(null, null, null, null)).thenReturn(esperados);

        assertEquals(esperados, movimientoService.listar(null));

        FiltroMovimiento filtro = new FiltroMovimiento(fecha, fecha.plusDays(1), 2L, "Cafe");
        when(movimientoRepository.buscarConFiltros(fecha, fecha.plusDays(1), 2L, "%cafe%")).thenReturn(esperados);
        assertEquals(esperados, movimientoService.listar(filtro));
    }

    @Test
    @DisplayName("Debe obtener movimiento por id o lanzar excepción si es null o no existe")
    void obtenerPorId() {
        Movimiento mov = Movimiento.manual(fecha, BigDecimal.TEN, TipoMovimiento.GASTO, comida, efectivo, "Tacos");
        when(movimientoRepository.findByIdConRelaciones(1L)).thenReturn(Optional.of(mov));
        when(movimientoRepository.findByIdConRelaciones(99L)).thenReturn(Optional.empty());

        assertEquals(mov, movimientoService.obtenerPorId(1L));
        assertThrows(IllegalArgumentException.class, () -> movimientoService.obtenerPorId(null));
        assertThrows(RecursoNoEncontradoException.class, () -> movimientoService.obtenerPorId(99L));
    }

    @Test
    @DisplayName("Debe registrar, actualizar, eliminar y calcular balance")
    void registrarActualizarEliminarYBalance() {
        when(categoriaService.obtenerPorId(1L)).thenReturn(comida);
        when(cuentaService.obtenerPorId(2L)).thenReturn(efectivo);
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(inv -> inv.getArgument(0));

        Movimiento registrado = movimientoService.registrar(
                fecha, new BigDecimal("120.5"), TipoMovimiento.GASTO, 1L, 2L, "Desayuno"
        );
        assertEquals(new BigDecimal("120.50"), registrado.getMonto());
        assertEquals("Desayuno", registrado.getDescripcion());

        when(movimientoRepository.findByIdConRelaciones(10L)).thenReturn(Optional.of(registrado));
        Movimiento actualizado = movimientoService.actualizar(
                10L, fecha.plusDays(1), new BigDecimal("200"), TipoMovimiento.INGRESO, 1L, 2L, "Reembolso"
        );
        assertEquals(TipoMovimiento.INGRESO, actualizado.getTipo());
        assertEquals(new BigDecimal("200.00"), actualizado.getMonto());

        movimientoService.eliminar(10L);
        verify(movimientoRepository).delete(registrado);

        BalancePeriodo balance = movimientoService.calcularBalance(List.of(actualizado));
        assertEquals(new BigDecimal("200.00"), balance.totalIngresos());
        assertEquals(new BigDecimal("200.00"), balance.balance());
    }
}
