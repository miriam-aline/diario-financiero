package com.diariofinanciero.web;

import com.diariofinanciero.domain.EntradaDiario;
import com.diariofinanciero.service.DiarioService;
import com.diariofinanciero.service.RecursoNoEncontradoException;
import com.diariofinanciero.service.ResumenSemanalDiario;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
class DiarioControllerTest {

    @Mock
    private DiarioService diarioService;

    @InjectMocks
    private DiarioController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private ResumenSemanalDiario resumenDe(LocalDate inicio) {
        return new ResumenSemanalDiario(
                inicio, inicio.plusDays(6), BigDecimal.ZERO, BigDecimal.ZERO,
                null, BigDecimal.ZERO, 0, 7, List.of(), List.of()
        );
    }

    @Test
    @DisplayName("GET /diario debe cargar vista, entrada existente y resultados de búsqueda")
    void verDiarioConBusqueda() throws Exception {
        LocalDate fecha = LocalDate.of(2026, 5, 13);
        EntradaDiario entrada = new EntradaDiario(fecha, "Nota", 4, "ahorro");

        when(diarioService.obtenerPorFecha(fecha)).thenReturn(Optional.of(entrada));
        when(diarioService.obtenerResumenSemanal(fecha)).thenReturn(resumenDe(LocalDate.of(2026, 5, 11)));
        when(diarioService.buscarEntradas("nota", null)).thenReturn(List.of());
        when(diarioService.listarEtiquetasUnicas()).thenReturn(List.of("ahorro"));

        mockMvc.perform(get("/diario").param("fecha", "2026-05-13").param("texto", "nota"))
                .andExpect(status().isOk())
                .andExpect(view().name("diario/index"))
                .andExpect(model().attribute("paginaActiva", "diario"))
                .andExpect(model().attribute("editandoExistente", true))
                .andExpect(model().attribute("modoBusqueda", true))
                .andExpect(model().attribute("etiquetasDisponibles", List.of("ahorro")));
    }

    @Test
    @DisplayName("POST /diario debe guardar entrada válida y redirigir")
    void guardarEntradaValida() throws Exception {
        mockMvc.perform(post("/diario")
                        .param("fecha", "2026-05-13")
                        .param("nota", "Ahorré hoy")
                        .param("estadoDeAnimo", "5")
                        .param("etiquetas", "ahorro"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/diario?semana=2026-05-13"))
                .andExpect(flash().attributeExists("mensajeExito"));

        verify(diarioService).guardarEntrada(LocalDate.of(2026, 5, 13), "Ahorré hoy", 5, "ahorro");
    }

    @Test
    @DisplayName("POST /diario debe volver a diario/index cuando hay errores de validación o dominio")
    void guardarEntradaConErrores() throws Exception {
        LocalDate fecha = LocalDate.of(2026, 5, 13);
        when(diarioService.obtenerPorFecha(fecha)).thenReturn(Optional.empty());
        when(diarioService.obtenerResumenSemanal(fecha)).thenReturn(resumenDe(LocalDate.of(2026, 5, 11)));
        when(diarioService.listarEtiquetasUnicas()).thenReturn(List.of());

        mockMvc.perform(post("/diario")
                        .param("fecha", "2026-05-13")
                        .param("nota", " "))
                .andExpect(status().isOk())
                .andExpect(view().name("diario/index"))
                .andExpect(model().attributeHasFieldErrors("entradaForm", "nota"));

        doThrow(new IllegalArgumentException("Error de dominio"))
                .when(diarioService).guardarEntrada(any(), any(), any(), any());

        mockMvc.perform(post("/diario")
                        .param("fecha", "2026-05-13")
                        .param("nota", "Nota válida"))
                .andExpect(status().isOk())
                .andExpect(view().name("diario/index"))
                .andExpect(model().attributeHasErrors("entradaForm"));
    }

    @Test
    @DisplayName("POST /diario/{id}/eliminar debe redirigir y devolver 404 si no existe")
    void eliminarYNoEncontrado() throws Exception {
        mockMvc.perform(post("/diario/5/eliminar").param("semana", "2026-05-11"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/diario?semana=2026-05-11"))
                .andExpect(flash().attributeExists("mensajeExito"));

        mockMvc.perform(post("/diario/6/eliminar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/diario"));

        doThrow(new RecursoNoEncontradoException("No encontrada")).when(diarioService).eliminar(99L);

        mockMvc.perform(post("/diario/99/eliminar"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(model().attribute("mensajeError", "No encontrada"));
    }
}
