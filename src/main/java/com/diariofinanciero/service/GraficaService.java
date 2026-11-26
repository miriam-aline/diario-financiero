package com.diariofinanciero.service;

import com.diariofinanciero.domain.Categoria;
import com.diariofinanciero.domain.Dinero;
import com.diariofinanciero.domain.Periodo;
import com.diariofinanciero.domain.TipoMovimiento;
import com.diariofinanciero.domain.TipoPeriodo;
import com.diariofinanciero.repository.GastoDiarioProjection;
import com.diariofinanciero.repository.GastoPorCategoriaProjection;
import com.diariofinanciero.repository.MovimientoRepository;
import com.diariofinanciero.repository.ResumenMensualProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class GraficaService {

    private static final Locale LOCALE_MX = Locale.forLanguageTag("es-MX");
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final int MESES_POR_DEFECTO = 6;
    private static final int MESES_MAXIMO = 12;

    private final MovimientoRepository movimientoRepository;
    private final CategoriaService categoriaService;

    public GraficaService(
            MovimientoRepository movimientoRepository,
            CategoriaService categoriaService
    ) {
        this.movimientoRepository = movimientoRepository;
        this.categoriaService = categoriaService;
    }

    public DatosGraficasMes obtenerDatos(YearMonth mesSolicitado, int cantidadMeses) {
        YearMonth mes = mesSolicitado == null ? YearMonth.now() : mesSolicitado;
        int ventanaMeses = normalizarVentanaMeses(cantidadMeses);
        Periodo periodoMes = Periodo.de(mes, TipoPeriodo.MES);

        List<Categoria> categorias = categoriaService.listarTodas();
        Map<Long, String> nombresPorCategoria = new HashMap<>();
        BigDecimal presupuestoTotal = Dinero.CERO;
        for (Categoria categoria : categorias) {
            nombresPorCategoria.put(categoria.getId(), categoria.getNombre());
            if (categoria.getPresupuestoMensual() != null) {
                presupuestoTotal = Dinero.sumar(presupuestoTotal, categoria.getPresupuestoMensual());
            }
        }

        // 1. Dona: gasto por categoría del mes seleccionado
        List<GastoPorCategoriaProjection> gastosCat = movimientoRepository.sumarPorCategoriaEnPeriodo(
                TipoMovimiento.GASTO, periodoMes.desde(), periodoMes.hasta());

        BigDecimal totalGastosMes = Dinero.CERO;
        for (GastoPorCategoriaProjection fila : gastosCat) {
            if (fila.getTotal() != null) {
                totalGastosMes = Dinero.sumar(totalGastosMes, fila.getTotal());
            }
        }

        final BigDecimal divisorGastos = totalGastosMes;
        List<DatosGraficasMes.ItemCategoriaGrafica> itemsCategoria = gastosCat.stream()
                .filter(f -> f.getTotal() != null && f.getTotal().compareTo(BigDecimal.ZERO) > 0)
                .map(f -> {
                    BigDecimal monto = Dinero.normalizar(f.getTotal());
                    BigDecimal porcentaje = divisorGastos.compareTo(BigDecimal.ZERO) > 0
                            ? monto.multiply(CIEN).divide(divisorGastos, 1, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
                    String nombre = nombresPorCategoria.getOrDefault(f.getCategoriaId(), "Sin categoría");
                    return new DatosGraficasMes.ItemCategoriaGrafica(nombre, monto, porcentaje);
                })
                .sorted(Comparator.comparing(DatosGraficasMes.ItemCategoriaGrafica::monto).reversed())
                .toList();

        // 2. Barras: ingresos vs. gastos por mes (últimos N meses terminando en 'mes')
        YearMonth primerMes = mes.minusMonths(ventanaMeses - 1L);
        List<ResumenMensualProjection> filasMensuales = movimientoRepository.sumarPorMesYTipo(
                primerMes.atDay(1), mes.atEndOfMonth());

        Map<YearMonth, BigDecimal> ingresosPorMes = new HashMap<>();
        Map<YearMonth, BigDecimal> gastosPorMes = new HashMap<>();
        for (ResumenMensualProjection fila : filasMensuales) {
            YearMonth ym = YearMonth.of(fila.getAnio(), fila.getMes());
            BigDecimal total = fila.getTotal() == null ? Dinero.CERO : Dinero.normalizar(fila.getTotal());
            if (fila.getTipo() == TipoMovimiento.INGRESO) {
                ingresosPorMes.put(ym, total);
            } else if (fila.getTipo() == TipoMovimiento.GASTO) {
                gastosPorMes.put(ym, total);
            }
        }

        List<DatosGraficasMes.ItemMesGrafica> serieMensual = new ArrayList<>();
        for (int i = 0; i < ventanaMeses; i++) {
            YearMonth actual = primerMes.plusMonths(i);
            BigDecimal ing = ingresosPorMes.getOrDefault(actual, Dinero.CERO);
            BigDecimal gas = gastosPorMes.getOrDefault(actual, Dinero.CERO);
            serieMensual.add(new DatosGraficasMes.ItemMesGrafica(
                    actual.toString(),
                    etiquetaMesCorta(actual),
                    ing,
                    gas
            ));
        }

        BigDecimal totalIngresosMes = ingresosPorMes.getOrDefault(mes, Dinero.CERO);

        // 3. Línea: gasto acumulado diario del mes vs. presupuesto total
        List<GastoDiarioProjection> gastosDiarios = movimientoRepository.sumarPorDiaEnPeriodo(
                TipoMovimiento.GASTO, periodoMes.desde(), periodoMes.hasta());
        Map<LocalDate, BigDecimal> gastoPorFecha = new HashMap<>();
        for (GastoDiarioProjection fila : gastosDiarios) {
            if (fila.getTotal() != null) {
                gastoPorFecha.put(fila.getFecha(), Dinero.normalizar(fila.getTotal()));
            }
        }

        LocalDate hoy = LocalDate.now();
        boolean esMesActual = mes.equals(YearMonth.from(hoy));
        boolean esMesFuturo = mes.isAfter(YearMonth.from(hoy));
        BigDecimal lineaPresupuesto = presupuestoTotal.compareTo(BigDecimal.ZERO) > 0 ? presupuestoTotal : null;

        List<DatosGraficasMes.ItemAcumuladoDia> serieAcumulada = new ArrayList<>();
        BigDecimal acumulado = Dinero.CERO;
        int diasDelMes = mes.lengthOfMonth();

        for (int dia = 1; dia <= diasDelMes; dia++) {
            LocalDate fecha = mes.atDay(dia);
            BigDecimal gastoDia = gastoPorFecha.getOrDefault(fecha, Dinero.CERO);
            boolean diaFuturo = esMesFuturo || (esMesActual && fecha.isAfter(hoy));

            if (diaFuturo && gastoDia.compareTo(BigDecimal.ZERO) == 0) {
                serieAcumulada.add(new DatosGraficasMes.ItemAcumuladoDia(
                        dia, fecha.toString(), Dinero.CERO, null, lineaPresupuesto));
            } else {
                acumulado = Dinero.sumar(acumulado, gastoDia);
                serieAcumulada.add(new DatosGraficasMes.ItemAcumuladoDia(
                        dia, fecha.toString(), gastoDia, acumulado, lineaPresupuesto));
            }
        }

        return new DatosGraficasMes(
                mes.toString(),
                periodoMes.etiqueta(),
                totalIngresosMes,
                totalGastosMes,
                presupuestoTotal,
                itemsCategoria,
                serieMensual,
                serieAcumulada
        );
    }

    public int normalizarVentanaMeses(int cantidadMeses) {
        if (cantidadMeses == 12) {
            return MESES_MAXIMO;
        }
        return MESES_POR_DEFECTO;
    }

    private String etiquetaMesCorta(YearMonth ym) {
        String nombre = ym.getMonth().getDisplayName(TextStyle.SHORT, LOCALE_MX).replace(".", "");
        String capitalizado = nombre.substring(0, 1).toUpperCase(LOCALE_MX) + nombre.substring(1);
        return capitalizado + " " + String.valueOf(ym.getYear()).substring(2);
    }
}
