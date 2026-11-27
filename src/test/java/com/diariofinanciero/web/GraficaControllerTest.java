package com.diariofinanciero.web;

import com.diariofinanciero.service.DatosGraficasMes;
import com.diariofinanciero.service.GraficaService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class GraficaControllerTest {

    @Mock
    private GraficaService graficaService;

    @InjectMocks
    private GraficaController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /graficas debe cargar modelo con mes y ventana normalizada")
    void verGraficas() throws Exception {
        YearMonth mayo = YearMonth.of(2026, 5);
        DatosGraficasMes datos = new DatosGraficasMes(
                "2026-05", "Mayo 2026", BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ZERO,
                List.of(), List.of(), List.of()
        );
        when(graficaService.normalizarVentanaMeses(12)).thenReturn(12);
        when(graficaService.obtenerDatos(mayo, 12)).thenReturn(datos);

        mockMvc.perform(get("/graficas").param("mes", "2026-05").param("meses", "12"))
                .andExpect(status().isOk())
                .andExpect(view().name("graficas/index"))
                .andExpect(model().attribute("paginaActiva", "graficas"))
                .andExpect(model().attribute("datos", datos))
                .andExpect(model().attribute("mesAnterior", YearMonth.of(2026, 4)))
                .andExpect(model().attribute("mesSiguiente", YearMonth.of(2026, 6)))
                .andExpect(model().attribute("mesesVentana", 12));
    }

    @Test
    @DisplayName("GET /api/graficas debe responder JSON y hacer fallback al mes actual si el parámetro es inválido")
    void obtenerDatosJson() throws Exception {
        YearMonth actual = YearMonth.now();
        DatosGraficasMes datos = new DatosGraficasMes(
                actual.toString(), "Mes Actual", new BigDecimal("100.00"), new BigDecimal("40.00"), null,
                List.of(), List.of(), List.of()
        );
        when(graficaService.obtenerDatos(actual, 6)).thenReturn(datos);

        mockMvc.perform(get("/api/graficas").param("mes", "invalido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mes").value(actual.toString()))
                .andExpect(jsonPath("$.totalIngresosMes").value(100.00));
    }
}
