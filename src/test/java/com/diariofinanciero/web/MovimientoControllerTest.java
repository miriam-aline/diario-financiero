package com.diariofinanciero.web;

import com.diariofinanciero.domain.Categoria;
import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.Movimiento;
import com.diariofinanciero.domain.TipoCuenta;
import com.diariofinanciero.domain.TipoMovimiento;
import com.diariofinanciero.service.BalancePeriodo;
import com.diariofinanciero.service.CategoriaService;
import com.diariofinanciero.service.CuentaService;
import com.diariofinanciero.service.MovimientoService;
import com.diariofinanciero.service.RecursoNoEncontradoException;
import com.diariofinanciero.service.TarjetaService;
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
class MovimientoControllerTest {

    @Mock
    private MovimientoService movimientoService;
    @Mock
    private CategoriaService categoriaService;
    @Mock
    private CuentaService cuentaService;
    @Mock
    private TarjetaService tarjetaService;

    @InjectMocks
    private MovimientoController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /movimientos debe listar movimientos y capturar rango de fechas inválido en errorFiltro")
    void listarMovimientos() throws Exception {
        when(movimientoService.listar(any())).thenReturn(List.of());
        when(movimientoService.calcularBalance(List.of())).thenReturn(
                new BalancePeriodo(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        when(categoriaService.listarTodas()).thenReturn(List.of());
        when(tarjetaService.obtenerAlertasPagoProximo(any(), anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/movimientos").param("mes", "2026-05"))
                .andExpect(status().isOk())
                .andExpect(view().name("movimientos/lista"))
                .andExpect(model().attribute("paginaActiva", "movimientos"))
                .andExpect(model().attribute("usarFiltroManual", false));

        mockMvc.perform(get("/movimientos").param("desde", "2026-05-20").param("hasta", "2026-05-01"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("errorFiltro"));
    }

    @Test
    @DisplayName("GET /movimientos/nuevo y POST /movimientos deben registrar y manejar agregarOtro")
    void nuevoYRegistrar() throws Exception {
        Cuenta cta = new Cuenta("Efectivo", TipoCuenta.EFECTIVO);
        ReflectionTestUtils.setField(cta, "id", 1L);
        when(cuentaService.listarTodas()).thenReturn(List.of(cta));
        when(categoriaService.listarRecientes(8)).thenReturn(List.of());
        when(categoriaService.listarTodas()).thenReturn(List.of());

        mockMvc.perform(get("/movimientos/nuevo").param("tipo", "INGRESO"))
                .andExpect(status().isOk())
                .andExpect(view().name("movimientos/nuevo"));

        mockMvc.perform(post("/movimientos")
                        .param("fecha", "2026-05-10")
                        .param("monto", "150.00")
                        .param("tipo", "GASTO")
                        .param("categoriaId", "1")
                        .param("cuentaId", "1")
                        .param("descripcion", "Tacos")
                        .param("agregarOtro", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movimientos/nuevo?tipo=GASTO"))
                .andExpect(flash().attributeExists("mensajeExito"));

        mockMvc.perform(post("/movimientos")
                        .param("fecha", "2026-05-10")
                        .param("monto", "150.00")
                        .param("tipo", "GASTO")
                        .param("categoriaId", "1")
                        .param("cuentaId", "1")
                        .param("descripcion", "Tacos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movimientos"));

        mockMvc.perform(post("/movimientos").param("descripcion", " "))
                .andExpect(status().isOk())
                .andExpect(view().name("movimientos/nuevo"))
                .andExpect(model().attributeHasErrors("movimientoForm"));
    }

    @Test
    @DisplayName("GET/POST editar, eliminar y 404 deben comportarse correctamente")
    void editarEliminarY404() throws Exception {
        Categoria cat = new Categoria("Comida");
        ReflectionTestUtils.setField(cat, "id", 1L);
        Cuenta cta = new Cuenta("Efectivo", TipoCuenta.EFECTIVO);
        ReflectionTestUtils.setField(cta, "id", 2L);
        Movimiento mov = Movimiento.manual(LocalDate.of(2026, 5, 10), BigDecimal.TEN, TipoMovimiento.GASTO, cat, cta, "Café");

        when(movimientoService.obtenerPorId(5L)).thenReturn(mov);
        when(cuentaService.listarTodas()).thenReturn(List.of(cta));
        when(categoriaService.listarTodas()).thenReturn(List.of(cat));

        mockMvc.perform(get("/movimientos/5/editar"))
                .andExpect(status().isOk())
                .andExpect(view().name("movimientos/editar"))
                .andExpect(model().attribute("movimientoId", 5L));

        mockMvc.perform(post("/movimientos/5/editar")
                        .param("fecha", "2026-05-11")
                        .param("monto", "20.00")
                        .param("tipo", "GASTO")
                        .param("categoriaId", "1")
                        .param("cuentaId", "2")
                        .param("descripcion", "Café grande"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movimientos"));

        mockMvc.perform(post("/movimientos/5/eliminar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movimientos"));
        verify(movimientoService).eliminar(5L);

        doThrow(new RecursoNoEncontradoException("No existe")).when(movimientoService).obtenerPorId(99L);
        mockMvc.perform(get("/movimientos/99/editar"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }
}
