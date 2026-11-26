package com.diariofinanciero.web;

import com.diariofinanciero.domain.Categoria;
import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.Movimiento;
import com.diariofinanciero.service.CategoriaService;
import com.diariofinanciero.service.CuentaService;
import com.diariofinanciero.service.ImportacionCsvService;
import com.diariofinanciero.service.RecursoNoEncontradoException;
import com.diariofinanciero.service.ReglaCategoriaService;
import com.diariofinanciero.service.ResultadoConfirmacionImportacion;
import com.diariofinanciero.service.VistaPreviaImportacion;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/importacion")
public class ImportacionController {

    private final ImportacionCsvService importacionCsvService;
    private final ReglaCategoriaService reglaCategoriaService;
    private final CuentaService cuentaService;
    private final CategoriaService categoriaService;

    public ImportacionController(
            ImportacionCsvService importacionCsvService,
            ReglaCategoriaService reglaCategoriaService,
            CuentaService cuentaService,
            CategoriaService categoriaService
    ) {
        this.importacionCsvService = importacionCsvService;
        this.reglaCategoriaService = reglaCategoriaService;
        this.cuentaService = cuentaService;
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public String mostrarFormulario(Model model) {
        ImportacionForm form = new ImportacionForm();
        List<Cuenta> cuentas = cuentaService.listarTodas();
        if (!cuentas.isEmpty()) {
            form.setCuentaId(cuentas.getFirst().getId());
        }
        cargarModeloInicio(model, form, cuentas);
        return "importacion/index";
    }

    @PostMapping("/vista-previa")
    public String generarVistaPrevia(
            @RequestParam("archivo") MultipartFile archivo,
            @ModelAttribute("importacionForm") ImportacionForm form,
            Model model
    ) {
        try {
            validarArchivoSubido(archivo);
            byte[] bytes = archivo.getBytes();
            VistaPreviaImportacion vista = importacionCsvService.generarVistaPrevia(bytes, form.aConfiguracion());
            model.addAttribute("paginaActiva", "importacion");
            model.addAttribute("vista", vista);
            return "importacion/vista-previa";
        } catch (IllegalArgumentException | IOException ex) {
            model.addAttribute("mensajeError", ex.getMessage());
            cargarModeloInicio(model, form, cuentaService.listarTodas());
            return "importacion/index";
        }
    }

    @PostMapping("/confirmar")
    public String confirmar(
            @RequestParam String token,
            RedirectAttributes redirectAttributes
    ) {
        try {
            ResultadoConfirmacionImportacion res = importacionCsvService.confirmarImportacion(token);
            String mensaje = "Importación completada: " + res.importados() + " movimiento(s) nuevo(s), "
                    + res.omitidosDuplicados() + " duplicado(s) omitido(s).";
            redirectAttributes.addFlashAttribute("mensajeExito", mensaje);

            if (res.pendientesSinCategoria() > 0) {
                return "redirect:/importacion/revision";
            }
            return "redirect:/movimientos";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
            return "redirect:/importacion";
        }
    }

    @GetMapping("/revision")
    public String verRevision(Model model) {
        List<Movimiento> pendientes = importacionCsvService.listarPendientesRevision();
        List<Categoria> categoriasAsignables = categoriaService.listarTodas().stream()
                .filter(c -> !c.getNombre().equalsIgnoreCase(ImportacionCsvService.NOMBRE_SIN_CATEGORIA))
                .toList();

        model.addAttribute("paginaActiva", "importacion");
        model.addAttribute("pendientes", pendientes);
        model.addAttribute("categorias", categoriasAsignables);
        return "importacion/revision";
    }

    @PostMapping("/revision/{id}")
    public String clasificarPendiente(
            @PathVariable Long id,
            @RequestParam Long categoriaId,
            @RequestParam(defaultValue = "false") boolean guardarRegla,
            @RequestParam(required = false) String textoRegla,
            RedirectAttributes redirectAttributes
    ) {
        try {
            int actualizados = importacionCsvService.clasificarPendiente(id, categoriaId, guardarRegla, textoRegla);
            String msg = actualizados > 1
                    ? "Categoría asignada y regla aplicada a " + actualizados + " movimientos pendientes."
                    : "Categoría asignada correctamente.";
            redirectAttributes.addFlashAttribute("mensajeExito", msg);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/importacion/revision";
    }

    @PostMapping("/reglas")
    public String guardarRegla(
            @RequestParam String textoContiene,
            @RequestParam Long categoriaId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            reglaCategoriaService.guardarRegla(textoContiene, categoriaId);
            redirectAttributes.addFlashAttribute("mensajeExito", "Regla de categorización guardada.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/importacion";
    }

    @PostMapping("/reglas/{id}/eliminar")
    public String eliminarRegla(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        reglaCategoriaService.eliminar(id);
        redirectAttributes.addFlashAttribute("mensajeExito", "Regla eliminada.");
        return "redirect:/importacion";
    }

    @GetMapping("/ejemplo.csv")
    public ResponseEntity<byte[]> descargarEjemplo() {
        byte[] contenido = importacionCsvService.generarCsvEjemplo().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename("ejemplo-banco.csv").build());
        return new ResponseEntity<>(contenido, headers, HttpStatus.OK);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public String manejarNoEncontrado(
            RecursoNoEncontradoException ex,
            Model model,
            HttpServletResponse response
    ) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("paginaActiva", "importacion");
        model.addAttribute("mensajeError", ex.getMessage());
        return "error/404";
    }

    private void validarArchivoSubido(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Selecciona un archivo CSV para importar");
        }
        if (archivo.getSize() > ImportacionCsvService.MAX_TAMANO_BYTES) {
            throw new IllegalArgumentException("El archivo supera el tamaño máximo permitido (5 MB)");
        }
        String nombreOriginal = archivo.getOriginalFilename();
        if (nombreOriginal == null || !nombreOriginal.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new IllegalArgumentException("Solo se permiten archivos con extensión .csv");
        }
    }

    private void cargarModeloInicio(Model model, ImportacionForm form, List<Cuenta> cuentas) {
        List<Categoria> categoriasAsignables = categoriaService.listarTodas().stream()
                .filter(c -> !c.getNombre().equalsIgnoreCase(ImportacionCsvService.NOMBRE_SIN_CATEGORIA))
                .toList();

        model.addAttribute("paginaActiva", "importacion");
        model.addAttribute("importacionForm", form);
        model.addAttribute("cuentas", cuentas);
        model.addAttribute("categorias", categoriasAsignables);
        model.addAttribute("reglas", reglaCategoriaService.listarTodas());
        model.addAttribute("pendientesCount", importacionCsvService.listarPendientesRevision().size());
    }
}
