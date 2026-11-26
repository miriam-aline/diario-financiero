package com.diariofinanciero.repository;

import com.diariofinanciero.domain.Movimiento;
import com.diariofinanciero.domain.TipoMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

    @Query("""
            SELECT m
            FROM Movimiento m
            JOIN FETCH m.categoria
            JOIN FETCH m.cuenta
            WHERE m.id = :id
            """)
    Optional<Movimiento> findByIdConRelaciones(@Param("id") Long id);

    @Query("""
            SELECT m
            FROM Movimiento m
            JOIN FETCH m.categoria c
            JOIN FETCH m.cuenta cu
            WHERE (:desde IS NULL OR m.fecha >= :desde)
              AND (:hasta IS NULL OR m.fecha <= :hasta)
              AND (:categoriaId IS NULL OR c.id = :categoriaId)
              AND (:patronTexto IS NULL OR LOWER(m.descripcion) LIKE :patronTexto)
            ORDER BY m.fecha DESC, m.id DESC
            """)
    List<Movimiento> buscarConFiltros(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("categoriaId") Long categoriaId,
            @Param("patronTexto") String patronTexto
    );

    @Query("""
            SELECT m.categoria.id AS categoriaId, COALESCE(SUM(m.monto), 0) AS total
            FROM Movimiento m
            WHERE m.tipo = :tipo
              AND m.fecha >= :desde
              AND m.fecha <= :hasta
            GROUP BY m.categoria.id
            """)
    List<GastoPorCategoriaProjection> sumarPorCategoriaEnPeriodo(
            @Param("tipo") TipoMovimiento tipo,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta
    );

    @Query("""
            SELECT YEAR(m.fecha) AS anio, MONTH(m.fecha) AS mes, m.tipo AS tipo, COALESCE(SUM(m.monto), 0) AS total
            FROM Movimiento m
            WHERE m.fecha >= :desde
              AND m.fecha <= :hasta
            GROUP BY YEAR(m.fecha), MONTH(m.fecha), m.tipo
            ORDER BY YEAR(m.fecha), MONTH(m.fecha)
            """)
    List<ResumenMensualProjection> sumarPorMesYTipo(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta
    );

    @Query("""
            SELECT m.fecha AS fecha, COALESCE(SUM(m.monto), 0) AS total
            FROM Movimiento m
            WHERE m.tipo = :tipo
              AND m.fecha >= :desde
              AND m.fecha <= :hasta
            GROUP BY m.fecha
            ORDER BY m.fecha ASC
            """)
    List<GastoDiarioProjection> sumarPorDiaEnPeriodo(
            @Param("tipo") TipoMovimiento tipo,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta
    );

    @Query("""
            SELECT m
            FROM Movimiento m
            JOIN FETCH m.categoria
            JOIN FETCH m.cuenta
            WHERE m.cuenta.id = :cuentaId
              AND m.fecha >= :desde
              AND m.fecha <= :hasta
            ORDER BY m.fecha DESC, m.id DESC
            """)
    List<Movimiento> buscarPorCuentaEnPeriodo(
            @Param("cuentaId") Long cuentaId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta
    );

    @Query("""
            SELECT m.hashImportacion
            FROM Movimiento m
            WHERE m.hashImportacion IN :hashes
            """)
    Set<String> findHashesExistentes(@Param("hashes") Collection<String> hashes);

    @Query("""
            SELECT m
            FROM Movimiento m
            JOIN FETCH m.categoria c
            JOIN FETCH m.cuenta cu
            WHERE LOWER(c.nombre) = LOWER(:nombreCategoria)
            ORDER BY m.fecha DESC, m.id DESC
            """)
    List<Movimiento> buscarPorCategoriaNombre(@Param("nombreCategoria") String nombreCategoria);
}
