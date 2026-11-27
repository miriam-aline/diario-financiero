package com.diariofinanciero.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntradaDiarioTest {

    @Nested
    @DisplayName("Construcción, actualización y validaciones")
    class ConstruccionYActualizacionTests {

        @Test
        @DisplayName("Debe crear entrada válida recortando nota y normalizando etiquetas")
        void crearEntradaValida() {
            LocalDate fecha = LocalDate.of(2026, 5, 10);
            EntradaDiario entrada = new EntradaDiario(fecha, "  Buen control hoy  ", 4, " #Ahorro, Super , ahorro ");

            assertNull(entrada.getId());
            assertEquals(fecha, entrada.getFecha());
            assertEquals("Buen control hoy", entrada.getNota());
            assertEquals(4, entrada.getEstadoDeAnimo());
            assertEquals("ahorro,super", entrada.getEtiquetas());
            assertEquals(List.of("ahorro", "super"), entrada.listaEtiquetas());
        }

        @Test
        @DisplayName("Debe actualizar contenido y permitir estadoDeAnimo y etiquetas nulos")
        void actualizarContenido() {
            EntradaDiario entrada = new EntradaDiario(LocalDate.of(2026, 5, 10), "Inicial", 3, "cafe");

            entrada.actualizar("  Nota actualizada  ", null, "   ");

            assertEquals("Nota actualizada", entrada.getNota());
            assertNull(entrada.getEstadoDeAnimo());
            assertNull(entrada.getEtiquetas());
            assertTrue(entrada.listaEtiquetas().isEmpty());
        }

        @Test
        @DisplayName("Debe validar fecha, nota, estadoDeAnimo y longitud máxima de etiquetas")
        void validarParametrosInvalidos() {
            LocalDate fecha = LocalDate.of(2026, 5, 10);

            assertEquals("La fecha de la entrada de diario es obligatoria",
                    assertThrows(IllegalArgumentException.class, () -> new EntradaDiario(null, "Nota", 3, null)).getMessage());

            assertEquals("La nota del diario es obligatoria",
                    assertThrows(IllegalArgumentException.class, () -> new EntradaDiario(fecha, null, 3, null)).getMessage());
            assertEquals("La nota del diario es obligatoria",
                    assertThrows(IllegalArgumentException.class, () -> new EntradaDiario(fecha, "   ", 3, null)).getMessage());

            String nota4001 = "a".repeat(4001);
            assertEquals("La nota no puede superar 4000 caracteres",
                    assertThrows(IllegalArgumentException.class, () -> new EntradaDiario(fecha, nota4001, 3, null)).getMessage());

            assertEquals("El estado de ánimo debe estar entre 1 y 5",
                    assertThrows(IllegalArgumentException.class, () -> new EntradaDiario(fecha, "Nota", 0, null)).getMessage());
            assertEquals("El estado de ánimo debe estar entre 1 y 5",
                    assertThrows(IllegalArgumentException.class, () -> new EntradaDiario(fecha, "Nota", 6, null)).getMessage());

            String etiquetasLargas = "a".repeat(501);
            assertEquals("Las etiquetas no pueden superar 500 caracteres",
                    assertThrows(IllegalArgumentException.class, () -> new EntradaDiario(fecha, "Nota", 3, etiquetasLargas)).getMessage());
        }
    }

    @Nested
    @DisplayName("normalizarEtiquetas y etiquetaAnimo")
    class EtiquetasYAnimoTests {

        @Test
        @DisplayName("Debe normalizar etiquetas quitando #, espacios, duplicados y vacíos")
        void normalizarEtiquetas() {
            assertNull(EntradaDiario.normalizarEtiquetas(null));
            assertNull(EntradaDiario.normalizarEtiquetas("   "));
            assertNull(EntradaDiario.normalizarEtiquetas(" , #, ###  , "));

            assertEquals(
                    "cafe,trabajo,finanzas",
                    EntradaDiario.normalizarEtiquetas(" #Cafe , ##Trabajo, cafe, , #Finanzas ")
            );
        }

        @Test
        @DisplayName("Debe retornar la descripción legible para cada estado de ánimo")
        void etiquetaAnimoPorValor() {
            LocalDate fecha = LocalDate.of(2026, 5, 10);
            assertEquals("", new EntradaDiario(fecha, "N", null, null).etiquetaAnimo());
            assertEquals("1/5 · Muy estresado", new EntradaDiario(fecha, "N", 1, null).etiquetaAnimo());
            assertEquals("2/5 · Inquieto", new EntradaDiario(fecha, "N", 2, null).etiquetaAnimo());
            assertEquals("3/5 · Neutral", new EntradaDiario(fecha, "N", 3, null).etiquetaAnimo());
            assertEquals("4/5 · Tranquilo", new EntradaDiario(fecha, "N", 4, null).etiquetaAnimo());
            assertEquals("5/5 · Muy satisfecho", new EntradaDiario(fecha, "N", 5, null).etiquetaAnimo());
        }
    }
}
