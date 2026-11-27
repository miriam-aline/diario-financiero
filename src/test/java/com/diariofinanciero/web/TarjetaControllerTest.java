package com.diariofinanciero.web;

import com.diariofinanciero.service.CuentaService;
import com.diariofinanciero.service.RecursoNoEncontradoException;
import com.diariofinanciero.service.TarjetaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
class TarjetaControllerTest {

    @Mock
    private TarjetaService tarjetaService;

    @Mock
    private CuentaService cuentaService;

    @InjectMocks
    private TarjetaController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /tarjetas debe cargar vista y resumen de tarjetas")
    void verTarjetas() throws Exception {
        when(tarjetaService.obtenerResumenTarjetas(any(), anyInt())).thenReturn(List.of());
        when(tarjetaService.obtenerAlertasPagoProximo(any(), anyInt())).thenReturn(List.of());
        when(tarjetaService.calcularTotalEstimadoProximoCorte(List.of())).thenReturn(BigDecimal.ZERO);

        mockMvc.perform(get("/tarjetas"))
                .andExpect(status().isOk())
                .andExpect(view().name("tarjetas/index"))
                .andExpect(model().attribute("paginaActiva", "tarjetas"))
                .andExpect(model().attribute("diasAviso", TarjetaService.DIAS_AVISO_PAGO_POR_DEFECTO));
    }

    @Test
    @DisplayName("POST /tarjetas debe crear tarjeta válida y manejar errores de validación o dominio")
    void crearTarjeta() throws Exception {
        mockMvc.perform(post("/tarjetas")
                        .param("nombre", "Tarjeta Oro")
                        .param("diaCorte", "15")
                        .param("diaPago", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tarjetas"))
                .andExpect(flash().attributeExists("mensajeExito"));

        verify(cuentaService).crearTarjetaCredito("Tarjeta Oro", 15, 5);

        when(tarjetaService.obtenerResumenTarjetas(any(), anyInt())).thenReturn(List.of());
        when(tarjetaService.obtenerAlertasPagoProximo(any(), anyInt())).thenReturn(List.of());
        when(tarjetaService.calcularTotalEstimadoProximoCorte(List.of())).thenReturn(BigDecimal.ZERO);

        mockMvc.perform(post("/tarjetas").param("nombre", " "))
                .andExpect(status().isOk())
                .andExpect(view().name("tarjetas/index"))
                .andExpect(model().attributeHasErrors("tarjetaForm"));

        doThrow(new IllegalArgumentException("Duplicada"))
                .when(cuentaService).crearTarjetaCredito("Duplicada", 10, 20);
        mockMvc.perform(post("/tarjetas")
                        .param("nombre", "Duplicada")
                        .param("diaCorte", "10")
                        .param("diaPago", "20"))
                .andExpect(status().isOk())
                .andExpect(view().name("tarjetas/index"))
                .andExpect(model().attributeHasErrors("tarjetaForm"));
    }

    @Test
    @DisplayName("POST /tarjetas/{id}/editar debe actualizar, reportar error de dominio o devolver 404")
    void actualizarTarjeta() throws Exception {
        mockMvc.perform(post("/tarjetas/3/editar")
                        .param("nombre", "Nu")
                        .param("diaCorte", "10")
                        .param("diaPago", "25"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tarjetas"))
                .andExpect(flash().attributeExists("mensajeExito"));

        doThrow(new IllegalArgumentException("Nombre repetido"))
                .when(cuentaService).actualizarTarjetaCredito(3L, "Repetida", 10, 25);
        mockMvc.perform(post("/tarjetas/3/editar")
                        .param("nombre", "Repetida")
                        .param("diaCorte", "10")
                        .param("diaPago", "25"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensajeError", "Nombre repetido"));

        doThrow(new RecursoNoEncontradoException("No encontrada"))
                .when(cuentaService).actualizarTarjetaCredito(99L, "X", 10, 25);
        mockMvc.perform(post("/tarjetas/99/editar")
                        .param("nombre", "X")
                        .param("diaCorte", "10")
                        .param("diaPago", "25"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }
}
