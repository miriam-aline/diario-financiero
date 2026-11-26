package com.diariofinanciero.repository;

import com.diariofinanciero.domain.EntradaDiario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EntradaDiarioRepository extends JpaRepository<EntradaDiario, Long> {

    Optional<EntradaDiario> findByFecha(LocalDate fecha);

    List<EntradaDiario> findByFechaBetweenOrderByFechaDesc(LocalDate desde, LocalDate hasta);

    List<EntradaDiario> findAllByOrderByFechaDesc();

    @Query("""
            SELECT e
            FROM EntradaDiario e
            WHERE (:patronTexto IS NULL
                   OR LOWER(e.nota) LIKE :patronTexto
                   OR LOWER(COALESCE(e.etiquetas, '')) LIKE :patronTexto)
              AND (:patronEtiqueta IS NULL
                   OR CONCAT(',', LOWER(COALESCE(e.etiquetas, '')), ',') LIKE :patronEtiqueta)
            ORDER BY e.fecha DESC
            """)
    List<EntradaDiario> buscar(
            @Param("patronTexto") String patronTexto,
            @Param("patronEtiqueta") String patronEtiqueta
    );
}
