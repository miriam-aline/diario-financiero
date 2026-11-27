package com.diariofinanciero.repository;

import com.diariofinanciero.domain.Categoria;
import com.diariofinanciero.domain.CompraMSI;
import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.TipoCuenta;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class CompraMSIRepositoryTest {

    @Autowired
    private CompraMSIRepository compraMSIRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private EntityManager entityManager;

    private Cuenta tarjeta;
    private Categoria tecnologia;

    @BeforeEach
    void prepararDatos() {
        compraMSIRepository.deleteAllInBatch();
        cuentaRepository.deleteAllInBatch();
        categoriaRepository.deleteAllInBatch();

        tarjeta = cuentaRepository.save(new Cuenta("Tarjeta Oro", TipoCuenta.CREDITO, 15, 5));
        tecnologia = categoriaRepository.save(new Categoria("Tecnología"));
        entityManager.flush();
    }

    @Test
    @DisplayName("Debe listar todas las compras MSI con relaciones cargadas ordenadas por fechaPrimerCargo desc e id desc")
    void findAllConRelacionesOrdenado() {
        CompraMSI antigua = compraMSIRepository.save(new CompraMSI(
                "Audífonos", new BigDecimal("1200.00"), 3, 0, tarjeta, null, LocalDate.of(2026, 1, 10)
        ));
        CompraMSI reciente1 = compraMSIRepository.save(new CompraMSI(
                "Monitor", new BigDecimal("6000.00"), 6, 1, tarjeta, tecnologia, LocalDate.of(2026, 3, 15)
        ));
        CompraMSI reciente2 = compraMSIRepository.save(new CompraMSI(
                "Teclado", new BigDecimal("1800.00"), 3, 0, tarjeta, tecnologia, LocalDate.of(2026, 3, 15)
        ));
        entityManager.flush();
        entityManager.clear();

        List<CompraMSI> resultado = compraMSIRepository.findAllConRelaciones();

        assertEquals(3, resultado.size());
        assertEquals(reciente2.getId(), resultado.get(0).getId());
        assertEquals(reciente1.getId(), resultado.get(1).getId());
        assertEquals(antigua.getId(), resultado.get(2).getId());

        assertEquals("Tarjeta Oro", resultado.get(0).getCuenta().getNombre());
        assertEquals("Tecnología", resultado.get(0).getCategoria().getNombre());
        assertNull(resultado.get(2).getCategoria());
    }

    @Test
    @DisplayName("Debe encontrar compra MSI por id con sus relaciones cargadas")
    void findByIdConRelaciones() {
        CompraMSI guardada = compraMSIRepository.save(new CompraMSI(
                "Laptop", new BigDecimal("18000.00"), 12, 2, tarjeta, tecnologia, LocalDate.of(2026, 2, 1)
        ));
        entityManager.flush();
        entityManager.clear();

        CompraMSI encontrada = compraMSIRepository.findByIdConRelaciones(guardada.getId()).orElseThrow();
        assertEquals("Laptop", encontrada.getDescripcion());
        assertEquals("Tarjeta Oro", encontrada.getCuenta().getNombre());
        assertEquals("Tecnología", encontrada.getCategoria().getNombre());

        assertTrue(compraMSIRepository.findByIdConRelaciones(-999L).isEmpty());
    }
}
