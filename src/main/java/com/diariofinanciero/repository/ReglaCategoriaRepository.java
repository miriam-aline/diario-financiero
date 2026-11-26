package com.diariofinanciero.repository;

import com.diariofinanciero.domain.ReglaCategoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReglaCategoriaRepository extends JpaRepository<ReglaCategoria, Long> {

    @Query("""
            SELECT r
            FROM ReglaCategoria r
            JOIN FETCH r.categoria
            ORDER BY LENGTH(r.textoContiene) DESC, r.textoContiene ASC
            """)
    List<ReglaCategoria> findAllConCategoriaOrdenadas();

    Optional<ReglaCategoria> findByTextoContieneIgnoreCase(String textoContiene);
}
