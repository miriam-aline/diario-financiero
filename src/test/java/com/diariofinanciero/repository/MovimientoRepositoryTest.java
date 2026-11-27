package com.diariofinanciero.repository;

import com.diariofinanciero.domain.Categoria;
import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.Movimiento;
import com.diariofinanciero.domain.OrigenMovimiento;
import com.diariofinanciero.domain.TipoCuenta;
import com.diariofinanciero.domain.TipoMovimiento;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class MovimientoRepositoryTest {

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private EntityManager entityManager;

    private Categoria comida;
    private Categoria salario;
    private Cuenta efectivo;
    private Cuenta tarjeta;
    private Movimiento mov1;
    private Movimiento mov2;
    private Movimiento mov3;
    private Movimiento mov4;

    @BeforeEach
    void prepararDatos() {
        movimientoRepository.deleteAllInBatch();
        categoriaRepository.deleteAllInBatch();
        cuentaRepository.deleteAllInBatch();

        comida = categoriaRepository.save(new Categoria("Comida"));
        salario = categoriaRepository.save(new Categoria("Salario"));
        efectivo = cuentaRepository.save(new Cuenta("Efectivo", TipoCuenta.EFECTIVO));
        tarjeta = cuentaRepository.save(new Cuenta("Tarjeta", TipoCuenta.CREDITO, 15, 5));

        mov1 = movimientoRepository.save(Movimiento.manual(
                LocalDate.of(2026, 4, 15), new BigDecimal("100.00"), TipoMovimiento.GASTO, comida, efectivo, "Café mañana"
        ));
        mov2 = movimientoRepository.save(new Movimiento(
                LocalDate.of(2026, 5, 1), new BigDecimal("5000.00"), TipoMovimiento.INGRESO, salario, efectivo, "Quincena",
                OrigenMovimiento.IMPORTADO, "hash-quincena"
        ));
        mov3 = movimientoRepository.save(Movimiento.manual(
                LocalDate.of(2026, 5, 10), new BigDecimal("250.50"), TipoMovimiento.GASTO, comida, tarjeta, "Cena café"
        ));
        mov4 = movimientoRepository.save(new Movimiento(
                LocalDate.of(2026, 5, 10), new BigDecimal("149.50"), TipoMovimiento.GASTO, comida, efectivo, "Supermercado",
                OrigenMovimiento.IMPORTADO, "hash-super"
        ));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Debe obtener movimiento por id con categoría y cuenta cargadas")
    void findByIdConRelaciones() {
        Movimiento encontrado = movimientoRepository.findByIdConRelaciones(mov3.getId()).orElseThrow();

        assertEquals("Cena café", encontrado.getDescripcion());
        assertEquals("Comida", encontrado.getCategoria().getNombre());
        assertEquals("Tarjeta", encontrado.getCuenta().getNombre());
        assertTrue(movimientoRepository.findByIdConRelaciones(-1L).isEmpty());
    }

    @Test
    @DisplayName("Debe filtrar movimientos opcionalmente por fechas, categoría y texto")
    void buscarConFiltros() {
        List<Movimiento> todos = movimientoRepository.buscarConFiltros(null, null, null, null);
        assertEquals(4, todos.size());
        assertEquals(mov4.getId(), todos.get(0).getId());

        List<Movimiento> filtrados = movimientoRepository.buscarConFiltros(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                comida.getId(),
                "%café%"
        );
        assertEquals(1, filtrados.size());
        assertEquals(mov3.getId(), filtrados.get(0).getId());
    }

    @Test
    @DisplayName("Debe sumar montos por categoría, por mes/tipo y por día en el periodo")
    void proyeccionesDeAgregacion() {
        List<GastoPorCategoriaProjection> porCategoria = movimientoRepository.sumarPorCategoriaEnPeriodo(
                TipoMovimiento.GASTO,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)
        );
        assertEquals(1, porCategoria.size());
        assertEquals(comida.getId(), porCategoria.get(0).getCategoriaId());
        assertEquals(0, new BigDecimal("400.00").compareTo(porCategoria.get(0).getTotal()));

        List<ResumenMensualProjection> porMesYTipo = movimientoRepository.sumarPorMesYTipo(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 5, 31)
        );
        assertEquals(3, porMesYTipo.size());
        assertEquals(2026, porMesYTipo.get(0).getAnio());
        assertEquals(4, porMesYTipo.get(0).getMes());
        assertEquals(TipoMovimiento.GASTO, porMesYTipo.get(0).getTipo());
        assertEquals(0, new BigDecimal("100.00").compareTo(porMesYTipo.get(0).getTotal()));

        List<GastoDiarioProjection> porDia = movimientoRepository.sumarPorDiaEnPeriodo(
                TipoMovimiento.GASTO,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 5, 31)
        );
        assertEquals(2, porDia.size());
        assertEquals(LocalDate.of(2026, 4, 15), porDia.get(0).getFecha());
        assertEquals(0, new BigDecimal("100.00").compareTo(porDia.get(0).getTotal()));
        assertEquals(LocalDate.of(2026, 5, 10), porDia.get(1).getFecha());
        assertEquals(0, new BigDecimal("400.00").compareTo(porDia.get(1).getTotal()));
    }

    @Test
    @DisplayName("Debe buscar por cuenta en periodo, verificar hashes existentes y buscar por nombre de categoría")
    void consultasPorCuentaHashYCategoriaNombre() {
        List<Movimiento> deEfectivoMayo = movimientoRepository.buscarPorCuentaEnPeriodo(
                efectivo.getId(),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)
        );
        assertEquals(2, deEfectivoMayo.size());
        assertEquals(mov4.getId(), deEfectivoMayo.get(0).getId());
        assertEquals(mov2.getId(), deEfectivoMayo.get(1).getId());
        assertEquals("Efectivo", deEfectivoMayo.get(0).getCuenta().getNombre());

        Set<String> hashes = movimientoRepository.findHashesExistentes(
                List.of("hash-quincena", "hash-super", "hash-inexistente")
        );
        assertEquals(Set.of("hash-quincena", "hash-super"), hashes);

        List<Movimiento> porNombreCategoria = movimientoRepository.buscarPorCategoriaNombre("cOmIdA");
        assertEquals(3, porNombreCategoria.size());
        assertEquals(mov4.getId(), porNombreCategoria.get(0).getId());
        assertEquals(mov1.getId(), porNombreCategoria.get(2).getId());
    }
}
