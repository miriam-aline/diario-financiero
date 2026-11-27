package com.diariofinanciero.repository;

import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.TipoCuenta;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class CuentaRepositoryTest {

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void limpiarDatos() {
        cuentaRepository.deleteAllInBatch();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Debe listar todas las cuentas ordenadas alfabéticamente por nombre")
    void listarOrdenadas() {
        cuentaRepository.saveAll(List.of(
                new Cuenta("Nu", TipoCuenta.CREDITO, 15, 5),
                new Cuenta("Cartera", TipoCuenta.EFECTIVO),
                new Cuenta("BBVA", TipoCuenta.DEBITO)
        ));

        List<String> nombres = cuentaRepository.findAllByOrderByNombreAsc().stream()
                .map(Cuenta::getNombre)
                .toList();

        assertEquals(List.of("BBVA", "Cartera", "Nu"), nombres);
    }

    @Test
    @DisplayName("Debe filtrar cuentas por tipo ordenadas por nombre")
    void filtrarPorTipoOrdenado() {
        cuentaRepository.saveAll(List.of(
                new Cuenta("Santander", TipoCuenta.CREDITO, 20, 10),
                new Cuenta("Amex", TipoCuenta.CREDITO, 10, 25),
                new Cuenta("Efectivo", TipoCuenta.EFECTIVO)
        ));

        List<String> creditos = cuentaRepository.findByTipoOrderByNombreAsc(TipoCuenta.CREDITO).stream()
                .map(Cuenta::getNombre)
                .toList();

        assertEquals(List.of("Amex", "Santander"), creditos);
    }

    @Test
    @DisplayName("Debe buscar cuenta por nombre ignorando mayúsculas y minúsculas")
    void buscarPorNombreIgnoreCase() {
        cuentaRepository.save(new Cuenta("Débito Principal", TipoCuenta.DEBITO));

        assertTrue(cuentaRepository.findByNombreIgnoreCase("débito principal").isPresent());
        assertTrue(cuentaRepository.findByNombreIgnoreCase("No Existe").isEmpty());
    }
}
