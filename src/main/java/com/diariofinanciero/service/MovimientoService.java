package com.diariofinanciero.service;

import com.diariofinanciero.domain.Categoria;
import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.Movimiento;
import com.diariofinanciero.domain.TipoMovimiento;
import com.diariofinanciero.repository.MovimientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class MovimientoService {

    private final MovimientoRepository movimientoRepository;
    private final CategoriaService categoriaService;
    private final CuentaService cuentaService;

    public MovimientoService(
            MovimientoRepository movimientoRepository,
            CategoriaService categoriaService,
            CuentaService cuentaService
    ) {
        this.movimientoRepository = movimientoRepository;
        this.categoriaService = categoriaService;
        this.cuentaService = cuentaService;
    }

    public List<Movimiento> listar(FiltroMovimiento filtro) {
        FiltroMovimiento criterio = filtro == null ? FiltroMovimiento.vacio() : filtro;
        return movimientoRepository.buscarConFiltros(
                criterio.desde(),
                criterio.hasta(),
                criterio.categoriaId(),
                criterio.patronBusquedaSql()
        );
    }

    public Movimiento obtenerPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El identificador del movimiento es obligatorio");
        }
        return movimientoRepository.findByIdConRelaciones(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Movimiento no encontrado con id: " + id));
    }

    @Transactional
    public Movimiento registrar(
            LocalDate fecha,
            BigDecimal monto,
            TipoMovimiento tipo,
            Long categoriaId,
            Long cuentaId,
            String descripcion
    ) {
        Categoria categoria = categoriaService.obtenerPorId(categoriaId);
        Cuenta cuenta = cuentaService.obtenerPorId(cuentaId);
        Movimiento movimiento = Movimiento.manual(fecha, monto, tipo, categoria, cuenta, descripcion);
        return movimientoRepository.save(movimiento);
    }

    @Transactional
    public Movimiento actualizar(
            Long id,
            LocalDate fecha,
            BigDecimal monto,
            TipoMovimiento tipo,
            Long categoriaId,
            Long cuentaId,
            String descripcion
    ) {
        Movimiento existente = obtenerPorId(id);
        Categoria categoria = categoriaService.obtenerPorId(categoriaId);
        Cuenta cuenta = cuentaService.obtenerPorId(cuentaId);
        existente.actualizar(fecha, monto, tipo, categoria, cuenta, descripcion);
        return movimientoRepository.save(existente);
    }

    @Transactional
    public void eliminar(Long id) {
        Movimiento existente = obtenerPorId(id);
        movimientoRepository.delete(existente);
    }

    public BalancePeriodo calcularBalance(List<Movimiento> movimientos) {
        return BalancePeriodo.calcular(movimientos);
    }
}
