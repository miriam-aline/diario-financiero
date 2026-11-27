package com.diariofinanciero.service;

import com.diariofinanciero.domain.Categoria;
import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.Movimiento;
import com.diariofinanciero.domain.Periodicidad;
import com.diariofinanciero.domain.Recurrente;
import com.diariofinanciero.domain.TipoCuenta;
import com.diariofinanciero.domain.TipoMovimiento;
import com.diariofinanciero.repository.MovimientoRepository;
import com.diariofinanciero.repository.RecurrenteRepository;
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
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurrenteServiceTest {

    @Mock
    private RecurrenteRepository recurrenteRepository;
    @Mock
    private MovimientoRepository movimientoRepository;
    @Mock
    private CategoriaService categoriaService;
    @Mock
    private CuentaService cuentaService;

    @InjectMocks
    private RecurrenteService recurrenteService;

    private final Categoria categoria = new Categoria("Servicios");
    private final Cuenta cuenta = new Cuenta("Débito", TipoCuenta.DEBITO);

    @Test
    @DisplayName("Debe listar, crear, alternar activo y eliminar recurrente")
    void crudBasico() {
        when(recurrenteRepository.findAllConRelaciones()).thenReturn(List.of());
        assertEquals(List.of(), recurrenteService.listarTodos());

        when(categoriaService.obtenerPorId(1L)).thenReturn(categoria);
        when(cuentaService.obtenerPorId(2L)).thenReturn(cuenta);
        when(recurrenteRepository.save(any(Recurrente.class))).thenAnswer(inv -> inv.getArgument(0));

        Recurrente creado = recurrenteService.crear(
                "Internet", new BigDecimal("500"), TipoMovimiento.GASTO, 1L, 2L,
                Periodicidad.MENSUAL, LocalDate.of(2026, 5, 1)
        );
        assertEquals("Internet", creado.getDescripcion());

        when(recurrenteRepository.findById(10L)).thenReturn(Optional.of(creado));
        recurrenteService.alternarActivo(10L);
        assertFalse(creado.isActivo());

        recurrenteService.eliminar(10L);
        verify(recurrenteRepository).delete(creado);

        assertThrows(IllegalArgumentException.class, () -> recurrenteService.alternarActivo(null));
        when(recurrenteRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> recurrenteService.eliminar(99L));
    }

    @Test
    @DisplayName("Debe generar múltiples ciclos vencidos omitiendo hashes ya existentes y avanzar proximoCobro")
    void generarMovimientosPendientes() {
        Recurrente recurrente = new Recurrente(
                "Renta", new BigDecimal("5000.00"), TipoMovimiento.GASTO, categoria, cuenta,
                Periodicidad.MENSUAL, LocalDate.of(2026, 1, 31)
        );
        ReflectionTestUtils.setField(recurrente, "id", 7L);

        LocalDate corte = LocalDate.of(2026, 3, 31);
        when(recurrenteRepository.findPendientesHasta(corte)).thenReturn(List.of(recurrente));
        when(movimientoRepository.findHashesExistentes(Set.of("REC-7-2026-01-31"))).thenReturn(Set.of("REC-7-2026-01-31"));
        when(movimientoRepository.findHashesExistentes(Set.of("REC-7-2026-02-28"))).thenReturn(Set.of());
        when(movimientoRepository.findHashesExistentes(Set.of("REC-7-2026-03-31"))).thenReturn(Set.of());

        int generados = recurrenteService.generarMovimientosPendientes(corte);

        assertEquals(2, generados);
        assertEquals(LocalDate.of(2026, 4, 30), recurrente.getProximoCobro());
        verify(movimientoRepository, times(2)).save(any(Movimiento.class));
        verify(recurrenteRepository).save(recurrente);
    }

    @Test
    @DisplayName("Debe ejecutar métodos programados diarios y al arrancar")
    void ejecucionesAutomaticas() {
        when(recurrenteRepository.findPendientesHasta(LocalDate.now())).thenReturn(List.of());

        recurrenteService.ejecutarProgramadoDiario();
        recurrenteService.ejecutarAlArrancar();

        verify(recurrenteRepository, times(2)).findPendientesHasta(LocalDate.now());
        verify(movimientoRepository, never()).save(any());
    }
}
