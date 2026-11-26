package com.diariofinanciero.web;

import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.Movimiento;
import com.diariofinanciero.domain.Periodo;
import com.diariofinanciero.domain.TipoMovimiento;
import com.diariofinanciero.domain.TipoPeriodo;
import com.diariofinanciero.service.BalancePeriodo;
import com.diariofinanciero.service.CategoriaService;
import com.diariofinanciero.service.CuentaService;
import com.diariofinanciero.service.FiltroMovimiento;
import com.diariofinanciero.service.MovimientoService;
import com.diariofinanciero.service.RecursoNoEncontradoException;
import com.diariofinanciero.service.TarjetaService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

@Controller
public class MovimientoController {

    private static final int LIMITE_CATEGORIAS_RECIENTES = 8;

    private final MovimientoService movimientoService;
    private final CategoriaService categoriaService;
    private final CuentaService cuentaService;
    private final TarjetaService tarjetaService;

    public MovimientoController(
            MovimientoService movimientoService,
            CategoriaService categoriaService,
            CuentaService cuentaService,
            TarjetaService tarjetaService
    ) {
        this.movimientoService = movimientoService;
        this.categoriaService = categoriaService;
        this.cuentaService = cuentaService;
        this.tarjetaService = tarjetaService;
    }

    @GetMapping("/movimientos")
    public String listar(
            @RequestParam(required = false) String mes,
            @RequestParam(required = false) TipoPeriodo tipoPeriodo,
            @RequestParam(defaultValue = "false") boolean todos,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String texto,
            Model model
    ) {
        YearMonth mesSeleccionado = parsearMes(mes);
        boolean usarFiltroManual = todos || desde != null || hasta != null;

        Periodo periodoActivo = null;
        FiltroMovimiento filtro;
        try {
            if (usarFiltroManual) {
                filtro = new FiltroMovimiento(desde, hasta, categoriaId, texto);
            } else {
                TipoPeriodo tipo = tipoPeriodo == null ? TipoPeriodo.MES : tipoPeriodo;
                periodoActivo = Periodo.de(mesSeleccionado, tipo);
                filtro = new FiltroMovimiento(periodoActivo.desde(), periodoActivo.hasta(), categoriaId, texto);
            }
        } catch (IllegalArgumentException ex) {
            filtro = FiltroMovimiento.vacio();
            model.addAttribute("errorFiltro", ex.getMessage());
        }

        List<Movimiento> movimientos = movimientoService.listar(filtro);
        BalancePeriodo balance = movimientoService.calcularBalance(movimientos);

        model.addAttribute("paginaActiva", "movimientos");
        model.addAttribute("movimientos", movimientos);
        model.addAttribute("balance", balance);
        model.addAttribute("filtro", filtro);
        model.addAttribute("periodoActivo", periodoActivo);
        model.addAttribute("periodoAnterior", periodoActivo == null ? null : periodoActivo.anterior());
        model.addAttribute("periodoSiguiente", periodoActivo == null ? null : periodoActivo.siguiente());
        model.addAttribute("mesSeleccionado", mesSeleccionado);
        model.addAttribute("usarFiltroManual", usarFiltroManual);
        model.addAttribute("tiposPeriodo", TipoPeriodo.values());
        model.addAttribute("categorias", categoriaService.listarTodas());
        model.addAttribute("alertasPagoTarjetas", tarjetaService.obtenerAlertasPagoProximo(
                LocalDate.now(), TarjetaService.DIAS_AVISO_PAGO_POR_DEFECTO));
        return "movimientos/lista";
    }

    @GetMapping("/movimientos/nuevo")
    public String mostrarFormularioNuevo(
            @RequestParam(required = false) TipoMovimiento tipo,
            Model model
    ) {
        MovimientoForm form = new MovimientoForm();
        if (tipo != null) {
            form.setTipo(tipo);
        }
        List<Cuenta> cuentas = cuentaService.listarTodas();
        if (!cuentas.isEmpty()) {
            form.setCuentaId(cuentas.getFirst().getId());
        }

        cargarModeloNuevo(model, form, cuentas);
        return "movimientos/nuevo";
    }

