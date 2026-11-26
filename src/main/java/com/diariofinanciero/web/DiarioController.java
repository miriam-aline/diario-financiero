package com.diariofinanciero.web;

import com.diariofinanciero.domain.EntradaDiario;
import com.diariofinanciero.service.DiaDiario;
import com.diariofinanciero.service.DiarioService;
import com.diariofinanciero.service.RecursoNoEncontradoException;
import com.diariofinanciero.service.ResumenSemanalDiario;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/diario")
public class DiarioController {

    private final DiarioService diarioService;

    public DiarioController(DiarioService diarioService) {
        this.diarioService = diarioService;
    }

    @GetMapping
    public String verDiario(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semana,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String etiqueta,
            Model model
    ) {
        LocalDate fechaEditor = fecha != null ? fecha : LocalDate.now();
        LocalDate referenciaSemana = semana != null ? semana : fechaEditor;

        Optional<EntradaDiario> entradaExistente = diarioService.obtenerPorFecha(fechaEditor);
        EntradaDiarioForm form = entradaExistente
                .map(EntradaDiarioForm::desdeEntidad)
                .orElseGet(() -> {
                    EntradaDiarioForm nuevo = new EntradaDiarioForm();
                    nuevo.setFecha(fechaEditor);
                    return nuevo;
                });

        cargarModelo(model, referenciaSemana, form, entradaExistente.isPresent(), texto, etiqueta);
        return "diario/index";
    }

    @PostMapping
    public String guardarEntrada(
            @Valid @ModelAttribute("entradaForm") EntradaDiarioForm form,
            BindingResult bindingResult,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semana,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        LocalDate referenciaSemana = semana != null
                ? semana
                : (form.getFecha() != null ? form.getFecha() : LocalDate.now());

        if (bindingResult.hasErrors()) {
            boolean editando = form.getFecha() != null && diarioService.obtenerPorFecha(form.getFecha()).isPresent();
            cargarModelo(model, referenciaSemana, form, editando, null, null);
            return "diario/index";
        }

        try {
            diarioService.guardarEntrada(
                    form.getFecha(),
                    form.getNota(),
                    form.getEstadoDeAnimo(),
                    form.getEtiquetas()
            );
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("dominio", ex.getMessage());
            boolean editando = form.getFecha() != null && diarioService.obtenerPorFecha(form.getFecha()).isPresent();
            cargarModelo(model, referenciaSemana, form, editando, null, null);
            return "diario/index";
        }

        redirectAttributes.addFlashAttribute("mensajeExito", "Entrada de diario guardada correctamente.");
        return "redirect:/diario?semana=" + form.getFecha();
    }

    @PostMapping("/{id}/eliminar")
    public String eliminarEntrada(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semana,
            RedirectAttributes redirectAttributes
    ) {
        diarioService.eliminar(id);
        redirectAttributes.addFlashAttribute("mensajeExito", "Entrada de diario eliminada.");
        if (semana != null) {
            return "redirect:/diario?semana=" + semana;
        }
        return "redirect:/diario";
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public String manejarNoEncontrado(
            RecursoNoEncontradoException ex,
            Model model,
            HttpServletResponse response
    ) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("paginaActiva", "diario");
        model.addAttribute("mensajeError", ex.getMessage());
        return "error/404";
    }

    private void cargarModelo(
            Model model,
            LocalDate referenciaSemana,
            EntradaDiarioForm form,
            boolean editandoExistente,
            String textoBusqueda,
            String etiquetaBusqueda
    ) {
        ResumenSemanalDiario resumen = diarioService.obtenerResumenSemanal(referenciaSemana);
        boolean modoBusqueda = (textoBusqueda != null && !textoBusqueda.isBlank())
                || (etiquetaBusqueda != null && !etiquetaBusqueda.isBlank());
        List<DiaDiario> resultadosBusqueda = modoBusqueda
                ? diarioService.buscarEntradas(textoBusqueda, etiquetaBusqueda)
                : List.of();

        model.addAttribute("paginaActiva", "diario");
        model.addAttribute("entradaForm", form);
        model.addAttribute("editandoExistente", editandoExistente);
        model.addAttribute("resumen", resumen);
        model.addAttribute("semanaReferencia", resumen.inicioSemana());
        model.addAttribute("semanaAnterior", resumen.inicioSemana().minusWeeks(1));
        model.addAttribute("semanaSiguiente", resumen.inicioSemana().plusWeeks(1));
        model.addAttribute("modoBusqueda", modoBusqueda);
        model.addAttribute("textoBusqueda", textoBusqueda == null ? "" : textoBusqueda.trim());
        model.addAttribute("etiquetaBusqueda", etiquetaBusqueda == null ? "" : etiquetaBusqueda.trim());
        model.addAttribute("resultadosBusqueda", resultadosBusqueda);
        model.addAttribute("etiquetasDisponibles", diarioService.listarEtiquetasUnicas());
    }
}
