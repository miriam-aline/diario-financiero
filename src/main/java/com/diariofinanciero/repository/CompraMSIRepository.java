package com.diariofinanciero.repository;

import com.diariofinanciero.domain.CompraMSI;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompraMSIRepository extends JpaRepository<CompraMSI, Long> {

    @Query("""
            SELECT c
            FROM CompraMSI c
            JOIN FETCH c.cuenta
            LEFT JOIN FETCH c.categoria
            ORDER BY c.fechaPrimerCargo DESC, c.id DESC
            """)
    List<CompraMSI> findAllConRelaciones();

    @Query("""
            SELECT c
            FROM CompraMSI c
            JOIN FETCH c.cuenta
            LEFT JOIN FETCH c.categoria
            WHERE c.id = :id
            """)
    Optional<CompraMSI> findByIdConRelaciones(@Param("id") Long id);
}
