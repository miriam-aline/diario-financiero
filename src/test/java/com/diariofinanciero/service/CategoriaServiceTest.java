package com.diariofinanciero.service;

import com.diariofinanciero.domain.Categoria;
import com.diariofinanciero.repository.CategoriaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    @Test
    @DisplayName("Debe listar todas las categorías ordenadas")
    void listarTodas() {
        List<Categoria> esperadas = List.of(new Categoria("Ahorro"), new Categoria("Comida"));
        when(categoriaRepository.findAllByOrderByNombreAsc()).thenReturn(esperadas);

        assertEquals(esperadas, categoriaService.listarTodas());
    }

    @Test
    @DisplayName("Debe obtener categoría por id y validar nulos o inexistentes")
    void obtenerPorId() {
        Categoria comida = new Categoria("Comida");
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(comida));
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertEquals(comida, categoriaService.obtenerPorId(1L));
        assertThrows(IllegalArgumentException.class, () -> categoriaService.obtenerPorId(null));
        assertThrows(RecursoNoEncontradoException.class, () -> categoriaService.obtenerPorId(99L));
    }

    @Test
    @DisplayName("Debe listar recientes o usar respaldo alfabético cuando no hay movimientos recientes")
    void listarRecientes() {
        assertThrows(IllegalArgumentException.class, () -> categoriaService.listarRecientes(0));

        Categoria comida = new Categoria("Comida");
        when(categoriaRepository.findCategoriasRecientes(PageRequest.of(0, 2)))
                .thenReturn(List.of(comida));
        assertEquals(List.of(comida), categoriaService.listarRecientes(2));

        Categoria a = new Categoria("A");
        Categoria b = new Categoria("B");
        Categoria c = new Categoria("C");
        when(categoriaRepository.findCategoriasRecientes(PageRequest.of(0, 2)))
                .thenReturn(List.of());
        when(categoriaRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(a, b, c));
        assertEquals(List.of(a, b), categoriaService.listarRecientes(2));
    }

    @Test
    @DisplayName("Debe crear categoría nueva y rechazar nombres duplicados")
    void crear() {
        when(categoriaRepository.findByNombreIgnoreCase("Salud")).thenReturn(Optional.empty());
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        Categoria creada = categoriaService.crear("  Salud ", new BigDecimal("500"));
        assertEquals("Salud", creada.getNombre());
        assertEquals(new BigDecimal("500.00"), creada.getPresupuestoMensual());
        verify(categoriaRepository).save(any(Categoria.class));
    }

    @Test
    @DisplayName("Debe rechazar creación cuando el nombre ya existe")
    void crearDuplicada() {
        when(categoriaRepository.findByNombreIgnoreCase("Comida")).thenReturn(Optional.of(new Categoria("Comida")));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> categoriaService.crear("  Comida  ", null)
        );
        assertEquals("Ya existe una categoría llamada \"Comida\"", ex.getMessage());
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe actualizar el presupuesto de una categoría existente")
    void actualizarPresupuesto() {
        Categoria comida = new Categoria("Comida");
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(comida));
        when(categoriaRepository.save(comida)).thenReturn(comida);

        Categoria actualizada = categoriaService.actualizarPresupuesto(5L, new BigDecimal("2500.5"));
        assertEquals(new BigDecimal("2500.50"), actualizada.getPresupuestoMensual());
        verify(categoriaRepository).save(comida);
    }
}
