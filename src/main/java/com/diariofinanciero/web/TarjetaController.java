package com.diariofinanciero.web;

import com.diariofinanciero.service.AlertaPagoTarjeta;
import com.diariofinanciero.service.CuentaService;
import com.diariofinanciero.service.RecursoNoEncontradoException;
import com.diariofinanciero.service.ResumenTarjeta;
import com.diariofinanciero.service.TarjetaService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/tarjetas")
public class TarjetaController {

    private final TarjetaService tarjetaService;
    private final CuentaService cuentaService;

    public TarjetaController(TarjetaService tarjetaService, CuentaService cuentaService) {
        this.tarjetaService = tarjetaService;
        this.cuentaService = cuentaService;
    }

    @GetMapping
    public String verTarjetas(Model model) {
        cargarModelo(model, new TarjetaForm());
        return "tarjetas/index";
    }

    @PostMapping
    public String crearTarjeta(
            @Valid @ModelAttribute("tarjetaForm") TarjetaForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            cargarModelo(model, form);
            return "tarjetas/index";
        }

        try {
            cuentaService.crearTarjetaCredito(form.getNombre(), form.getDiaCorte(), form.getDiaPago());
            redirectAttributes.addFlashAttribute("mensajeExito", "Tarjeta de crédito registrada correctamente.");
            return "redirect:/tarjetas";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("dominio", ex.getMessage());
            cargarModelo(model, form);
            return "tarjetas/index";
        }
    }

    @PostMapping("/{id}/editar")
    public String actualizarTarjeta(
            @PathVariable Long id,
            @RequestParam String nombre,
            @RequestParam Integer diaCorte,
            @RequestParam Integer diaPago,
            RedirectAttributes redirectAttributes
    ) {
        try {
            cuentaService.actualizarTarjetaCredito(id, nombre, diaCorte, diaPago);
            redirectAttributes.addFlashAttribute("mensajeExito", "Fechas de tarjeta actualizadas.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/tarjetas";
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public String manejarNoEncontrado(
            RecursoNoEncontradoException ex,
            Model model,
            HttpServletResponse response
    ) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("paginaActiva", "tarjetas");
        model.addAttribute("mensajeError", ex.getMessage());
        return "error/404";
    }

    private void cargarModelo(Model model, TarjetaForm form) {
        LocalDate hoy = LocalDate.now();
        int diasAviso = TarjetaService.DIAS_AVISO_PAGO_POR_DEFECTO;
        List<ResumenTarjeta> tarjetas = tarjetaService.obtenerResumenTarjetas(hoy, diasAviso);
        List<AlertaPagoTarjeta> alertas = tarjetaService.obtenerAlertasPagoProximo(hoy, diasAviso);
        BigDecimal totalProximoCorte = tarjetaService.calcularTotalEstimadoProximoCorte(tarjetas);

        model.addAttribute("paginaActiva", "tarjetas");
        model.addAttribute("tarjetas", tarjetas);
        model.addAttribute("alertasPago", alertas);
        model.addAttribute("totalProximoCorte", totalProximoCorte);
        model.addAttribute("diasAviso", diasAviso);
        model.addAttribute("tarjetaForm", form);
    }
}
