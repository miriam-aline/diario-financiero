package com.diariofinanciero.service;

import com.diariofinanciero.domain.Categoria;
import com.diariofinanciero.domain.EstadoPresupuesto;
import com.diariofinanciero.domain.TipoMovimiento;
import com.diariofinanciero.repository.GastoPorCategoriaProjection;
import com.diariofinanciero.repository.MovimientoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresupuestoServiceTest {

    @Mock
    private CategoriaService categoriaService;

    @Mock
    private MovimientoRepository movimientoRepository;

    @InjectMocks
    private PresupuestoService presupuestoService;

    @Test
    @DisplayName("Debe calcular resumen mensual, orden por prioridad de estado, comparativo y acotar barra a 100")
    void obtenerResumenMensual() {
        YearMonth mayo = YearMonth.of(2026, 5);
        YearMonth abril = YearMonth.of(2026, 4);

        Categoria comida = new Categoria("Comida", new BigDecimal("1000.00"));
        ReflectionTestUtils.setField(comida, "id", 1L);
        Categoria ocio = new Categoria("Ocio", new BigDecimal("500.00"));
        ReflectionTestUtils.setField(ocio, "id", 2L);
        Categoria sinPres = new Categoria("Varios");
        ReflectionTestUtils.setField(sinPres, "id", 3L);

        when(categoriaService.listarTodas()).thenReturn(List.of(sinPres, comida, ocio));

        GastoPorCategoriaProjection actComida = projection(1L, "850.00");
        GastoPorCategoriaProjection actOcio = projection(2L, "600.00");
        GastoPorCategoriaProjection actSinPres = projection(3L, "100.00");
        when(movimientoRepository.sumarPorCategoriaEnPeriodo(TipoMovimiento.GASTO, mayo.atDay(1), mayo.atEndOfMonth()))
                .thenReturn(List.of(actComida, actOcio, actSinPres));

        GastoPorCategoriaProjection antOcio = projection(2L, "400.00");
        when(movimientoRepository.sumarPorCategoriaEnPeriodo(TipoMovimiento.GASTO, abril.atDay(1), abril.atEndOfMonth()))
                .thenReturn(List.of(antOcio));

        ResumenPresupuestosMes resumen = presupuestoService.obtenerResumenMensual(mayo);

        assertEquals(mayo, resumen.mes());
        assertEquals(abril, resumen.mesAnterior());
        assertEquals(new BigDecimal("1500.00"), resumen.presupuestoTotal());
        assertEquals(new BigDecimal("1550.00"), resumen.gastadoTotalMes());
        assertEquals(new BigDecimal("400.00"), resumen.gastadoTotalMesAnterior());
        assertEquals(new BigDecimal("1150.00"), resumen.diferenciaTotal());
        assertEquals(new BigDecimal("96.7"), resumen.porcentajeAvanceGlobal());
        assertEquals(97, resumen.porcentajeBarraGlobal());
        assertEquals(EstadoPresupuesto.AVISO, resumen.estadoGlobal());

        // Orden: EXCEDIDO (Ocio), AVISO (Comida), SIN_PRESUPUESTO (Varios)
        List<ResumenCategoriaPresupuesto> items = resumen.categorias();
        assertEquals("Ocio", items.get(0).categoriaNombre());
        assertEquals(EstadoPresupuesto.EXCEDIDO, items.get(0).estado());
        assertEquals(100, items.get(0).porcentajeBarra());
        assertEquals(new BigDecimal("-100.00"), items.get(0).disponible());
        assertEquals(new BigDecimal("50.0"), items.get(0).porcentajeCambioMesAnterior());

        assertEquals("Comida", items.get(1).categoriaNombre());
        assertEquals(EstadoPresupuesto.AVISO, items.get(1).estado());
        assertNull(items.get(1).porcentajeCambioMesAnterior());

        assertEquals("Varios", items.get(2).categoriaNombre());
        assertEquals(EstadoPresupuesto.SIN_PRESUPUESTO, items.get(2).estado());
        assertNull(items.get(2).disponible());
        assertEquals(0, items.get(2).porcentajeBarra());
    }

    @Test
    @DisplayName("Debe usar YearMonth actual cuando se solicita null y manejar presupuestoTotal cero")
    void resumenSinPresupuestos() {
        YearMonth actual = YearMonth.now();
        when(categoriaService.listarTodas()).thenReturn(List.of());
        when(movimientoRepository.sumarPorCategoriaEnPeriodo(TipoMovimiento.GASTO, actual.atDay(1), actual.atEndOfMonth()))
                .thenReturn(List.of());
        when(movimientoRepository.sumarPorCategoriaEnPeriodo(TipoMovimiento.GASTO, actual.minusMonths(1).atDay(1), actual.minusMonths(1).atEndOfMonth()))
                .thenReturn(List.of());

        ResumenPresupuestosMes resumen = presupuestoService.obtenerResumenMensual(null);

        assertEquals(actual, resumen.mes());
        assertEquals(EstadoPresupuesto.SIN_PRESUPUESTO, resumen.estadoGlobal());
        assertNull(resumen.porcentajeAvanceGlobal());
        assertEquals(0, resumen.porcentajeBarraGlobal());
    }

    private GastoPorCategoriaProjection projection(Long id, String monto) {
        GastoPorCategoriaProjection p = mock(GastoPorCategoriaProjection.class);
        when(p.getCategoriaId()).thenReturn(id);
        when(p.getTotal()).thenReturn(new BigDecimal(monto));
        return p;
    }
}
