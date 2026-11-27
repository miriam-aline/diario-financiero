package com.diariofinanciero.web;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoriaFormTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("Debe validar nombre obligatorio/longitud y presupuesto positivo de máximo 2 decimales")
    void validacionYPropiedades() {
        CategoriaForm valido = new CategoriaForm();
        valido.setNombre("Comida");
        valido.setPresupuestoMensual(new BigDecimal("1500.50"));
        assertTrue(validator.validate(valido).isEmpty());
        assertEquals("Comida", valido.getNombre());
        assertEquals(new BigDecimal("1500.50"), valido.getPresupuestoMensual());

        CategoriaForm invalido = new CategoriaForm();
        invalido.setNombre("   ");
        invalido.setPresupuestoMensual(BigDecimal.ZERO);
        assertFalse(validator.validate(invalido).isEmpty());

        CategoriaForm muyLargo = new CategoriaForm();
        muyLargo.setNombre("a".repeat(101));
        muyLargo.setPresupuestoMensual(new BigDecimal("10.123"));
        assertEquals(2, validator.validate(muyLargo).size());
    }
}
