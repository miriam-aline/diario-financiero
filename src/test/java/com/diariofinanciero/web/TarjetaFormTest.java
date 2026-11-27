package com.diariofinanciero.web;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TarjetaFormTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("Debe validar nombre y rango 1..31 de día de corte y día de pago")
    void validacionYPropiedades() {
        TarjetaForm form = new TarjetaForm();
        form.setNombre("Tarjeta Oro");
        form.setDiaCorte(15);
        form.setDiaPago(5);

        assertTrue(validator.validate(form).isEmpty());
        assertEquals("Tarjeta Oro", form.getNombre());
        assertEquals(15, form.getDiaCorte());
        assertEquals(5, form.getDiaPago());

        form.setDiaCorte(0);
        form.setDiaPago(32);
        assertEquals(2, validator.validate(form).size());

        form.setNombre(" ");
        assertFalse(validator.validate(form).isEmpty());
    }
}
