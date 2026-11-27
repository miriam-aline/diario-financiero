package com.diariofinanciero.web;

import com.diariofinanciero.domain.EstadoPresupuesto;
import com.diariofinanciero.service.BalancePeriodo;
import com.diariofinanciero.service.CompraMSIService;
import com.diariofinanciero.service.DiarioService;
import com.diariofinanciero.service.ImportacionCsvService;
import com.diariofinanciero.service.MovimientoService;
import com.diariofinanciero.service.PresupuestoService;
import com.diariofinanciero.service.ResumenCategoriaPresupuesto;
import com.diariofinanciero.service.ResumenPresupuestosMes;
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
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class InicioControllerTest {

    @Mock
    private MovimientoService movimientoService;
    @Mock
    private PresupuestoService presupuestoService;
    @Mock
    private DiarioService diarioService;
    @Mock
    private TarjetaService tarjetaService;
    @Mock
    private CompraMSIService compraMSIService;
    @Mock
    private ImportacionCsvService importacionCsvService;

    @InjectMocks
    private InicioController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET / debe ensamblar resumen del panel inicial y filtrar presupuestos con monto asignado")
    void inicio() throws Exception {
        YearMonth mes = YearMonth.now();
        BalancePeriodo balance = new BalancePeriodo(new BigDecimal("5000.00"), new BigDecimal("1200.00"), new BigDecimal("3800.00"));
        ResumenCategoriaPresupuesto conPresupuesto = new ResumenCategoriaPresupuesto(
                1L, "Comida", new BigDecimal("2000"), new BigDecimal("1200"),
                BigDecimal.ZERO, new BigDecimal("1200"), null, new BigDecimal("60.0"), 60,
                new BigDecimal("800"), EstadoPresupuesto.NORMAL
        );
        ResumenCategoriaPresupuesto sinPresupuesto = new ResumenCategoriaPresupuesto(
                2L, "Varios", null, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, null, null, 0, null, EstadoPresupuesto.SIN_PRESUPUESTO
        );
        ResumenPresupuestosMes resumenPres = new ResumenPresupuestosMes(
                mes, mes.minusMonths(1), "Mes", "Mes Ant",
                new BigDecimal("2000"), new BigDecimal("1200"), BigDecimal.ZERO, new BigDecimal("1200"),
                new BigDecimal("60.0"), 60, EstadoPresupuesto.NORMAL, List.of(conPresupuesto, sinPresupuesto)
        );

        when(movimientoService.listar(any())).thenReturn(List.of());
        when(movimientoService.calcularBalance(List.of())).thenReturn(balance);
        when(presupuestoService.obtenerResumenMensual(mes)).thenReturn(resumenPres);
        when(diarioService.obtenerPorFecha(any())).thenReturn(Optional.empty());
        when(tarjetaService.obtenerResumenTarjetas(any(), anyInt())).thenReturn(List.of());
        when(tarjetaService.obtenerAlertasPagoProximo(any(), anyInt())).thenReturn(List.of());
        when(tarjetaService.calcularTotalEstimadoProximoCorte(List.of())).thenReturn(BigDecimal.ZERO);
        when(compraMSIService.listarTodas()).thenReturn(List.of());
        when(compraMSIService.calcularCompromisoMensualActual(List.of())).thenReturn(new BigDecimal("450.00"));
        when(importacionCsvService.listarPendientesRevision()).thenReturn(List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("inicio/index"))
                .andExpect(model().attribute("paginaActiva", "inicio"))
                .andExpect(model().attribute("balanceMes", balance))
                .andExpect(model().attribute("presupuestosDestacados", List.of(conPresupuesto)))
                .andExpect(model().attribute("compromisoMsi", new BigDecimal("450.00")))
                .andExpect(model().attribute("pendientesSinCategoria", 0));
    }
}
