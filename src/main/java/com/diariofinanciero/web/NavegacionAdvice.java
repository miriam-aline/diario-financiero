package com.diariofinanciero.web;

import com.diariofinanciero.service.ImportacionCsvService;
import com.diariofinanciero.service.TarjetaService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDate;

/**
 * Expone contadores globales ligeros para mostrar avisos discretos en el menú lateral.
 */
@ControllerAdvice
public class NavegacionAdvice {

    private final ImportacionCsvService importacionCsvService;
    private final TarjetaService tarjetaService;

    public NavegacionAdvice(
            ImportacionCsvService importacionCsvService,
            TarjetaService tarjetaService
    ) {
        this.importacionCsvService = importacionCsvService;
        this.tarjetaService = tarjetaService;
    }

    @ModelAttribute("navPendientesCsv")
    public int navPendientesCsv() {
        return importacionCsvService.listarPendientesRevision().size();
    }

    @ModelAttribute("navAlertasTarjetas")
    public int navAlertasTarjetas() {
        return tarjetaService.obtenerAlertasPagoProximo(
                LocalDate.now(), TarjetaService.DIAS_AVISO_PAGO_POR_DEFECTO).size();
    }
}
