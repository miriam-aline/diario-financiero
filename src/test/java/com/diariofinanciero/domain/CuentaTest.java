package com.diariofinanciero.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CuentaTest {

    @Nested
    @DisplayName("Construcción y validaciones")
    class ConstruccionTests {

        @Test
        @DisplayName("Debe crear cuenta de efectivo o débito sin días de corte ni pago")
        void crearEfectivoODebito() {
            Cuenta efectivo = new Cuenta("  Efectivo  ", TipoCuenta.EFECTIVO);
            assertNull(efectivo.getId());
            assertEquals("Efectivo", efectivo.getNombre());
            assertEquals(TipoCuenta.EFECTIVO, efectivo.getTipo());
            assertNull(efectivo.getDiaCorte());
            assertNull(efectivo.getDiaPago());

            Cuenta debito = new Cuenta("BBVA Débito", TipoCuenta.DEBITO);
            assertEquals("BBVA Débito", debito.getNombre());
            assertEquals(TipoCuenta.DEBITO, debito.getTipo());
        }

        @Test
        @DisplayName("Debe crear cuenta de crédito con o sin días de corte y pago válidos")
        void crearCredito() {
            Cuenta tarjeta = new Cuenta("Nu Crédito", TipoCuenta.CREDITO, 1, 31);
            assertEquals("Nu Crédito", tarjeta.getNombre());
            assertEquals(TipoCuenta.CREDITO, tarjeta.getTipo());
            assertEquals(1, tarjeta.getDiaCorte());
            assertEquals(31, tarjeta.getDiaPago());

            Cuenta tarjetaSinDias = new Cuenta("Tarjeta Sin Fechas", TipoCuenta.CREDITO, null, null);
            assertNull(tarjetaSinDias.getDiaCorte());
            assertNull(tarjetaSinDias.getDiaPago());
        }

        @Test
        @DisplayName("Debe validar nombre obligatorio y longitud máxima de 100 caracteres")
        void validarNombre() {
            assertEquals("El nombre de la cuenta es obligatorio",
                    assertThrows(IllegalArgumentException.class, () -> new Cuenta(null, TipoCuenta.EFECTIVO)).getMessage());
            assertEquals("El nombre de la cuenta es obligatorio",
                    assertThrows(IllegalArgumentException.class, () -> new Cuenta("   ", TipoCuenta.EFECTIVO)).getMessage());

            String nombre100 = "a".repeat(100);
            assertEquals(nombre100, new Cuenta(" " + nombre100 + " ", TipoCuenta.EFECTIVO).getNombre());

            assertEquals("El nombre de la cuenta no puede superar 100 caracteres",
                    assertThrows(IllegalArgumentException.class, () -> new Cuenta("a".repeat(101), TipoCuenta.EFECTIVO)).getMessage());
        }

        @Test
        @DisplayName("Debe validar tipo obligatorio y prohibir días de corte o pago en cuentas no crédito")
        void validarTipoYDiasEnNoCredito() {
            assertEquals("El tipo de cuenta es obligatorio",
                    assertThrows(IllegalArgumentException.class, () -> new Cuenta("Cuenta", null)).getMessage());

            assertEquals("Solo las cuentas de crédito pueden tener día de corte o de pago",
                    assertThrows(IllegalArgumentException.class, () -> new Cuenta("Efectivo", TipoCuenta.EFECTIVO, 10, null)).getMessage());

            assertEquals("Solo las cuentas de crédito pueden tener día de corte o de pago",
                    assertThrows(IllegalArgumentException.class, () -> new Cuenta("Débito", TipoCuenta.DEBITO, null, 20)).getMessage());
        }

        @Test
        @DisplayName("Debe validar rango 1..31 para día de corte y día de pago")
        void validarRangoDias() {
            assertEquals("El día de corte debe estar entre 1 y 31",
                    assertThrows(IllegalArgumentException.class, () -> new Cuenta("TC", TipoCuenta.CREDITO, 0, 10)).getMessage());
            assertEquals("El día de corte debe estar entre 1 y 31",
                    assertThrows(IllegalArgumentException.class, () -> new Cuenta("TC", TipoCuenta.CREDITO, 32, 10)).getMessage());
            assertEquals("El día de pago debe estar entre 1 y 31",
                    assertThrows(IllegalArgumentException.class, () -> new Cuenta("TC", TipoCuenta.CREDITO, 10, 0)).getMessage());
            assertEquals("El día de pago debe estar entre 1 y 31",
                    assertThrows(IllegalArgumentException.class, () -> new Cuenta("TC", TipoCuenta.CREDITO, 10, 32)).getMessage());
        }
    }

    @Nested
    @DisplayName("actualizarTarjetaCredito")
    class ActualizarTarjetaCreditoTests {

        @Test
        @DisplayName("Debe actualizar nombre, día de corte y día de pago en cuenta de crédito")
        void actualizarCreditoValido() {
            Cuenta tarjeta = new Cuenta("Tarjeta Vieja", TipoCuenta.CREDITO, 10, 25);

            tarjeta.actualizarTarjetaCredito("  Tarjeta Nueva  ", 15, 5);

            assertEquals("Tarjeta Nueva", tarjeta.getNombre());
            assertEquals(15, tarjeta.getDiaCorte());
            assertEquals(5, tarjeta.getDiaPago());
        }

        @Test
        @DisplayName("Debe rechazar actualización si no es crédito o faltan/son inválidos los parámetros")
        void actualizarCreditoInvalido() {
            Cuenta debito = new Cuenta("Débito", TipoCuenta.DEBITO);
            assertEquals("Solo las cuentas de crédito pueden tener día de corte y pago",
                    assertThrows(IllegalArgumentException.class, () -> debito.actualizarTarjetaCredito("X", 10, 20)).getMessage());

            Cuenta tarjeta = new Cuenta("TC", TipoCuenta.CREDITO, 10, 20);
            assertEquals("El día de corte y el día de pago son obligatorios para una tarjeta de crédito",
                    assertThrows(IllegalArgumentException.class, () -> tarjeta.actualizarTarjetaCredito("TC", null, 20)).getMessage());
            assertEquals("El día de corte y el día de pago son obligatorios para una tarjeta de crédito",
                    assertThrows(IllegalArgumentException.class, () -> tarjeta.actualizarTarjetaCredito("TC", 10, null)).getMessage());
            assertThrows(IllegalArgumentException.class, () -> tarjeta.actualizarTarjetaCredito(" ", 10, 20));
            assertThrows(IllegalArgumentException.class, () -> tarjeta.actualizarTarjetaCredito("TC", 0, 20));
            assertThrows(IllegalArgumentException.class, () -> tarjeta.actualizarTarjetaCredito("TC", 10, 32));
        }
    }
}
