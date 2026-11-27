package com.diariofinanciero.repository;

import com.diariofinanciero.domain.Categoria;
import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.Periodicidad;
import com.diariofinanciero.domain.Recurrente;
import com.diariofinanciero.domain.TipoCuenta;
import com.diariofinanciero.domain.TipoMovimiento;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class RecurrenteRepositoryTest {

    @Autowired
    private RecurrenteRepository recurrenteRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private EntityManager entityManager;

    private Categoria servicios;
    private Cuenta debito;

    @BeforeEach
    void prepararDatos() {
        recurrenteRepository.deleteAllInBatch();
        categoriaRepository.deleteAllInBatch();
        cuentaRepository.deleteAllInBatch();

        servicios = categoriaRepository.save(new Categoria("Servicios"));
        debito = cuentaRepository.save(new Cuenta("Débito", TipoCuenta.DEBITO));
        entityManager.flush();
    }

    @Test
    @DisplayName("Debe listar todos los recurrentes con relaciones ordenados por activo desc y próximo cobro asc")
    void findAllConRelacionesOrdenado() {
        Recurrente inactivo = new Recurrente(
                "Gimnasio", new BigDecimal("600"), TipoMovimiento.GASTO, servicios, debito,
                Periodicidad.MENSUAL, LocalDate.of(2026, 5, 1)
        );
        inactivo.alternarActivo();
        recurrenteRepository.save(inactivo);

        Recurrente activoTarde = recurrenteRepository.save(new Recurrente(
                "Internet", new BigDecimal("500"), TipoMovimiento.GASTO, servicios, debito,
                Periodicidad.MENSUAL, LocalDate.of(2026, 5, 20)
        ));
        Recurrente activoPronto = recurrenteRepository.save(new Recurrente(
                "Agua", new BigDecimal("200"), TipoMovimiento.GASTO, servicios, debito,
                Periodicidad.MENSUAL, LocalDate.of(2026, 5, 5)
        ));
        entityManager.flush();
        entityManager.clear();

        List<Recurrente> resultado = recurrenteRepository.findAllConRelaciones();

        assertEquals(3, resultado.size());
        assertEquals(activoPronto.getId(), resultado.get(0).getId());
        assertEquals(activoTarde.getId(), resultado.get(1).getId());
        assertEquals(inactivo.getId(), resultado.get(2).getId());
        assertEquals("Servicios", resultado.get(0).getCategoria().getNombre());
        assertEquals("Débito", resultado.get(0).getCuenta().getNombre());
    }

    @Test
    @DisplayName("Debe encontrar solo recurrentes activos con proximoCobro menor o igual a la fecha límite")
    void findPendientesHasta() {
        Recurrente inactivo = new Recurrente(
                "Inactivo", new BigDecimal("100"), TipoMovimiento.GASTO, servicios, debito,
                Periodicidad.MENSUAL, LocalDate.of(2026, 5, 1)
        );
        inactivo.alternarActivo();
        recurrenteRepository.save(inactivo);

        Recurrente pasado = recurrenteRepository.save(new Recurrente(
                "Luz", new BigDecimal("350"), TipoMovimiento.GASTO, servicios, debito,
                Periodicidad.MENSUAL, LocalDate.of(2026, 5, 10)
        ));
        Recurrente limite = recurrenteRepository.save(new Recurrente(
                "Gas", new BigDecimal("400"), TipoMovimiento.GASTO, servicios, debito,
                Periodicidad.MENSUAL, LocalDate.of(2026, 5, 15)
        ));
        recurrenteRepository.save(new Recurrente(
                "Futuro", new BigDecimal("250"), TipoMovimiento.GASTO, servicios, debito,
                Periodicidad.MENSUAL, LocalDate.of(2026, 5, 16)
        ));
        entityManager.flush();
        entityManager.clear();

        List<Recurrente> pendientes = recurrenteRepository.findPendientesHasta(LocalDate.of(2026, 5, 15));

        assertEquals(2, pendientes.size());
        assertEquals(pasado.getId(), pendientes.get(0).getId());
        assertEquals(limite.getId(), pendientes.get(1).getId());
    }
}
