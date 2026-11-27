package com.diariofinanciero.web;

import com.diariofinanciero.domain.Periodicidad;
import com.diariofinanciero.domain.TipoMovimiento;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecurrenteFormTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("Debe tener valores por defecto y validar campos obligatorios")
    void valoresYValidacion() {
        RecurrenteForm form = new RecurrenteForm();
        assertEquals(TipoMovimiento.GASTO, form.getTipo());
        assertEquals(Periodicidad.MENSUAL, form.getPeriodicidad());

        form.setDescripcion("Internet");
        form.setMonto(new BigDecimal("599.00"));
        form.setCategoriaId(1L);
        form.setCuentaId(2L);
        form.setProximoCobro(LocalDate.of(2026, 6, 1));
        assertTrue(validator.validate(form).isEmpty());

        form.setDescripcion("");
        form.setMonto(new BigDecimal("-5.00"));
        assertFalse(validator.validate(form).isEmpty());
    }
}
