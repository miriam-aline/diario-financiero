package com.diariofinanciero.repository;

import com.diariofinanciero.domain.Recurrente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RecurrenteRepository extends JpaRepository<Recurrente, Long> {

    @Query("""
            SELECT r
            FROM Recurrente r
            JOIN FETCH r.categoria
            JOIN FETCH r.cuenta
            ORDER BY r.activo DESC, r.proximoCobro ASC, r.id ASC
            """)
    List<Recurrente> findAllConRelaciones();

    @Query("""
            SELECT r
            FROM Recurrente r
            JOIN FETCH r.categoria
            JOIN FETCH r.cuenta
            WHERE r.activo = true
              AND r.proximoCobro <= :hastaFecha
            ORDER BY r.proximoCobro ASC, r.id ASC
            """)
    List<Recurrente> findPendientesHasta(@Param("hastaFecha") LocalDate hastaFecha);
}
