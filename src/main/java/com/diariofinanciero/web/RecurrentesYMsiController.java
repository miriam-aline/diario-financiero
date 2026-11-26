package com.diariofinanciero.web;

import com.diariofinanciero.domain.CompraMSI;
import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.MensualidadMSI;
import com.diariofinanciero.domain.Periodicidad;
import com.diariofinanciero.domain.TipoCuenta;
import com.diariofinanciero.domain.TipoMovimiento;
import com.diariofinanciero.service.CategoriaService;
import com.diariofinanciero.service.CompraMSIService;
import com.diariofinanciero.service.CuentaService;
import com.diariofinanciero.service.RecursoNoEncontradoException;
import com.diariofinanciero.service.RecurrenteService;
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

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/recurrentes")
public class RecurrentesYMsiController {

    private final RecurrenteService recurrenteService;
    private final CompraMSIService compraMSIService;
    private final CategoriaService categoriaService;
    private final CuentaService cuentaService;

    public RecurrentesYMsiController(
            RecurrenteService recurrenteService,
            CompraMSIService compraMSIService,
            CategoriaService categoriaService,
            CuentaService cuentaService
    ) {
        this.recurrenteService = recurrenteService;
        this.compraMSIService = compraMSIService;
        this.categoriaService = categoriaService;
        this.cuentaService = cuentaService;
    }

    @GetMapping
    public String verPantalla(Model model) {
        List<Cuenta> cuentas = cuentaService.listarTodas();
        RecurrenteForm recurrenteForm = new RecurrenteForm();
        CompraMSIForm msiForm = new CompraMSIForm();

        if (!cuentas.isEmpty()) {
            recurrenteForm.setCuentaId(cuentas.getFirst().getId());
            Cuenta cuentaCreditoPreferida = cuentas.stream()
                    .filter(c -> c.getTipo() == TipoCuenta.CREDITO)
                    .findFirst()
                    .orElse(cuentas.getFirst());
            msiForm.setCuentaId(cuentaCreditoPreferida.getId());
        }

        cargarModelo(model, recurrenteForm, msiForm, cuentas);
        return "recurrentes/index";
    }

    @PostMapping
    public String crearRecurrente(
            @Valid @ModelAttribute("recurrenteForm") RecurrenteForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            cargarModelo(model, form, new CompraMSIForm(), cuentaService.listarTodas());
            return "recurrentes/index";
        }

        try {
            recurrenteService.crear(
                    form.getDescripcion(),
                    form.getMonto(),
                    form.getTipo(),
                    form.getCategoriaId(),
                    form.getCuentaId(),
                    form.getPeriodicidad(),
                    form.getProximoCobro()
            );
            redirectAttributes.addFlashAttribute("mensajeExito", "Movimiento recurrente programado correctamente.");
            return "redirect:/recurrentes";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("dominio", ex.getMessage());
            cargarModelo(model, form, new CompraMSIForm(), cuentaService.listarTodas());
            return "recurrentes/index";
        }
    }

    @PostMapping("/ejecutar-pendientes")
    public String ejecutarPendientes(RedirectAttributes redirectAttributes) {
        int generados = recurrenteService.generarMovimientosPendientes(LocalDate.now());
        String msg = generados > 0
                ? "Se generaron " + generados + " movimiento(s) recurrente(s) vencido(s) hasta hoy."
                : "No había cobros recurrentes pendientes hasta hoy.";
        redirectAttributes.addFlashAttribute("mensajeExito", msg);
        return "redirect:/recurrentes";
    }

    @PostMapping("/{id}/alternar")
    public String alternarRecurrente(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        recurrenteService.alternarActivo(id);
        redirectAttributes.addFlashAttribute("mensajeExito", "Estado del recurrente actualizado.");
        return "redirect:/recurrentes";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminarRecurrente(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        recurrenteService.eliminar(id);
        redirectAttributes.addFlashAttribute("mensajeExito", "Recurrente eliminado.");
        return "redirect:/recurrentes";
    }

    @PostMapping("/msi")
    public String crearCompraMsi(
            @Valid @ModelAttribute("msiForm") CompraMSIForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            cargarModelo(model, new RecurrenteForm(), form, cuentaService.listarTodas());
            return "recurrentes/index";
        }

        try {
            compraMSIService.crear(
                    form.getDescripcion(),
                    form.getMontoTotal(),
                    form.getNumeroMensualidades(),
                    form.getMensualidadesPagadas(),
                    form.getCuentaId(),
                    form.getCategoriaId(),
                    form.getFechaPrimerCargo()
            );
            redirectAttributes.addFlashAttribute("mensajeExito", "Compra a meses sin intereses registrada.");
            return "redirect:/recurrentes";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("dominio", ex.getMessage());
            cargarModelo(model, new RecurrenteForm(), form, cuentaService.listarTodas());
            return "recurrentes/index";
        }
    }

    @PostMapping("/msi/{id}/pagar-cuota")
    public String pagarSiguienteCuota(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean crearMovimiento,
            RedirectAttributes redirectAttributes
    ) {
        try {
            MensualidadMSI cuota = compraMSIService.registrarSiguienteMensualidad(id, crearMovimiento);
            String extra = crearMovimiento ? " y se registró el movimiento correspondiente." : ".";
            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "Mensualidad #" + cuota.numero() + " ($" + cuota.monto() + " MXN) marcada como pagada" + extra
            );
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/recurrentes";
    }

    @PostMapping("/msi/{id}/eliminar")
    public String eliminarCompraMsi(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        compraMSIService.eliminar(id);
        redirectAttributes.addFlashAttribute("mensajeExito", "Compra a MSI eliminada.");
        return "redirect:/recurrentes";
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public String manejarNoEncontrado(
            RecursoNoEncontradoException ex,
            Model model,
            HttpServletResponse response
    ) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("paginaActiva", "recurrentes");
        model.addAttribute("mensajeError", ex.getMessage());
        return "error/404";
    }

    private void cargarModelo(
            Model model,
            RecurrenteForm recurrenteForm,
            CompraMSIForm msiForm,
            List<Cuenta> cuentas
    ) {
        List<CompraMSI> comprasMsi = compraMSIService.listarTodas();

        model.addAttribute("paginaActiva", "recurrentes");
        model.addAttribute("recurrentes", recurrenteService.listarTodos());
        model.addAttribute("comprasMsi", comprasMsi);
        model.addAttribute("saldoPendienteMsi", compraMSIService.calcularSaldoPendienteTotal(comprasMsi));
        model.addAttribute("compromisoMensualMsi", compraMSIService.calcularCompromisoMensualActual(comprasMsi));
        model.addAttribute("recurrenteForm", recurrenteForm);
        model.addAttribute("msiForm", msiForm);
        model.addAttribute("categorias", categoriaService.listarTodas());
        model.addAttribute("cuentas", cuentas);
        model.addAttribute("tiposMovimiento", TipoMovimiento.values());
        model.addAttribute("periodicidades", Periodicidad.values());
    }
}
