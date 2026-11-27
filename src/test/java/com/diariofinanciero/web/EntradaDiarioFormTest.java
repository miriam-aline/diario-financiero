package com.diariofinanciero.web;

import com.diariofinanciero.domain.EntradaDiario;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntradaDiarioFormTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("Debe mapear desde EntradaDiario y validar campos")
    void desdeEntidadYValidacion() {
        EntradaDiario entrada = new EntradaDiario(LocalDate.of(2026, 5, 10), "Buen día", 4, "ahorro,meta");
        EntradaDiarioForm form = EntradaDiarioForm.desdeEntidad(entrada);

        assertEquals(LocalDate.of(2026, 5, 10), form.getFecha());
        assertEquals("Buen día", form.getNota());
        assertEquals(4, form.getEstadoDeAnimo());
        assertEquals("ahorro, meta", form.getEtiquetas());
        assertTrue(validator.validate(form).isEmpty());

        EntradaDiario sinEtiquetas = new EntradaDiario(LocalDate.of(2026, 5, 11), "Otro", null, null);
        assertEquals("", EntradaDiarioForm.desdeEntidad(sinEtiquetas).getEtiquetas());

        form.setNota(" ");
        form.setEstadoDeAnimo(6);
        assertFalse(validator.validate(form).isEmpty());
    }
}
