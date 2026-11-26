package com.diariofinanciero.web;

import com.diariofinanciero.service.ArchivoRespaldo;
import com.diariofinanciero.service.CategoriaService;
import com.diariofinanciero.service.ExportacionCsvService;
import com.diariofinanciero.service.FiltroMovimiento;
import com.diariofinanciero.service.RecursoNoEncontradoException;
import com.diariofinanciero.service.RespaldoService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Controller
public class RespaldoYExportacionController {

    private final ExportacionCsvService exportacionCsvService;
    private final RespaldoService respaldoService;
    private final CategoriaService categoriaService;

    public RespaldoYExportacionController(
            ExportacionCsvService exportacionCsvService,
            RespaldoService respaldoService,
            CategoriaService categoriaService
    ) {
        this.exportacionCsvService = exportacionCsvService;
        this.respaldoService = respaldoService;
        this.categoriaService = categoriaService;
    }

    @GetMapping("/respaldo")
    public String verRespaldo(Model model) {
        model.addAttribute("paginaActiva", "respaldo");
        model.addAttribute("respaldos", respaldoService.listarRespaldos());
        model.addAttribute("directorioRespaldos", respaldoService.getDirectorioRespaldos().toString());
        model.addAttribute("categorias", categoriaService.listarTodas());
        return "respaldo/index";
    }

    @PostMapping("/respaldo/crear")
    public String crearRespaldo(RedirectAttributes redirectAttributes) {
        try {
            ArchivoRespaldo creado = respaldoService.crearRespaldo();
            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "Respaldo creado correctamente: " + creado.nombreArchivo() + " (" + creado.tamanoLegible() + ")."
            );
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", "No se pudo crear el respaldo de la base de datos.");
        }
        return "redirect:/respaldo";
    }

    @GetMapping("/respaldo/descargar/{nombreArchivo}")
    public ResponseEntity<byte[]> descargarRespaldo(@PathVariable String nombreArchivo) {
        byte[] contenido = respaldoService.leerRespaldo(nombreArchivo);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment().filename(nombreArchivo).build());
        return new ResponseEntity<>(contenido, headers, HttpStatus.OK);
    }

    @GetMapping("/exportar/movimientos.csv")
    public ResponseEntity<byte[]> exportarMovimientosCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String texto
    ) {
        FiltroMovimiento filtro = new FiltroMovimiento(desde, hasta, categoriaId, texto);
        byte[] csv = exportacionCsvService.exportarMovimientos(filtro);

        String nombreArchivo = "movimientos-" + LocalDate.now() + ".csv";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename(nombreArchivo).build());
        return new ResponseEntity<>(csv, headers, HttpStatus.OK);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public String manejarNoEncontrado(
            RecursoNoEncontradoException ex,
            Model model,
            HttpServletResponse response
    ) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("paginaActiva", "respaldo");
        model.addAttribute("mensajeError", ex.getMessage());
        return "error/404";
    }
}
