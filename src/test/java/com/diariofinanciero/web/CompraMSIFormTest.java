package com.diariofinanciero.web;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompraMSIFormTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("Debe tener valores iniciales esperados y validar restricciones")
    void validacionYValoresPorDefecto() {
        CompraMSIForm form = new CompraMSIForm();
        assertEquals(6, form.getNumeroMensualidades());
        assertEquals(0, form.getMensualidadesPagadas());

        form.setDescripcion("Laptop");
        form.setMontoTotal(new BigDecimal("12000.00"));
        form.setCuentaId(1L);
        form.setCategoriaId(2L);
        form.setFechaPrimerCargo(LocalDate.of(2026, 5, 1));
        assertTrue(validator.validate(form).isEmpty());

        form.setDescripcion("");
        form.setNumeroMensualidades(1);
        form.setMensualidadesPagadas(-1);
        assertFalse(validator.validate(form).isEmpty());
    }
}
