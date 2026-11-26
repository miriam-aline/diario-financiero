package com.diariofinanciero.repository;

import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.TipoCuenta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    List<Cuenta> findAllByOrderByNombreAsc();

    List<Cuenta> findByTipoOrderByNombreAsc(TipoCuenta tipo);

    Optional<Cuenta> findByNombreIgnoreCase(String nombre);
}
