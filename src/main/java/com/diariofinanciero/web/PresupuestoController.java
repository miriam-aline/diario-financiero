package com.diariofinanciero.web;

import com.diariofinanciero.service.CategoriaService;
import com.diariofinanciero.service.PresupuestoService;
import com.diariofinanciero.service.RecursoNoEncontradoException;
import com.diariofinanciero.service.ResumenPresupuestosMes;
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
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@Controller
@RequestMapping("/presupuestos")
public class PresupuestoController {

    private final PresupuestoService presupuestoService;
    private final CategoriaService categoriaService;

    public PresupuestoController(
            PresupuestoService presupuestoService,
            CategoriaService categoriaService
    ) {
        this.presupuestoService = presupuestoService;
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public String verPresupuestos(@RequestParam(required = false) String mes, Model model) {
        YearMonth mesSeleccionado = parsearMes(mes);
        cargarModelo(model, mesSeleccionado, new CategoriaForm());
        return "presupuestos/index";
    }

    @PostMapping("/categorias/{id}")
    public String actualizarPresupuesto(
            @PathVariable Long id,
            @RequestParam(required = false) String presupuestoMensual,
            @RequestParam(required = false) String mes,
            RedirectAttributes redirectAttributes
    ) {
        YearMonth mesRedireccion = parsearMes(mes);
        try {
            BigDecimal monto = parsearPresupuestoOpcional(presupuestoMensual);
            categoriaService.actualizarPresupuesto(id, monto);
            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    monto == null ? "Presupuesto eliminado para la categoría." : "Presupuesto actualizado correctamente."
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/presupuestos?mes=" + mesRedireccion;
    }

    @PostMapping("/categorias")
    public String crearCategoria(
            @Valid @ModelAttribute("categoriaForm") CategoriaForm form,
            BindingResult bindingResult,
            @RequestParam(required = false) String mes,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        YearMonth mesSeleccionado = parsearMes(mes);
        if (bindingResult.hasErrors()) {
            cargarModelo(model, mesSeleccionado, form);
            return "presupuestos/index";
        }

        try {
            categoriaService.crear(form.getNombre(), form.getPresupuestoMensual());
            redirectAttributes.addFlashAttribute("mensajeExito", "Categoría creada correctamente.");
            return "redirect:/presupuestos?mes=" + mesSeleccionado;
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("dominio", ex.getMessage());
            cargarModelo(model, mesSeleccionado, form);
            return "presupuestos/index";
        }
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public String manejarNoEncontrado(
            RecursoNoEncontradoException ex,
            Model model,
            HttpServletResponse response
    ) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("paginaActiva", "presupuestos");
        model.addAttribute("mensajeError", ex.getMessage());
        return "error/404";
    }

    private void cargarModelo(Model model, YearMonth mesSeleccionado, CategoriaForm categoriaForm) {
        ResumenPresupuestosMes resumen = presupuestoService.obtenerResumenMensual(mesSeleccionado);
        model.addAttribute("paginaActiva", "presupuestos");
        model.addAttribute("resumen", resumen);
        model.addAttribute("mesSeleccionado", mesSeleccionado);
        model.addAttribute("mesAnterior", mesSeleccionado.minusMonths(1));
        model.addAttribute("mesSiguiente", mesSeleccionado.plusMonths(1));
        model.addAttribute("categoriaForm", categoriaForm);
    }

    private YearMonth parsearMes(String mes) {
        if (mes == null || mes.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(mes.trim());
        } catch (DateTimeParseException ex) {
            return YearMonth.now();
        }
    }

    private BigDecimal parsearPresupuestoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(valor.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Ingresa un monto numérico válido (ejemplo: 3500.00)");
        }
    }
}
