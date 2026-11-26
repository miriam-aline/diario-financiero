package com.diariofinanciero.repository;

import com.diariofinanciero.domain.Categoria;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findAllByOrderByNombreAsc();

    Optional<Categoria> findByNombreIgnoreCase(String nombre);

    @Query("""
            SELECT m.categoria
            FROM Movimiento m
            GROUP BY m.categoria
            ORDER BY MAX(m.fecha) DESC, MAX(m.id) DESC
            """)
    List<Categoria> findCategoriasRecientes(Pageable pageable);
}
