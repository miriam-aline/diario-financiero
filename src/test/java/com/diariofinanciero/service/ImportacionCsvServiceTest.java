package com.diariofinanciero.service;

import com.diariofinanciero.domain.Categoria;
import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.Movimiento;
import com.diariofinanciero.domain.ReglaCategoria;
import com.diariofinanciero.domain.TipoCuenta;
import com.diariofinanciero.domain.TipoMovimiento;
import com.diariofinanciero.repository.CategoriaRepository;
import com.diariofinanciero.repository.MovimientoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportacionCsvServiceTest {

    @Mock
    private CuentaService cuentaService;
    @Mock
    private CategoriaService categoriaService;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private MovimientoRepository movimientoRepository;
    @Mock
    private ReglaCategoriaService reglaCategoriaService;

    @InjectMocks
    private ImportacionCsvService importacionCsvService;

    private Cuenta cuenta;
    private Categoria sinCategoria;
    private Categoria comida;

    @BeforeEach
    void setUp() {
        cuenta = new Cuenta("Débito", TipoCuenta.DEBITO);
        ReflectionTestUtils.setField(cuenta, "id", 1L);

        sinCategoria = new Categoria(ImportacionCsvService.NOMBRE_SIN_CATEGORIA);
        ReflectionTestUtils.setField(sinCategoria, "id", 10L);

        comida = new Categoria("Comida");
        ReflectionTestUtils.setField(comida, "id", 20L);
    }

    @Test
    @DisplayName("Debe validar archivo vacío, binario, demasiado grande o formato de fecha inválido")
    void validarArchivoYConfiguracion() {
        ConfiguracionCsv config = new ConfiguracionCsv(1L, "Fecha", "Descripción", "Cargo", "Abono", "dd/MM/yyyy", ',', true);

        assertThrows(IllegalArgumentException.class, () -> importacionCsvService.generarVistaPrevia(null, config));
        assertThrows(IllegalArgumentException.class, () -> importacionCsvService.generarVistaPrevia(new byte[0], config));
        assertThrows(IllegalArgumentException.class, () ->
                importacionCsvService.generarVistaPrevia(new byte[(int) ImportacionCsvService.MAX_TAMANO_BYTES + 1], config));
        assertThrows(IllegalArgumentException.class, () ->
                importacionCsvService.generarVistaPrevia(new byte[]{'a', 0, 'b'}, config));

        when(cuentaService.obtenerPorId(1L)).thenReturn(cuenta);
        ConfiguracionCsv formatoMalo = new ConfiguracionCsv(1L, "Fecha", "Descripción", "Cargo", null, "formato-malo", ',', true);
        assertThrows(IllegalArgumentException.class, () ->
                importacionCsvService.generarVistaPrevia("a,b".getBytes(StandardCharsets.UTF_8), formatoMalo));
    }

    @Test
    @DisplayName("Debe generar vista previa con BOM, ocurrencias iguales, avisos, reglas y detección de duplicados")
    void generarVistaPreviaYConfirmar() {
        String csv = "\uFEFFFecha,Descripción,Cargo,Abono\n"
                + "24/11/2026,CAFETERIA CENTRO,\"$65.00\",\n"
                + "24/11/2026,CAFETERIA CENTRO,65.00,\n"
                + "25/11/2026,DEPOSITO NOMINA,,\"1,500.00 MXN\"\n"
                + ",,,\n"
                + "TOTAL,,130.00,1500.00\n"
                + "99/99/2026,FECHA MALA,10.00,\n"
                + "26/11/2026,,10.00,\n"
                + "26/11/2026,SIN MONTO,,\n";

        ReglaCategoria reglaCafe = new ReglaCategoria("cafeteria", comida);
        when(cuentaService.obtenerPorId(1L)).thenReturn(cuenta);
        when(categoriaRepository.findByNombreIgnoreCase(ImportacionCsvService.NOMBRE_SIN_CATEGORIA))
                .thenReturn(Optional.of(sinCategoria));
        when(reglaCategoriaService.listarTodas()).thenReturn(List.of(reglaCafe));
        when(reglaCategoriaService.buscarCoincidencia("CAFETERIA CENTRO", List.of(reglaCafe)))
                .thenReturn(Optional.of(reglaCafe));
        when(reglaCategoriaService.buscarCoincidencia("DEPOSITO NOMINA", List.of(reglaCafe)))
                .thenReturn(Optional.empty());

        // Simula que la primera ocurrencia del café ya existía en BD
        when(movimientoRepository.findHashesExistentes(anySet())).thenAnswer(inv -> {
            Set<String> hashes = inv.getArgument(0);
            String primero = hashes.stream().sorted().findFirst().orElse("");
            return Set.of(primero);
        });

        ConfiguracionCsv config = new ConfiguracionCsv(1L, "Fecha", "Descripción", "Cargo", "Abono", "dd/MM/yyyy", ',', true);
        VistaPreviaImportacion vista = importacionCsvService.generarVistaPrevia(csv.getBytes(StandardCharsets.UTF_8), config);

        assertEquals(8, vista.totalFilasLeidas());
        assertEquals(3, vista.filas().size());
        assertEquals(2, vista.totalNuevos());
        assertEquals(1, vista.totalDuplicados());
        assertEquals(5, vista.totalOmitidas());
        assertEquals(1, vista.totalSinCategoria());

        when(movimientoRepository.findHashesExistentes(anySet())).thenReturn(Set.of());
        when(categoriaService.obtenerPorId(10L)).thenReturn(sinCategoria);
        when(categoriaService.obtenerPorId(20L)).thenReturn(comida);

        ResultadoConfirmacionImportacion resultado = importacionCsvService.confirmarImportacion(vista.tokenConfirmacion());
        assertEquals(2, resultado.importados());
        assertEquals(1, resultado.omitidosDuplicados());
        assertEquals(1, resultado.pendientesSinCategoria());
        verify(movimientoRepository, times(2)).save(any(Movimiento.class));

        // Reusar token debe fallar
        assertThrows(IllegalArgumentException.class, () ->
                importacionCsvService.confirmarImportacion(vista.tokenConfirmacion()));
    }

    @Test
    @DisplayName("Debe soportar CSV sin encabezados y misma columna con signo para cargo y abono")
    void generarVistaPreviaSinEncabezadoYColumnaConSigno() {
        String csv = "24/11/2026;Tacos;-85.50\n25/11/2026;Reembolso;+40.00\n";
        when(cuentaService.obtenerPorId(1L)).thenReturn(cuenta);
        when(categoriaRepository.findByNombreIgnoreCase(ImportacionCsvService.NOMBRE_SIN_CATEGORIA))
                .thenReturn(Optional.of(sinCategoria));
        when(reglaCategoriaService.listarTodas()).thenReturn(List.of());
        when(reglaCategoriaService.buscarCoincidencia(any(), any())).thenReturn(Optional.empty());
        when(movimientoRepository.findHashesExistentes(anySet())).thenReturn(Set.of());

        ConfiguracionCsv config = new ConfiguracionCsv(1L, "1", "2", "3", "3", "dd/MM/yyyy", ';', false);
        VistaPreviaImportacion vista = importacionCsvService.generarVistaPrevia(csv.getBytes(StandardCharsets.UTF_8), config);

        assertEquals(2, vista.filas().size());
        assertEquals(TipoMovimiento.GASTO, vista.filas().get(0).tipo());
        assertEquals(new BigDecimal("85.50"), vista.filas().get(0).monto());
        assertEquals(TipoMovimiento.INGRESO, vista.filas().get(1).tipo());
        assertEquals(new BigDecimal("40.00"), vista.filas().get(1).monto());
    }

    @Test
    @DisplayName("Debe validar token inválido en confirmación")
    void confirmarTokenInvalido() {
        assertThrows(IllegalArgumentException.class, () -> importacionCsvService.confirmarImportacion(null));
        assertThrows(IllegalArgumentException.class, () -> importacionCsvService.confirmarImportacion(" "));
        assertThrows(IllegalArgumentException.class, () -> importacionCsvService.confirmarImportacion("desconocido"));
    }

    @Test
    @DisplayName("Debe listar pendientes y clasificar movimiento aplicando regla al resto de pendientes")
    void listarYClasificarPendientes() {
        Movimiento principal = new Movimiento(
                LocalDate.of(2026, 5, 1), new BigDecimal("50"), TipoMovimiento.GASTO,
                sinCategoria, cuenta, "CAFE CENTRAL", com.diariofinanciero.domain.OrigenMovimiento.IMPORTADO, "h1"
        );
        ReflectionTestUtils.setField(principal, "id", 100L);

        Movimiento otroCoincidente = new Movimiento(
                LocalDate.of(2026, 5, 2), new BigDecimal("60"), TipoMovimiento.GASTO,
                sinCategoria, cuenta, "CAFE CENTRAL SUR", com.diariofinanciero.domain.OrigenMovimiento.IMPORTADO, "h2"
        );
        ReflectionTestUtils.setField(otroCoincidente, "id", 101L);

        when(movimientoRepository.buscarPorCategoriaNombre(ImportacionCsvService.NOMBRE_SIN_CATEGORIA))
                .thenReturn(List.of(principal, otroCoincidente));
        assertEquals(2, importacionCsvService.listarPendientesRevision().size());

        when(movimientoRepository.findByIdConRelaciones(100L)).thenReturn(Optional.of(principal));
        when(categoriaService.obtenerPorId(20L)).thenReturn(comida);
        ReglaCategoria regla = new ReglaCategoria("cafe central", comida);
        when(reglaCategoriaService.guardarRegla("cafe central", 20L)).thenReturn(regla);

        int actualizados = importacionCsvService.clasificarPendiente(100L, 20L, true, "cafe central");

        assertEquals(2, actualizados);
        assertEquals(comida, principal.getCategoria());
        assertEquals(comida, otroCoincidente.getCategoria());
        verify(movimientoRepository, times(2)).save(any(Movimiento.class));

        when(movimientoRepository.findByIdConRelaciones(999L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () ->
                importacionCsvService.clasificarPendiente(999L, 20L, false, null));
    }

    @Test
    @DisplayName("Debe generar CSV de ejemplo no vacío con encabezado esperado")
    void generarCsvEjemplo() {
        String ejemplo = importacionCsvService.generarCsvEjemplo();
        assertFalse(ejemplo.isBlank());
        assertTrue(ejemplo.startsWith("Fecha,Descripción,Cargo,Abono"));
    }
}
