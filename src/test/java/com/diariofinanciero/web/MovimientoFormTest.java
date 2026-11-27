package com.diariofinanciero.web;

import com.diariofinanciero.domain.Categoria;
import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.Movimiento;
import com.diariofinanciero.domain.TipoCuenta;
import com.diariofinanciero.domain.TipoMovimiento;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovimientoFormTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("Debe mapear desde Movimiento y validar restricciones")
    void desdeEntidadYValidacion() {
        Categoria cat = new Categoria("Comida");
        ReflectionTestUtils.setField(cat, "id", 10L);
        Cuenta cta = new Cuenta("Efectivo", TipoCuenta.EFECTIVO);
        ReflectionTestUtils.setField(cta, "id", 20L);
        Movimiento mov = Movimiento.manual(
                LocalDate.of(2026, 5, 10),
                new BigDecimal("150.50"),
                TipoMovimiento.GASTO,
                cat,
                cta,
                "Tacos"
        );

        MovimientoForm form = MovimientoForm.desdeEntidad(mov);
        assertEquals(LocalDate.of(2026, 5, 10), form.getFecha());
        assertEquals(new BigDecimal("150.50"), form.getMonto());
        assertEquals(TipoMovimiento.GASTO, form.getTipo());
        assertEquals(10L, form.getCategoriaId());
        assertEquals(20L, form.getCuentaId());
        assertEquals("Tacos", form.getDescripcion());
        assertTrue(validator.validate(form).isEmpty());

        form.setMonto(BigDecimal.ZERO);
        form.setDescripcion(" ");
        assertFalse(validator.validate(form).isEmpty());
    }
}
