package com.diariofinanciero.repository;

import com.diariofinanciero.domain.Categoria;
import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.Movimiento;
import com.diariofinanciero.domain.TipoCuenta;
import com.diariofinanciero.domain.TipoMovimiento;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class CategoriaRepositoryTest {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void limpiarDatos() {
        movimientoRepository.deleteAllInBatch();
        categoriaRepository.deleteAllInBatch();
        cuentaRepository.deleteAllInBatch();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Debe listar todas las categorías ordenadas alfabéticamente por nombre")
    void listarOrdenadasPorNombre() {
        categoriaRepository.saveAll(List.of(
                new Categoria("Viajes"),
                new Categoria("Ahorro"),
                new Categoria("Comida")
        ));

        List<String> nombres = categoriaRepository.findAllByOrderByNombreAsc().stream()
                .map(Categoria::getNombre)
                .toList();

        assertEquals(List.of("Ahorro", "Comida", "Viajes"), nombres);
    }

    @Test
    @DisplayName("Debe buscar categoría por nombre ignorando mayúsculas y minúsculas")
    void buscarPorNombreIgnoreCase() {
        categoriaRepository.save(new Categoria("Servicios"));

        assertTrue(categoriaRepository.findByNombreIgnoreCase("sErViCiOs").isPresent());
        assertEquals("Servicios", categoriaRepository.findByNombreIgnoreCase("SERVICIOS").orElseThrow().getNombre());
        assertTrue(categoriaRepository.findByNombreIgnoreCase("Inexistente").isEmpty());
    }

    @Test
    @DisplayName("Debe retornar categorías recientes ordenadas por última fecha e id de movimiento")
    void encontrarCategoriasRecientes() {
        Categoria comida = categoriaRepository.save(new Categoria("Comida"));
        Categoria transporte = categoriaRepository.save(new Categoria("Transporte"));
        Categoria ocio = categoriaRepository.save(new Categoria("Ocio"));
        Cuenta cuenta = cuentaRepository.save(new Cuenta("Efectivo", TipoCuenta.EFECTIVO));

        LocalDate hoy = LocalDate.of(2026, 5, 20);
        movimientoRepository.save(Movimiento.manual(hoy.minusDays(5), new BigDecimal("50"), TipoMovimiento.GASTO, comida, cuenta, "Tacos"));
        movimientoRepository.save(Movimiento.manual(hoy.minusDays(1), new BigDecimal("30"), TipoMovimiento.GASTO, transporte, cuenta, "Metro"));
        movimientoRepository.save(Movimiento.manual(hoy.minusDays(1), new BigDecimal("200"), TipoMovimiento.GASTO, ocio, cuenta, "Cine"));
        entityManager.flush();

        List<Categoria> recientes = categoriaRepository.findCategoriasRecientes(PageRequest.of(0, 2));

        assertEquals(2, recientes.size());
        assertEquals("Ocio", recientes.get(0).getNombre());
        assertEquals("Transporte", recientes.get(1).getNombre());
    }
}
