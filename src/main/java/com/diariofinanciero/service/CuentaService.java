package com.diariofinanciero.service;

import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.TipoCuenta;
import com.diariofinanciero.repository.CuentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CuentaService {

    private final CuentaRepository cuentaRepository;

    public CuentaService(CuentaRepository cuentaRepository) {
        this.cuentaRepository = cuentaRepository;
    }

    public List<Cuenta> listarTodas() {
        return cuentaRepository.findAllByOrderByNombreAsc();
    }

    public List<Cuenta> listarPorTipo(TipoCuenta tipo) {
        return cuentaRepository.findByTipoOrderByNombreAsc(tipo);
    }

    public Cuenta obtenerPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El identificador de cuenta es obligatorio");
        }
        return cuentaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta no encontrada con id: " + id));
    }

    @Transactional
    public Cuenta crearTarjetaCredito(String nombre, Integer diaCorte, Integer diaPago) {
        if (diaCorte == null || diaPago == null) {
            throw new IllegalArgumentException("El día de corte y el día de pago son obligatorios");
        }
        Cuenta nueva = new Cuenta(nombre, TipoCuenta.CREDITO, diaCorte, diaPago);
        if (cuentaRepository.findByNombreIgnoreCase(nueva.getNombre()).isPresent()) {
            throw new IllegalArgumentException("Ya existe una cuenta llamada \"" + nueva.getNombre() + "\"");
        }
        return cuentaRepository.save(nueva);
    }

    @Transactional
    public Cuenta actualizarTarjetaCredito(Long id, String nombre, Integer diaCorte, Integer diaPago) {
        Cuenta existente = obtenerPorId(id);
        String nombreLimpio = nombre == null ? "" : nombre.trim();
        Optional<Cuenta> duplicada = cuentaRepository.findByNombreIgnoreCase(nombreLimpio);
        if (duplicada.isPresent() && !duplicada.get().getId().equals(id)) {
            throw new IllegalArgumentException("Ya existe otra cuenta llamada \"" + nombreLimpio + "\"");
        }
        existente.actualizarTarjetaCredito(nombre, diaCorte, diaPago);
        return cuentaRepository.save(existente);
    }
}
