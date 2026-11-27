package com.diariofinanciero.web;

import com.diariofinanciero.domain.EstadoPresupuesto;
import com.diariofinanciero.service.CategoriaService;
import com.diariofinanciero.service.PresupuestoService;
import com.diariofinanciero.service.RecursoNoEncontradoException;
import com.diariofinanciero.service.ResumenPresupuestosMes;
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
import java.time.YearMonth;
import java.util.List;

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
class PresupuestoControllerTest {

    @Mock
    private PresupuestoService presupuestoService;

    @Mock
    private CategoriaService categoriaService;

    @InjectMocks
    private PresupuestoController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private ResumenPresupuestosMes resumenDe(YearMonth mes) {
        return new ResumenPresupuestosMes(
                mes, mes.minusMonths(1), "Mes", "Mes Ant",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                null, 0, EstadoPresupuesto.SIN_PRESUPUESTO, List.of()
        );
    }

    @Test
    @DisplayName("GET /presupuestos debe cargar modelo para el mes indicado")
    void verPresupuestos() throws Exception {
        YearMonth mayo = YearMonth.of(2026, 5);
        when(presupuestoService.obtenerResumenMensual(mayo)).thenReturn(resumenDe(mayo));

        mockMvc.perform(get("/presupuestos").param("mes", "2026-05"))
                .andExpect(status().isOk())
                .andExpect(view().name("presupuestos/index"))
                .andExpect(model().attribute("paginaActiva", "presupuestos"))
                .andExpect(model().attribute("mesSeleccionado", mayo));
    }

    @Test
    @DisplayName("POST /presupuestos/categorias/{id} debe actualizar, limpiar o reportar monto inválido")
    void actualizarPresupuesto() throws Exception {
        mockMvc.perform(post("/presupuestos/categorias/1")
                        .param("presupuestoMensual", "2500.50")
                        .param("mes", "2026-05"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/presupuestos?mes=2026-05"))
                .andExpect(flash().attribute("mensajeExito", "Presupuesto actualizado correctamente."));

        verify(categoriaService).actualizarPresupuesto(1L, new BigDecimal("2500.50"));

        mockMvc.perform(post("/presupuestos/categorias/1")
                        .param("presupuestoMensual", " ")
                        .param("mes", "2026-05"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensajeExito", "Presupuesto eliminado para la categoría."));

        mockMvc.perform(post("/presupuestos/categorias/1")
                        .param("presupuestoMensual", "abc")
                        .param("mes", "2026-05"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensajeError", "Ingresa un monto numérico válido (ejemplo: 3500.00)"));

        doThrow(new RecursoNoEncontradoException("No existe")).when(categoriaService).actualizarPresupuesto(99L, null);
        mockMvc.perform(post("/presupuestos/categorias/99").param("mes", "2026-05"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    @Test
    @DisplayName("POST /presupuestos/categorias debe crear categoría o mostrar errores de validación/dominio")
    void crearCategoria() throws Exception {
        YearMonth mayo = YearMonth.of(2026, 5);

        mockMvc.perform(post("/presupuestos/categorias")
                        .param("nombre", "Mascotas")
                        .param("presupuestoMensual", "800.00")
                        .param("mes", "2026-05"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/presupuestos?mes=2026-05"))
                .andExpect(flash().attributeExists("mensajeExito"));

        verify(categoriaService).crear("Mascotas", new BigDecimal("800.00"));

        when(presupuestoService.obtenerResumenMensual(mayo)).thenReturn(resumenDe(mayo));
        mockMvc.perform(post("/presupuestos/categorias")
                        .param("nombre", " ")
                        .param("mes", "2026-05"))
                .andExpect(status().isOk())
                .andExpect(view().name("presupuestos/index"))
                .andExpect(model().attributeHasFieldErrors("categoriaForm", "nombre"));

        doThrow(new IllegalArgumentException("Duplicada")).when(categoriaService).crear("Comida", null);
        mockMvc.perform(post("/presupuestos/categorias")
                        .param("nombre", "Comida")
                        .param("mes", "2026-05"))
                .andExpect(status().isOk())
                .andExpect(view().name("presupuestos/index"))
                .andExpect(model().attributeHasErrors("categoriaForm"));
    }
}
