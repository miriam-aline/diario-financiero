package com.diariofinanciero.web;

import com.diariofinanciero.service.DatosGraficasMes;
import com.diariofinanciero.service.GraficaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@Controller
public class GraficaController {

    private final GraficaService graficaService;

    public GraficaController(GraficaService graficaService) {
        this.graficaService = graficaService;
    }

    @GetMapping("/graficas")
    public String verGraficas(
            @RequestParam(required = false) String mes,
            @RequestParam(defaultValue = "6") int meses,
            Model model
    ) {
        YearMonth mesSeleccionado = parsearMes(mes);
        int ventana = graficaService.normalizarVentanaMeses(meses);
        DatosGraficasMes datos = graficaService.obtenerDatos(mesSeleccionado, ventana);

        model.addAttribute("paginaActiva", "graficas");
        model.addAttribute("datos", datos);
        model.addAttribute("mesSeleccionado", mesSeleccionado);
        model.addAttribute("mesAnterior", mesSeleccionado.minusMonths(1));
        model.addAttribute("mesSiguiente", mesSeleccionado.plusMonths(1));
        model.addAttribute("mesesVentana", ventana);
        return "graficas/index";
    }

    @GetMapping("/api/graficas")
    @ResponseBody
    public DatosGraficasMes obtenerDatosJson(
            @RequestParam(required = false) String mes,
            @RequestParam(defaultValue = "6") int meses
    ) {
        YearMonth mesSeleccionado = parsearMes(mes);
        return graficaService.obtenerDatos(mesSeleccionado, meses);
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
}
