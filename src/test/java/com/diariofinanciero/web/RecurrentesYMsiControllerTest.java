package com.diariofinanciero.web;

import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.MensualidadMSI;
import com.diariofinanciero.domain.Periodicidad;
import com.diariofinanciero.domain.TipoCuenta;
import com.diariofinanciero.domain.TipoMovimiento;
import com.diariofinanciero.service.CategoriaService;
import com.diariofinanciero.service.CompraMSIService;
import com.diariofinanciero.service.CuentaService;
import com.diariofinanciero.service.RecursoNoEncontradoException;
import com.diariofinanciero.service.RecurrenteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class RecurrentesYMsiControllerTest {

    @Mock
    private RecurrenteService recurrenteService;
    @Mock
    private CompraMSIService compraMSIService;
    @Mock
    private CategoriaService categoriaService;
    @Mock
    private CuentaService cuentaService;

    @InjectMocks
    private RecurrentesYMsiController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /recurrentes debe preseleccionar cuenta crédito para MSI y cargar catálogos")
    void verPantalla() throws Exception {
        Cuenta debito = new Cuenta("Débito", TipoCuenta.DEBITO);
        ReflectionTestUtils.setField(debito, "id", 1L);
        Cuenta credito = new Cuenta("Oro", TipoCuenta.CREDITO, 15, 5);
        ReflectionTestUtils.setField(credito, "id", 2L);

        when(cuentaService.listarTodas()).thenReturn(List.of(debito, credito));
        when(compraMSIService.listarTodas()).thenReturn(List.of());
        when(recurrenteService.listarTodos()).thenReturn(List.of());
        when(compraMSIService.calcularSaldoPendienteTotal(List.of())).thenReturn(BigDecimal.ZERO);
        when(compraMSIService.calcularCompromisoMensualActual(List.of())).thenReturn(BigDecimal.ZERO);
        when(categoriaService.listarTodas()).thenReturn(List.of());

        mockMvc.perform(get("/recurrentes"))
                .andExpect(status().isOk())
                .andExpect(view().name("recurrentes/index"))
                .andExpect(model().attribute("paginaActiva", "recurrentes"));
    }

    @Test
    @DisplayName("Endpoints de recurrentes deben crear, ejecutar pendientes, alternar y eliminar")
    void accionesRecurrentes() throws Exception {
        mockMvc.perform(post("/recurrentes")
                        .param("descripcion", "Internet")
                        .param("monto", "500.00")
                        .param("tipo", "GASTO")
                        .param("categoriaId", "1")
                        .param("cuentaId", "2")
                        .param("periodicidad", "MENSUAL")
                        .param("proximoCobro", "2026-06-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recurrentes"))
                .andExpect(flash().attributeExists("mensajeExito"));

        verify(recurrenteService).crear(
                "Internet", new BigDecimal("500.00"), TipoMovimiento.GASTO, 1L, 2L,
                Periodicidad.MENSUAL, LocalDate.of(2026, 6, 1)
        );

        when(recurrenteService.generarMovimientosPendientes(any())).thenReturn(2);
        mockMvc.perform(post("/recurrentes/ejecutar-pendientes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recurrentes"))
                .andExpect(flash().attribute("mensajeExito", "Se generaron 2 movimiento(s) recurrente(s) vencido(s) hasta hoy."));

        mockMvc.perform(post("/recurrentes/3/alternar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recurrentes"));
        verify(recurrenteService).alternarActivo(3L);

        mockMvc.perform(post("/recurrentes/3/eliminar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recurrentes"));
        verify(recurrenteService).eliminar(3L);
    }

    @Test
    @DisplayName("Endpoints de MSI deben crear, pagar cuota, eliminar y manejar 404")
    void accionesMsi() throws Exception {
        mockMvc.perform(post("/recurrentes/msi")
                        .param("descripcion", "Laptop")
                        .param("montoTotal", "12000.00")
                        .param("numeroMensualidades", "6")
                        .param("mensualidadesPagadas", "0")
                        .param("cuentaId", "2")
                        .param("fechaPrimerCargo", "2026-06-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recurrentes"));

        when(compraMSIService.registrarSiguienteMensualidad(4L, true))
                .thenReturn(new MensualidadMSI(1, LocalDate.of(2026, 6, 1), new BigDecimal("2000.00"), false));
        mockMvc.perform(post("/recurrentes/msi/4/pagar-cuota"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("mensajeExito"));

        when(compraMSIService.registrarSiguienteMensualidad(4L, true))
                .thenThrow(new IllegalStateException("Ya pagada"));
        mockMvc.perform(post("/recurrentes/msi/4/pagar-cuota"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensajeError", "Ya pagada"));

        mockMvc.perform(post("/recurrentes/msi/4/eliminar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recurrentes"));

        doThrow(new RecursoNoEncontradoException("No existe")).when(compraMSIService).eliminar(99L);
        mockMvc.perform(post("/recurrentes/msi/99/eliminar"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }
}
