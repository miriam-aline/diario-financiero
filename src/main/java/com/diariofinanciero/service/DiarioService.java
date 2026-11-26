package com.diariofinanciero.service;

import com.diariofinanciero.domain.Dinero;
import com.diariofinanciero.domain.EntradaDiario;
import com.diariofinanciero.domain.Movimiento;
import com.diariofinanciero.domain.TipoMovimiento;
import com.diariofinanciero.repository.EntradaDiarioRepository;
import com.diariofinanciero.repository.MovimientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

@Service
@Transactional(readOnly = true)
public class DiarioService {

    private final EntradaDiarioRepository entradaDiarioRepository;
    private final MovimientoRepository movimientoRepository;

    public DiarioService(
            EntradaDiarioRepository entradaDiarioRepository,
            MovimientoRepository movimientoRepository
    ) {
        this.entradaDiarioRepository = entradaDiarioRepository;
        this.movimientoRepository = movimientoRepository;
    }

    @Transactional
    public EntradaDiario guardarEntrada(
            LocalDate fecha,
            String nota,
            Integer estadoDeAnimo,
            String etiquetas
    ) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de la entrada es obligatoria");
        }
        Optional<EntradaDiario> existente = entradaDiarioRepository.findByFecha(fecha);
        if (existente.isPresent()) {
            EntradaDiario entrada = existente.get();
            entrada.actualizar(nota, estadoDeAnimo, etiquetas);
            return entradaDiarioRepository.save(entrada);
        }
        EntradaDiario nueva = new EntradaDiario(fecha, nota, estadoDeAnimo, etiquetas);
        return entradaDiarioRepository.save(nueva);
    }

    public Optional<EntradaDiario> obtenerPorFecha(LocalDate fecha) {
        if (fecha == null) {
            return Optional.empty();
        }
        return entradaDiarioRepository.findByFecha(fecha);
    }

    public EntradaDiario obtenerPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El identificador de la entrada es obligatorio");
        }
        return entradaDiarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Entrada de diario no encontrada con id: " + id));
    }

    @Transactional
    public void eliminar(Long id) {
        EntradaDiario existente = obtenerPorId(id);
        entradaDiarioRepository.delete(existente);
    }

    public ResumenSemanalDiario obtenerResumenSemanal(LocalDate fechaReferencia) {
        LocalDate referencia = fechaReferencia == null ? LocalDate.now() : fechaReferencia;
        LocalDate inicioSemana = referencia.with(DayOfWeek.MONDAY);
        LocalDate finSemana = inicioSemana.plusDays(6);

        List<Movimiento> movimientosSemana = movimientoRepository.buscarConFiltros(
                inicioSemana, finSemana, null, null);
        List<EntradaDiario> notasSemana = entradaDiarioRepository.findByFechaBetweenOrderByFechaDesc(
                inicioSemana, finSemana);

        Map<LocalDate, List<Movimiento>> movimientosPorDia = new HashMap<>();
        Map<String, BigDecimal> gastosPorCategoria = new HashMap<>();
        Set<LocalDate> fechasConGasto = new HashSet<>();

        BigDecimal totalGastado = Dinero.CERO;
        BigDecimal totalIngresado = Dinero.CERO;

        for (Movimiento mov : movimientosSemana) {
            movimientosPorDia.computeIfAbsent(mov.getFecha(), f -> new ArrayList<>()).add(mov);
            if (mov.getTipo() == TipoMovimiento.GASTO) {
                totalGastado = Dinero.sumar(totalGastado, mov.getMonto());
                fechasConGasto.add(mov.getFecha());
                String catNombre = mov.getCategoria().getNombre();
                BigDecimal acumuladoCat = gastosPorCategoria.getOrDefault(catNombre, Dinero.CERO);
                gastosPorCategoria.put(catNombre, Dinero.sumar(acumuladoCat, mov.getMonto()));
            } else if (mov.getTipo() == TipoMovimiento.INGRESO) {
                totalIngresado = Dinero.sumar(totalIngresado, mov.getMonto());
            }
        }

        String categoriaMasAlta = null;
        BigDecimal montoCategoriaMasAlta = Dinero.CERO;
        for (Map.Entry<String, BigDecimal> entry : gastosPorCategoria.entrySet()) {
            if (categoriaMasAlta == null || entry.getValue().compareTo(montoCategoriaMasAlta) > 0) {
                categoriaMasAlta = entry.getKey();
                montoCategoriaMasAlta = entry.getValue();
            }
        }

        // Días sin gasto sobre los días transcurridos de la semana (no cuenta días futuros aún no vividos)
        LocalDate hoy = LocalDate.now();
        int diasEvaluados = 0;
        int diasSinGasto = 0;
        for (int i = 0; i < 7; i++) {
            LocalDate dia = inicioSemana.plusDays(i);
            if (!dia.isAfter(hoy)) {
                diasEvaluados++;
                if (!fechasConGasto.contains(dia)) {
                    diasSinGasto++;
                }
            }
        }

        Map<LocalDate, EntradaDiario> entradasPorDia = new HashMap<>();
        for (EntradaDiario nota : notasSemana) {
            entradasPorDia.put(nota.getFecha(), nota);
        }

        // Une cada día de la semana (del más reciente al lunes) que tenga nota o movimientos, más hoy si cae en esta semana
        List<DiaDiario> dias = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate dia = inicioSemana.plusDays(i);
            EntradaDiario entrada = entradasPorDia.get(dia);
            List<Movimiento> movsDia = movimientosPorDia.getOrDefault(dia, List.of());
            if (entrada != null || !movsDia.isEmpty() || dia.equals(hoy)) {
                BalancePeriodo balanceDia = BalancePeriodo.calcular(movsDia);
                dias.add(new DiaDiario(
                        dia,
                        DiaDiario.formatearDia(dia),
                        entrada,
                        movsDia,
                        balanceDia.totalIngresos(),
                        balanceDia.totalGastos(),
                        balanceDia.balance()
                ));
            }
        }

        return new ResumenSemanalDiario(
                inicioSemana,
                finSemana,
                totalGastado,
                totalIngresado,
                categoriaMasAlta,
                montoCategoriaMasAlta,
                diasSinGasto,
                diasEvaluados,
                notasSemana,
                dias
        );
    }

    public List<DiaDiario> buscarEntradas(String texto, String etiqueta) {
        String textoLimpio = (texto == null || texto.isBlank()) ? null : texto.trim();
        String etiquetaLimpia = EntradaDiario.normalizarEtiquetas(etiqueta);
        if (etiquetaLimpia != null && etiquetaLimpia.contains(",")) {
            etiquetaLimpia = etiquetaLimpia.split(",")[0];
        }

        String patronTexto = textoLimpio == null ? null : "%" + textoLimpio.toLowerCase(Locale.ROOT) + "%";
        String patronEtiqueta = etiquetaLimpia == null ? null : "%," + etiquetaLimpia + ",%";

        List<EntradaDiario> encontradas = entradaDiarioRepository.buscar(patronTexto, patronEtiqueta);
        if (encontradas.isEmpty()) {
            return List.of();
        }

        LocalDate minFecha = encontradas.stream()
                .map(EntradaDiario::getFecha)
                .min(Comparator.naturalOrder())
                .orElseThrow();
        LocalDate maxFecha = encontradas.stream()
                .map(EntradaDiario::getFecha)
                .max(Comparator.naturalOrder())
                .orElseThrow();

        List<Movimiento> movimientosRango = movimientoRepository.buscarConFiltros(minFecha, maxFecha, null, null);
        Map<LocalDate, List<Movimiento>> movsPorFecha = new HashMap<>();
        for (Movimiento m : movimientosRango) {
            movsPorFecha.computeIfAbsent(m.getFecha(), f -> new ArrayList<>()).add(m);
        }

        List<DiaDiario> resultado = new ArrayList<>();
        for (EntradaDiario entrada : encontradas) {
            List<Movimiento> movsDia = movsPorFecha.getOrDefault(entrada.getFecha(), List.of());
            BalancePeriodo balance = BalancePeriodo.calcular(movsDia);
            resultado.add(new DiaDiario(
                    entrada.getFecha(),
                    DiaDiario.formatearDia(entrada.getFecha()),
                    entrada,
                    movsDia,
                    balance.totalIngresos(),
                    balance.totalGastos(),
                    balance.balance()
            ));
        }
        return resultado;
    }

    public List<String> listarEtiquetasUnicas() {
        Set<String> unicas = new TreeSet<>();
        for (EntradaDiario entrada : entradaDiarioRepository.findAllByOrderByFechaDesc()) {
            unicas.addAll(entrada.listaEtiquetas());
        }
        return List.copyOf(unicas);
    }
}
