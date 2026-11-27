package com.diariofinanciero.service;

import com.diariofinanciero.domain.Categoria;
import com.diariofinanciero.domain.ReglaCategoria;
import com.diariofinanciero.repository.ReglaCategoriaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReglaCategoriaServiceTest {

    @Mock
    private ReglaCategoriaRepository reglaCategoriaRepository;

    @Mock
    private CategoriaService categoriaService;

    @InjectMocks
    private ReglaCategoriaService reglaCategoriaService;

    @Test
    @DisplayName("Debe listar reglas y buscar la primera coincidencia")
    void listarYBuscarCoincidencia() {
        Categoria comida = new Categoria("Comida");
        ReglaCategoria r1 = new ReglaCategoria("uber eats", comida);
        when(reglaCategoriaRepository.findAllConCategoriaOrdenadas()).thenReturn(List.of(r1));

        List<ReglaCategoria> reglas = reglaCategoriaService.listarTodas();
        assertEquals(1, reglas.size());

        assertTrue(reglaCategoriaService.buscarCoincidencia("Compra UBER EATS", reglas).isPresent());
        assertTrue(reglaCategoriaService.buscarCoincidencia("Farmacia", reglas).isEmpty());
    }

    @Test
    @DisplayName("Debe guardar nueva regla o actualizar categoría si ya existe")
    void guardarRegla() {
        Categoria comida = new Categoria("Comida");
        Categoria ocio = new Categoria("Ocio");
        when(categoriaService.obtenerPorId(1L)).thenReturn(comida);
        when(categoriaService.obtenerPorId(2L)).thenReturn(ocio);

        assertThrows(IllegalArgumentException.class, () -> reglaCategoriaService.guardarRegla("   ", 1L));

        when(reglaCategoriaRepository.findByTextoContieneIgnoreCase("cafe")).thenReturn(Optional.empty());
        when(reglaCategoriaRepository.save(any(ReglaCategoria.class))).thenAnswer(inv -> inv.getArgument(0));

        ReglaCategoria nueva = reglaCategoriaService.guardarRegla(" Café ", 1L);
        assertEquals("cafe", nueva.getTextoContiene());
        assertEquals(comida, nueva.getCategoria());

        when(reglaCategoriaRepository.findByTextoContieneIgnoreCase("cafe")).thenReturn(Optional.of(nueva));
        ReglaCategoria actualizada = reglaCategoriaService.guardarRegla("CAFE", 2L);
        assertEquals(ocio, actualizada.getCategoria());
    }

    @Test
    @DisplayName("Debe eliminar regla existente y validar id nulo o inexistente")
    void eliminar() {
        ReglaCategoria regla = new ReglaCategoria("uber", new Categoria("Transporte"));
        when(reglaCategoriaRepository.findById(3L)).thenReturn(Optional.of(regla));
        when(reglaCategoriaRepository.findById(99L)).thenReturn(Optional.empty());

        reglaCategoriaService.eliminar(3L);
        verify(reglaCategoriaRepository).delete(regla);

        assertThrows(IllegalArgumentException.class, () -> reglaCategoriaService.eliminar(null));
        assertThrows(RecursoNoEncontradoException.class, () -> reglaCategoriaService.eliminar(99L));
    }
}
