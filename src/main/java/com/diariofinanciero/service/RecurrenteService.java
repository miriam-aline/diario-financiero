package com.diariofinanciero.service;

import com.diariofinanciero.domain.Categoria;
import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.Movimiento;
import com.diariofinanciero.domain.OrigenMovimiento;
import com.diariofinanciero.domain.Periodicidad;
import com.diariofinanciero.domain.Recurrente;
import com.diariofinanciero.domain.TipoMovimiento;
import com.diariofinanciero.repository.MovimientoRepository;
import com.diariofinanciero.repository.RecurrenteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class RecurrenteService {

    private static final Logger log = LoggerFactory.getLogger(RecurrenteService.class);
    private static final int MAX_CICLOS_POR_EJECUCION = 60;

    private final RecurrenteRepository recurrenteRepository;
    private final MovimientoRepository movimientoRepository;
    private final CategoriaService categoriaService;
    private final CuentaService cuentaService;

    public RecurrenteService(
            RecurrenteRepository recurrenteRepository,
            MovimientoRepository movimientoRepository,
            CategoriaService categoriaService,
            CuentaService cuentaService
    ) {
        this.recurrenteRepository = recurrenteRepository;
        this.movimientoRepository = movimientoRepository;
        this.categoriaService = categoriaService;
        this.cuentaService = cuentaService;
    }

    public List<Recurrente> listarTodos() {
        return recurrenteRepository.findAllConRelaciones();
    }

    @Transactional
    public Recurrente crear(
            String descripcion,
            BigDecimal monto,
            TipoMovimiento tipo,
            Long categoriaId,
            Long cuentaId,
            Periodicidad periodicidad,
            LocalDate proximoCobro
    ) {
        Categoria categoria = categoriaService.obtenerPorId(categoriaId);
        Cuenta cuenta = cuentaService.obtenerPorId(cuentaId);
        Recurrente recurrente = new Recurrente(
                descripcion, monto, tipo, categoria, cuenta, periodicidad, proximoCobro);
        return recurrenteRepository.save(recurrente);
    }

    @Transactional
    public void alternarActivo(Long id) {
        Recurrente recurrente = obtenerPorId(id);
        recurrente.alternarActivo();
        recurrenteRepository.save(recurrente);
    }

    @Transactional
    public void eliminar(Long id) {
        Recurrente recurrente = obtenerPorId(id);
        recurrenteRepository.delete(recurrente);
    }

    /**
     * Se ejecuta automáticamente todos los días a las 00:05 para generar movimientos vencidos.
     */
    @Scheduled(cron = "${diario.recurrentes.cron:0 5 0 * * *}")
    @Transactional
    public void ejecutarProgramadoDiario() {
        int generados = generarMovimientosPendientes(LocalDate.now());
        if (generados > 0) {
            log.info("Recurrentes ejecutados automáticamente: {} movimiento(s) generado(s)", generados);
        }
    }

    /**
     * Al arrancar la app local, genera cualquier recurrente que haya vencido mientras el equipo estuvo apagado.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void ejecutarAlArrancar() {
        int generados = generarMovimientosPendientes(LocalDate.now());
        if (generados > 0) {
            log.info("Recurrentes generados al iniciar la aplicación: {} movimiento(s)", generados);
        }
    }

    @Transactional
    public int generarMovimientosPendientes(LocalDate hastaFecha) {
        LocalDate corte = hastaFecha == null ? LocalDate.now() : hastaFecha;
        List<Recurrente> pendientes = recurrenteRepository.findPendientesHasta(corte);
        int generados = 0;

        for (Recurrente recurrente : pendientes) {
            int iteraciones = 0;
            while (!recurrente.getProximoCobro().isAfter(corte) && iteraciones < MAX_CICLOS_POR_EJECUCION) {
                LocalDate fechaCobro = recurrente.getProximoCobro();
                String claveIdempotente = "REC-" + recurrente.getId() + "-" + fechaCobro;

                if (movimientoRepository.findHashesExistentes(Set.of(claveIdempotente)).isEmpty()) {
                    Movimiento movimiento = new Movimiento(
                            fechaCobro,
                            recurrente.getMonto(),
                            recurrente.getTipo(),
                            recurrente.getCategoria(),
                            recurrente.getCuenta(),
                            recurrente.getDescripcion(),
                            OrigenMovimiento.MANUAL,
                            claveIdempotente
                    );
                    movimientoRepository.save(movimiento);
                    generados++;
                }

                recurrente.avanzarProximoCobro();
                iteraciones++;
            }
            recurrenteRepository.save(recurrente);
        }
        return generados;
    }

    private Recurrente obtenerPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El identificador del recurrente es obligatorio");
        }
        return recurrenteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Recurrente no encontrado con id: " + id));
    }
}
