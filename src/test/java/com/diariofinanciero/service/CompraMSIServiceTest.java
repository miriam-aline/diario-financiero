package com.diariofinanciero.service;

import com.diariofinanciero.domain.CompraMSI;
import com.diariofinanciero.domain.Categoria;
import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.MensualidadMSI;
import com.diariofinanciero.domain.Movimiento;
import com.diariofinanciero.domain.TipoCuenta;
import com.diariofinanciero.repository.CategoriaRepository;
import com.diariofinanciero.repository.CompraMSIRepository;
import com.diariofinanciero.repository.MovimientoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompraMSIServiceTest {

    @Mock
    private CompraMSIRepository compraMSIRepository;
    @Mock
    private CuentaService cuentaService;
    @Mock
    private CategoriaService categoriaService;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private MovimientoRepository movimientoRepository;

    @InjectMocks
    private CompraMSIService compraMSIService;

    private Cuenta tarjeta;
    private Categoria tecnologia;
    private Categoria sinCategoria;

    @BeforeEach
    void setUp() {
        tarjeta = new Cuenta("TC Oro", TipoCuenta.CREDITO, 15, 5);
        tecnologia = new Categoria("Tecnología");
        sinCategoria = new Categoria("Sin categoría");
    }

    @Test
    @DisplayName("Debe listar todas las compras MSI")
    void listarTodas() {
        List<CompraMSI> lista = List.of();
        when(compraMSIRepository.findAllConRelaciones()).thenReturn(lista);
        assertEquals(lista, compraMSIService.listarTodas());
    }

    @Test
    @DisplayName("Debe crear compra MSI con o sin categoría")
    void crear() {
        when(cuentaService.obtenerPorId(1L)).thenReturn(tarjeta);
        when(categoriaService.obtenerPorId(2L)).thenReturn(tecnologia);
        when(compraMSIRepository.save(any(CompraMSI.class))).thenAnswer(inv -> inv.getArgument(0));

        CompraMSI conCategoria = compraMSIService.crear(
                "Laptop", new BigDecimal("12000"), 6, 0, 1L, 2L, LocalDate.of(2026, 3, 1)
        );
        assertEquals("Laptop", conCategoria.getDescripcion());
        assertEquals(tecnologia, conCategoria.getCategoria());

        CompraMSI sinCat = compraMSIService.crear(
                "Silla", new BigDecimal("3000"), 3, 0, 1L, null, LocalDate.of(2026, 3, 1)
        );
        assertEquals(null, sinCat.getCategoria());
    }

    @Test
    @DisplayName("Debe registrar pago sin crear movimiento cuando registrarMovimiento es false")
    void registrarMensualidadSinMovimiento() {
        CompraMSI compra = new CompraMSI("Monitor", new BigDecimal("300.00"), 3, 0, tarjeta, tecnologia, LocalDate.of(2026, 3, 10));
        ReflectionTestUtils.setField(compra, "id", 10L);
        when(compraMSIRepository.findByIdConRelaciones(10L)).thenReturn(Optional.of(compra));

        MensualidadMSI cuota = compraMSIService.registrarSiguienteMensualidad(10L, false);

        assertEquals(1, cuota.numero());
        assertEquals(1, compra.getMensualidadesPagadas());
        verify(compraMSIRepository).save(compra);
        verify(movimientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe registrar pago y generar movimiento idempotente usando Sin categoría si la compra no tiene categoría")
    void registrarMensualidadConMovimiento() {
        String descLarga = "x".repeat(250);
        CompraMSI compra = new CompraMSI(descLarga, new BigDecimal("300.00"), 3, 0, tarjeta, null, LocalDate.of(2026, 3, 10));
        ReflectionTestUtils.setField(compra, "id", 11L);

        when(compraMSIRepository.findByIdConRelaciones(11L)).thenReturn(Optional.of(compra));
        when(categoriaRepository.findByNombreIgnoreCase(ImportacionCsvService.NOMBRE_SIN_CATEGORIA))
                .thenReturn(Optional.of(sinCategoria));
        when(movimientoRepository.findHashesExistentes(Set.of("MSI-11-1"))).thenReturn(Set.of());

        compraMSIService.registrarSiguienteMensualidad(11L, true);

        ArgumentCaptor<Movimiento> captor = ArgumentCaptor.forClass(Movimiento.class);
        verify(movimientoRepository).save(captor.capture());
        Movimiento guardado = captor.getValue();
        assertEquals(255, guardado.getDescripcion().length());
        assertEquals("MSI-11-1", guardado.getHashImportacion());
        assertEquals(sinCategoria, guardado.getCategoria());
    }

    @Test
    @DisplayName("Debe omitir movimiento duplicado y lanzar excepción si la compra no existe")
    void registrarMensualidadDuplicadaONoEncontrada() {
        CompraMSI compra = new CompraMSI("TV", new BigDecimal("600.00"), 3, 0, tarjeta, tecnologia, LocalDate.of(2026, 3, 10));
        ReflectionTestUtils.setField(compra, "id", 12L);
        when(compraMSIRepository.findByIdConRelaciones(12L)).thenReturn(Optional.of(compra));
        when(movimientoRepository.findHashesExistentes(Set.of("MSI-12-1"))).thenReturn(Set.of("MSI-12-1"));

        compraMSIService.registrarSiguienteMensualidad(12L, true);
        verify(movimientoRepository, never()).save(any());

        when(compraMSIRepository.findByIdConRelaciones(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> compraMSIService.registrarSiguienteMensualidad(99L, true));
    }

    @Test
    @DisplayName("Debe eliminar compra existente y fallar si no existe")
    void eliminar() {
        CompraMSI compra = new CompraMSI("TV", new BigDecimal("600.00"), 3, 0, tarjeta, tecnologia, LocalDate.of(2026, 3, 10));
        when(compraMSIRepository.findById(5L)).thenReturn(Optional.of(compra));
        when(compraMSIRepository.findById(99L)).thenReturn(Optional.empty());

        compraMSIService.eliminar(5L);
        verify(compraMSIRepository).delete(compra);

        assertThrows(RecursoNoEncontradoException.class, () -> compraMSIService.eliminar(99L));
    }

    @Test
    @DisplayName("Debe calcular saldo pendiente total y compromiso mensual actual")
    void calculosAgregados() {
        CompraMSI activa = new CompraMSI("A", new BigDecimal("100.00"), 3, 1, tarjeta, null, LocalDate.of(2026, 1, 1));
        CompraMSI liquidada = new CompraMSI("B", new BigDecimal("200.00"), 2, 2, tarjeta, null, LocalDate.of(2026, 1, 1));

        List<CompraMSI> compras = List.of(activa, liquidada);

        assertEquals(new BigDecimal("66.66"), compraMSIService.calcularSaldoPendienteTotal(compras));
        assertEquals(new BigDecimal("33.33"), compraMSIService.calcularCompromisoMensualActual(compras));
    }
}
