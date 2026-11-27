package com.diariofinanciero.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatosGraficasMesTest {

    @Test
    @DisplayName("Debe exponer los componentes del record principal y sus records anidados")
    void recordsAnidadosYPrincipal() {
        var cat = new DatosGraficasMes.ItemCategoriaGrafica("Comida", new BigDecimal("400.00"), new BigDecimal("50.0"));
        var mes = new DatosGraficasMes.ItemMesGrafica("2026-05", "May 26", new BigDecimal("1000.00"), new BigDecimal("400.00"));
        var dia = new DatosGraficasMes.ItemAcumuladoDia(1, "2026-05-01", new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("800.00"));

        DatosGraficasMes datos = new DatosGraficasMes(
                "2026-05",
                "Mayo 2026",
                new BigDecimal("1000.00"),
                new BigDecimal("400.00"),
                new BigDecimal("800.00"),
                List.of(cat),
                List.of(mes),
                List.of(dia)
        );

        assertEquals("2026-05", datos.mes());
        assertEquals("Mayo 2026", datos.etiquetaMes());
        assertEquals(1, datos.gastoPorCategoria().size());
        assertEquals(1, datos.ingresosVsGastosMensual().size());
        assertEquals(1, datos.gastoAcumuladoDiario().size());
        assertEquals("Comida", cat.categoria());
        assertEquals("May 26", mes.etiqueta());
        assertEquals(1, dia.dia());
    }
}
