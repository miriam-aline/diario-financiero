package com.diariofinanciero.repository;

import com.diariofinanciero.domain.EntradaDiario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class EntradaDiarioRepositoryTest {

    @Autowired
    private EntradaDiarioRepository entradaDiarioRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void prepararDatos() {
        entradaDiarioRepository.deleteAllInBatch();
        entradaDiarioRepository.saveAll(List.of(
                new EntradaDiario(LocalDate.of(2026, 5, 1), "Pagué la renta a tiempo", 4, "vivienda,ahorro"),
                new EntradaDiario(LocalDate.of(2026, 5, 10), "Compré café y pan", 3, "cafe,ahorrorapido"),
                new EntradaDiario(LocalDate.of(2026, 5, 20), "Buen cierre de quincena", 5, "ahorro,meta")
        ));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Debe encontrar entrada por fecha exacta")
    void buscarPorFecha() {
        assertEquals(
                "Compré café y pan",
                entradaDiarioRepository.findByFecha(LocalDate.of(2026, 5, 10)).orElseThrow().getNota()
        );
        assertTrue(entradaDiarioRepository.findByFecha(LocalDate.of(2026, 5, 2)).isEmpty());
    }

    @Test
    @DisplayName("Debe listar entradas por rango de fechas y todas en orden descendente")
    void listarPorRangoYTodasDescendente() {
        List<EntradaDiario> rango = entradaDiarioRepository.findByFechaBetweenOrderByFechaDesc(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 10)
        );
        assertEquals(2, rango.size());
        assertEquals(LocalDate.of(2026, 5, 10), rango.get(0).getFecha());
        assertEquals(LocalDate.of(2026, 5, 1), rango.get(1).getFecha());

        List<EntradaDiario> todas = entradaDiarioRepository.findAllByOrderByFechaDesc();
        assertEquals(3, todas.size());
        assertEquals(LocalDate.of(2026, 5, 20), todas.get(0).getFecha());
        assertEquals(LocalDate.of(2026, 5, 1), todas.get(2).getFecha());
    }

    @Test
    @DisplayName("Debe buscar combinando patrón de texto y etiqueta exacta delimitada por comas")
    void buscarPorTextoYEtiqueta() {
        List<EntradaDiario> todas = entradaDiarioRepository.buscar(null, null);
        assertEquals(3, todas.size());

        List<EntradaDiario> porTextoNota = entradaDiarioRepository.buscar("%renta%", null);
        assertEquals(1, porTextoNota.size());
        assertEquals(LocalDate.of(2026, 5, 1), porTextoNota.get(0).getFecha());

        List<EntradaDiario> porTextoEtiqueta = entradaDiarioRepository.buscar("%meta%", null);
        assertEquals(1, porTextoEtiqueta.size());
        assertEquals(LocalDate.of(2026, 5, 20), porTextoEtiqueta.get(0).getFecha());

        List<EntradaDiario> porEtiquetaExacta = entradaDiarioRepository.buscar(null, "%,ahorro,%");
        assertEquals(2, porEtiquetaExacta.size());
        assertEquals(LocalDate.of(2026, 5, 20), porEtiquetaExacta.get(0).getFecha());
        assertEquals(LocalDate.of(2026, 5, 1), porEtiquetaExacta.get(1).getFecha());

        List<EntradaDiario> combinado = entradaDiarioRepository.buscar("%quincena%", "%,ahorro,%");
        assertEquals(1, combinado.size());
        assertEquals(LocalDate.of(2026, 5, 20), combinado.get(0).getFecha());
    }
}
