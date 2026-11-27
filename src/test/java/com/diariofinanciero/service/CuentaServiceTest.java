package com.diariofinanciero.service;

import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.TipoCuenta;
import com.diariofinanciero.repository.CuentaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuentaServiceTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @InjectMocks
    private CuentaService cuentaService;

    @Test
    @DisplayName("Debe listar todas las cuentas y filtrar por tipo")
    void listarYFiltrar() {
        List<Cuenta> todas = List.of(new Cuenta("Efectivo", TipoCuenta.EFECTIVO));
        List<Cuenta> creditos = List.of(new Cuenta("Nu", TipoCuenta.CREDITO, 15, 5));
        when(cuentaRepository.findAllByOrderByNombreAsc()).thenReturn(todas);
        when(cuentaRepository.findByTipoOrderByNombreAsc(TipoCuenta.CREDITO)).thenReturn(creditos);

        assertEquals(todas, cuentaService.listarTodas());
        assertEquals(creditos, cuentaService.listarPorTipo(TipoCuenta.CREDITO));
    }

    @Test
    @DisplayName("Debe obtener cuenta por id o lanzar excepción si es null o no existe")
    void obtenerPorId() {
        Cuenta cuenta = new Cuenta("Efectivo", TipoCuenta.EFECTIVO);
        when(cuentaRepository.findById(1L)).thenReturn(Optional.of(cuenta));
        when(cuentaRepository.findById(99L)).thenReturn(Optional.empty());

        assertEquals(cuenta, cuentaService.obtenerPorId(1L));
        assertThrows(IllegalArgumentException.class, () -> cuentaService.obtenerPorId(null));
        assertThrows(RecursoNoEncontradoException.class, () -> cuentaService.obtenerPorId(99L));
    }

    @Test
    @DisplayName("Debe crear tarjeta de crédito y validar días obligatorios y nombre único")
    void crearTarjetaCredito() {
        assertThrows(IllegalArgumentException.class, () -> cuentaService.crearTarjetaCredito("Nu", null, 5));
        assertThrows(IllegalArgumentException.class, () -> cuentaService.crearTarjetaCredito("Nu", 15, null));

        when(cuentaRepository.findByNombreIgnoreCase("Nu")).thenReturn(Optional.empty());
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(inv -> inv.getArgument(0));

        Cuenta creada = cuentaService.crearTarjetaCredito("  Nu  ", 15, 5);
        assertEquals("Nu", creada.getNombre());
        assertEquals(TipoCuenta.CREDITO, creada.getTipo());

        when(cuentaRepository.findByNombreIgnoreCase("Nu")).thenReturn(Optional.of(creada));
        assertThrows(IllegalArgumentException.class, () -> cuentaService.crearTarjetaCredito("Nu", 15, 5));
    }

    @Test
    @DisplayName("Debe actualizar tarjeta permitiendo mismo nombre pero rechazando duplicado de otra cuenta")
    void actualizarTarjetaCredito() {
        Cuenta tarjeta = new Cuenta("Oro", TipoCuenta.CREDITO, 10, 25);
        ReflectionTestUtils.setField(tarjeta, "id", 1L);

        Cuenta otra = new Cuenta("Platino", TipoCuenta.CREDITO, 20, 5);
        ReflectionTestUtils.setField(otra, "id", 2L);

        when(cuentaRepository.findById(1L)).thenReturn(Optional.of(tarjeta));
        when(cuentaRepository.findByNombreIgnoreCase("Oro")).thenReturn(Optional.of(tarjeta));
        when(cuentaRepository.save(tarjeta)).thenReturn(tarjeta);

        Cuenta actualizada = cuentaService.actualizarTarjetaCredito(1L, " Oro ", 12, 27);
        assertEquals(12, actualizada.getDiaCorte());
        assertEquals(27, actualizada.getDiaPago());
        verify(cuentaRepository).save(tarjeta);

        when(cuentaRepository.findByNombreIgnoreCase("Platino")).thenReturn(Optional.of(otra));
        assertThrows(IllegalArgumentException.class, () -> cuentaService.actualizarTarjetaCredito(1L, "Platino", 12, 27));
    }
}
