package com.diariofinanciero.service;

import com.diariofinanciero.domain.Categoria;
import com.diariofinanciero.domain.CompraMSI;
import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.Dinero;
import com.diariofinanciero.domain.MensualidadMSI;
import com.diariofinanciero.domain.Movimiento;
import com.diariofinanciero.domain.OrigenMovimiento;
import com.diariofinanciero.domain.TipoMovimiento;
import com.diariofinanciero.repository.CategoriaRepository;
import com.diariofinanciero.repository.CompraMSIRepository;
import com.diariofinanciero.repository.MovimientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class CompraMSIService {

    private final CompraMSIRepository compraMSIRepository;
    private final CuentaService cuentaService;
    private final CategoriaService categoriaService;
    private final CategoriaRepository categoriaRepository;
    private final MovimientoRepository movimientoRepository;

    public CompraMSIService(
            CompraMSIRepository compraMSIRepository,
            CuentaService cuentaService,
            CategoriaService categoriaService,
            CategoriaRepository categoriaRepository,
            MovimientoRepository movimientoRepository
    ) {
        this.compraMSIRepository = compraMSIRepository;
        this.cuentaService = cuentaService;
        this.categoriaService = categoriaService;
        this.categoriaRepository = categoriaRepository;
        this.movimientoRepository = movimientoRepository;
    }

    public List<CompraMSI> listarTodas() {
        return compraMSIRepository.findAllConRelaciones();
    }

    @Transactional
    public CompraMSI crear(
            String descripcion,
            BigDecimal montoTotal,
            int numeroMensualidades,
            int mensualidadesPagadas,
            Long cuentaId,
            Long categoriaId,
            LocalDate fechaPrimerCargo
    ) {
        Cuenta cuenta = cuentaService.obtenerPorId(cuentaId);
        Categoria categoria = categoriaId == null ? null : categoriaService.obtenerPorId(categoriaId);
        CompraMSI compra = new CompraMSI(
                descripcion,
                montoTotal,
                numeroMensualidades,
                mensualidadesPagadas,
                cuenta,
                categoria,
                fechaPrimerCargo
        );
        return compraMSIRepository.save(compra);
    }

    @Transactional
    public MensualidadMSI registrarSiguienteMensualidad(Long id, boolean registrarMovimiento) {
        CompraMSI compra = compraMSIRepository.findByIdConRelaciones(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Compra a MSI no encontrada con id: " + id));

        MensualidadMSI cuota = compra.registrarPagoSiguienteMensualidad();
        compraMSIRepository.save(compra);

        if (registrarMovimiento) {
            Categoria categoria = compra.getCategoria() != null
                    ? compra.getCategoria()
                    : categoriaRepository.findByNombreIgnoreCase(ImportacionCsvService.NOMBRE_SIN_CATEGORIA)
                            .orElseGet(() -> categoriaService.listarTodas().getFirst());

            String clave = "MSI-" + compra.getId() + "-" + cuota.numero();
            if (movimientoRepository.findHashesExistentes(Set.of(clave)).isEmpty()) {
                String desc = compra.getDescripcion() + " (MSI " + cuota.numero() + "/" + compra.getNumeroMensualidades() + ")";
                if (desc.length() > 255) {
                    desc = desc.substring(0, 255);
                }
                Movimiento mov = new Movimiento(
                        cuota.fechaCargo(),
                        cuota.monto(),
                        TipoMovimiento.GASTO,
                        categoria,
                        compra.getCuenta(),
                        desc,
                        OrigenMovimiento.MANUAL,
                        clave
                );
                movimientoRepository.save(mov);
            }
        }

        return cuota;
    }

    @Transactional
    public void eliminar(Long id) {
        CompraMSI compra = compraMSIRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Compra a MSI no encontrada con id: " + id));
        compraMSIRepository.delete(compra);
    }

    public BigDecimal calcularSaldoPendienteTotal(List<CompraMSI> compras) {
        BigDecimal total = Dinero.CERO;
        for (CompraMSI c : compras) {
            total = Dinero.sumar(total, c.saldoPendiente());
        }
        return total;
    }

    public BigDecimal calcularCompromisoMensualActual(List<CompraMSI> compras) {
        BigDecimal total = Dinero.CERO;
        for (CompraMSI c : compras) {
            MensualidadMSI siguiente = c.proximaMensualidad();
            if (siguiente != null) {
                total = Dinero.sumar(total, siguiente.monto());
            }
        }
        return total;
    }
}