    @PostMapping("/movimientos")
    public String registrar(
            @Valid @ModelAttribute("movimientoForm") MovimientoForm form,
            BindingResult bindingResult,
            @RequestParam(defaultValue = "false") boolean agregarOtro,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            cargarModeloNuevo(model, form, cuentaService.listarTodas());
            return "movimientos/nuevo";
        }

        try {
            movimientoService.registrar(
                    form.getFecha(),
                    form.getMonto(),
                    form.getTipo(),
                    form.getCategoriaId(),
                    form.getCuentaId(),
                    form.getDescripcion()
            );
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("dominio", ex.getMessage());
            cargarModeloNuevo(model, form, cuentaService.listarTodas());
            return "movimientos/nuevo";
        }

        if (agregarOtro) {
            redirectAttributes.addFlashAttribute("mensajeExito", "Movimiento guardado. Puedes registrar otro ahora.");
            return "redirect:/movimientos/nuevo?tipo=" + form.getTipo().name();
        }

        redirectAttributes.addFlashAttribute("mensajeExito", "Movimiento registrado correctamente.");
        return "redirect:/movimientos";
    }

    @GetMapping("/movimientos/{id}/editar")
    public String mostrarEdicion(@PathVariable Long id, Model model) {
        Movimiento movimiento = movimientoService.obtenerPorId(id);
        model.addAttribute("paginaActiva", "movimientos");
        model.addAttribute("movimientoId", id);
        model.addAttribute("movimientoForm", MovimientoForm.desdeEntidad(movimiento));
        cargarCatalogos(model, cuentaService.listarTodas());
        return "movimientos/editar";
    }

    @PostMapping("/movimientos/{id}/editar")
    public String actualizar(
            @PathVariable Long id,
            @Valid @ModelAttribute("movimientoForm") MovimientoForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("paginaActiva", "movimientos");
            model.addAttribute("movimientoId", id);
            cargarCatalogos(model, cuentaService.listarTodas());
            return "movimientos/editar";
        }

        try {
            movimientoService.actualizar(
                    id,
                    form.getFecha(),
                    form.getMonto(),
                    form.getTipo(),
                    form.getCategoriaId(),
                    form.getCuentaId(),
                    form.getDescripcion()
            );
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("dominio", ex.getMessage());
            model.addAttribute("paginaActiva", "movimientos");
            model.addAttribute("movimientoId", id);
            cargarCatalogos(model, cuentaService.listarTodas());
            return "movimientos/editar";
        }

        redirectAttributes.addFlashAttribute("mensajeExito", "Movimiento actualizado correctamente.");
        return "redirect:/movimientos";
    }

    @PostMapping("/movimientos/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        movimientoService.eliminar(id);
        redirectAttributes.addFlashAttribute("mensajeExito", "Movimiento eliminado.");
        return "redirect:/movimientos";
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public String manejarNoEncontrado(
            RecursoNoEncontradoException ex,
            Model model,
            HttpServletResponse response
    ) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("paginaActiva", "movimientos");
        model.addAttribute("mensajeError", ex.getMessage());
        return "error/404";
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

    private void cargarModeloNuevo(Model model, MovimientoForm form, List<Cuenta> cuentas) {
        model.addAttribute("paginaActiva", "nuevo");
        model.addAttribute("movimientoForm", form);
        model.addAttribute("categoriasRecientes", categoriaService.listarRecientes(LIMITE_CATEGORIAS_RECIENTES));
        cargarCatalogos(model, cuentas);
    }

    private void cargarCatalogos(Model model, List<Cuenta> cuentas) {
        model.addAttribute("categorias", categoriaService.listarTodas());
        model.addAttribute("cuentas", cuentas);
        model.addAttribute("tiposMovimiento", TipoMovimiento.values());
    }
}
