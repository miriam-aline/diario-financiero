package com.diariofinanciero.service;

import com.diariofinanciero.domain.Categoria;
import com.diariofinanciero.domain.Dinero;
import com.diariofinanciero.domain.EstadoPresupuesto;
import com.diariofinanciero.domain.Periodo;
import com.diariofinanciero.domain.TipoMovimiento;
import com.diariofinanciero.domain.TipoPeriodo;
import com.diariofinanciero.repository.GastoPorCategoriaProjection;
import com.diariofinanciero.repository.MovimientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class PresupuestoService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final CategoriaService categoriaService;
    private final MovimientoRepository movimientoRepository;

    public PresupuestoService(
            CategoriaService categoriaService,
            MovimientoRepository movimientoRepository
    ) {
        this.categoriaService = categoriaService;
        this.movimientoRepository = movimientoRepository;
    }

    public ResumenPresupuestosMes obtenerResumenMensual(YearMonth mesSolicitado) {
        YearMonth mes = mesSolicitado == null ? YearMonth.now() : mesSolicitado;
        YearMonth mesAnterior = mes.minusMonths(1);

        Periodo periodoActual = Periodo.de(mes, TipoPeriodo.MES);
        Periodo periodoAnterior = Periodo.de(mesAnterior, TipoPeriodo.MES);

        Map<Long, BigDecimal> gastosActuales = mapearGastos(
                movimientoRepository.sumarPorCategoriaEnPeriodo(
                        TipoMovimiento.GASTO, periodoActual.desde(), periodoActual.hasta())
        );
        Map<Long, BigDecimal> gastosAnteriores = mapearGastos(
                movimientoRepository.sumarPorCategoriaEnPeriodo(
                        TipoMovimiento.GASTO, periodoAnterior.desde(), periodoAnterior.hasta())
        );

        List<Categoria> categorias = categoriaService.listarTodas();

        BigDecimal presupuestoTotal = Dinero.CERO;
        BigDecimal gastadoTotalConPresupuesto = Dinero.CERO;
        BigDecimal gastadoTotalMes = Dinero.CERO;
        BigDecimal gastadoTotalMesAnterior = Dinero.CERO;

        List<ResumenCategoriaPresupuesto> items = categorias.stream()
                .map(categoria -> construirResumenCategoria(
                        categoria,
                        gastosActuales.getOrDefault(categoria.getId(), Dinero.CERO),
                        gastosAnteriores.getOrDefault(categoria.getId(), Dinero.CERO)
                ))
                .sorted(Comparator
                        .comparingInt((ResumenCategoriaPresupuesto r) -> r.estado().getPrioridadOrden())
                        .thenComparing(ResumenCategoriaPresupuesto::gastadoMes, Comparator.reverseOrder())
                        .thenComparing(ResumenCategoriaPresupuesto::categoriaNombre, String.CASE_INSENSITIVE_ORDER))
                .toList();

        for (ResumenCategoriaPresupuesto item : items) {
            gastadoTotalMes = Dinero.sumar(gastadoTotalMes, item.gastadoMes());
            gastadoTotalMesAnterior = Dinero.sumar(gastadoTotalMesAnterior, item.gastadoMesAnterior());
            if (item.presupuestoMensual() != null) {
                presupuestoTotal = Dinero.sumar(presupuestoTotal, item.presupuestoMensual());
                gastadoTotalConPresupuesto = Dinero.sumar(gastadoTotalConPresupuesto, item.gastadoMes());
            }
        }

        BigDecimal presupuestoGlobal = presupuestoTotal.compareTo(BigDecimal.ZERO) > 0 ? presupuestoTotal : null;
        EstadoPresupuesto estadoGlobal = EstadoPresupuesto.evaluar(gastadoTotalConPresupuesto, presupuestoGlobal);
        BigDecimal porcentajeGlobal = calcularPorcentaje(gastadoTotalConPresupuesto, presupuestoGlobal);
        int barraGlobal = acotarBarra(porcentajeGlobal);

        return new ResumenPresupuestosMes(
                mes,
                mesAnterior,
                periodoActual.etiqueta(),
                periodoAnterior.etiqueta(),
                presupuestoTotal,
                gastadoTotalMes,
                gastadoTotalMesAnterior,
                Dinero.restar(gastadoTotalMes, gastadoTotalMesAnterior),
                porcentajeGlobal,
                barraGlobal,
                estadoGlobal,
                items
        );
    }

    private ResumenCategoriaPresupuesto construirResumenCategoria(
            Categoria categoria,
            BigDecimal gastadoMes,
            BigDecimal gastadoMesAnterior
    ) {
        BigDecimal presupuesto = categoria.getPresupuestoMensual();
        BigDecimal gastadoActual = Dinero.normalizar(gastadoMes);
        BigDecimal gastadoPrevio = Dinero.normalizar(gastadoMesAnterior);
        BigDecimal diferencia = Dinero.restar(gastadoActual, gastadoPrevio);

        BigDecimal porcentajeCambio = null;
        if (gastadoPrevio.compareTo(BigDecimal.ZERO) > 0) {
            porcentajeCambio = diferencia.multiply(CIEN).divide(gastadoPrevio, 1, RoundingMode.HALF_UP);
        }

        EstadoPresupuesto estado = EstadoPresupuesto.evaluar(gastadoActual, presupuesto);
        BigDecimal porcentajeAvance = calcularPorcentaje(gastadoActual, presupuesto);
        int porcentajeBarra = acotarBarra(porcentajeAvance);
        BigDecimal disponible = presupuesto == null ? null : Dinero.restar(presupuesto, gastadoActual);

        return new ResumenCategoriaPresupuesto(
                categoria.getId(),
                categoria.getNombre(),
                presupuesto,
                gastadoActual,
                gastadoPrevio,
                diferencia,
                porcentajeCambio,
                porcentajeAvance,
                porcentajeBarra,
                disponible,
                estado
        );
    }

    private Map<Long, BigDecimal> mapearGastos(List<GastoPorCategoriaProjection> filas) {
        Map<Long, BigDecimal> mapa = new HashMap<>();
        for (GastoPorCategoriaProjection fila : filas) {
            BigDecimal total = fila.getTotal() == null ? Dinero.CERO : Dinero.normalizar(fila.getTotal());
            mapa.put(fila.getCategoriaId(), total);
        }
        return mapa;
    }

    private BigDecimal calcularPorcentaje(BigDecimal gastado, BigDecimal presupuesto) {
        if (presupuesto == null || presupuesto.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return gastado.multiply(CIEN).divide(presupuesto, 1, RoundingMode.HALF_UP);
    }

    private int acotarBarra(BigDecimal porcentaje) {
        if (porcentaje == null) {
            return 0;
        }
        int entero = porcentaje.setScale(0, RoundingMode.HALF_UP).intValue();
        return Math.max(0, Math.min(100, entero));
    }
}
