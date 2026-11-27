package com.diariofinanciero.web;

import com.diariofinanciero.service.ConfiguracionCsv;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ImportacionFormTest {

    @Test
    @DisplayName("Debe convertir separadores simbólicos a caracteres en aConfiguracion")
    void conversionAConfiguracion() {
        ImportacionForm form = new ImportacionForm();
        form.setCuentaId(1L);
        form.setColumnaFecha("Date");
        form.setColumnaDescripcion("Memo");
        form.setColumnaCargo("Debit");
        form.setColumnaAbono("Credit");
        form.setFormatoFecha("yyyy-MM-dd");
        form.setPrimeraFilaEsEncabezado(false);

        form.setSeparador(";");
        ConfiguracionCsv puntoYComa = form.aConfiguracion();
        assertEquals(';', puntoYComa.separador());
        assertFalse(puntoYComa.primeraFilaEsEncabezado());

        form.setSeparador("\\t");
        assertEquals('\t', form.aConfiguracion().separador());

        form.setSeparador("TAB");
        assertEquals('\t', form.aConfiguracion().separador());

        form.setSeparador("|");
        assertEquals('|', form.aConfiguracion().separador());

        form.setSeparador(null);
        assertEquals(',', form.aConfiguracion().separador());
    }
}
