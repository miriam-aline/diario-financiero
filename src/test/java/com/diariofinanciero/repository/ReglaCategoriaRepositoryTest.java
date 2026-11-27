package com.diariofinanciero.repository;

import com.diariofinanciero.domain.Categoria;
import com.diariofinanciero.domain.ReglaCategoria;
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
class ReglaCategoriaRepositoryTest {

    @Autowired
    private ReglaCategoriaRepository reglaCategoriaRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private EntityManager entityManager;

    private Categoria comida;
    private Categoria transporte;

    @BeforeEach
    void prepararDatos() {
        reglaCategoriaRepository.deleteAllInBatch();
        categoriaRepository.deleteAllInBatch();

        comida = categoriaRepository.save(new Categoria("Comida"));
        transporte = categoriaRepository.save(new Categoria("Transporte"));
        entityManager.flush();
    }

    @Test
    @DisplayName("Debe listar reglas con categoría ordenadas de mayor a menor longitud y alfabéticamente")
    void findAllConCategoriaOrdenadas() {
        reglaCategoriaRepository.saveAll(List.of(
                new ReglaCategoria("uber", transporte),
                new ReglaCategoria("uber eats", comida),
                new ReglaCategoria("cafe", comida)
        ));
        entityManager.flush();
        entityManager.clear();

        List<ReglaCategoria> reglas = reglaCategoriaRepository.findAllConCategoriaOrdenadas();

        assertEquals(3, reglas.size());
        assertEquals("uber eats", reglas.get(0).getTextoContiene());
        assertEquals("Comida", reglas.get(0).getCategoria().getNombre());
        assertEquals("cafe", reglas.get(1).getTextoContiene());
        assertEquals("uber", reglas.get(2).getTextoContiene());
        assertEquals("Transporte", reglas.get(2).getCategoria().getNombre());
    }

    @Test
    @DisplayName("Debe buscar regla por textoContiene ignorando mayúsculas y minúsculas")
    void findByTextoContieneIgnoreCase() {
        reglaCategoriaRepository.save(new ReglaCategoria("Supermercado", comida));

        assertTrue(reglaCategoriaRepository.findByTextoContieneIgnoreCase("SUPERMERCADO").isPresent());
        assertTrue(reglaCategoriaRepository.findByTextoContieneIgnoreCase("otro").isEmpty());
    }
}
