package com.diariofinanciero.web;

import com.diariofinanciero.domain.CompraMSI;
import com.diariofinanciero.domain.EntradaDiario;
import com.diariofinanciero.domain.EstadoPresupuesto;
import com.diariofinanciero.domain.Movimiento;
import com.diariofinanciero.domain.Periodo;
import com.diariofinanciero.domain.TipoPeriodo;
import com.diariofinanciero.service.AlertaPagoTarjeta;
import com.diariofinanciero.service.BalancePeriodo;
import com.diariofinanciero.service.CompraMSIService;
import com.diariofinanciero.service.DiarioService;
import com.diariofinanciero.service.FiltroMovimiento;
import com.diariofinanciero.service.ImportacionCsvService;
import com.diariofinanciero.service.MovimientoService;
import com.diariofinanciero.service.PresupuestoService;
import com.diariofinanciero.service.ResumenCategoriaPresupuesto;
import com.diariofinanciero.service.ResumenPresupuestosMes;
import com.diariofinanciero.service.ResumenTarjeta;
import com.diariofinanciero.service.TarjetaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Controller
public class InicioController {

    private static final int LIMITE_ULTIMOS_MOVIMIENTOS = 6;
    private static final int LIMITE_PRESUPUESTOS_DESTACADOS = 4;

    private final MovimientoService movimientoService;
    private final PresupuestoService presupuestoService;
    private final DiarioService diarioService;
    private final TarjetaService tarjetaService;
    private final CompraMSIService compraMSIService;
    private final ImportacionCsvService importacionCsvService;

    public InicioController(
            MovimientoService movimientoService,
            PresupuestoService presupuestoService,
            DiarioService diarioService,
            TarjetaService tarjetaService,
            CompraMSIService compraMSIService,
            ImportacionCsvService importacionCsvService
    ) {
        this.movimientoService = movimientoService;
        this.presupuestoService = presupuestoService;
        this.diarioService = diarioService;
        this.tarjetaService = tarjetaService;
        this.compraMSIService = compraMSIService;
        this.importacionCsvService = importacionCsvService;
    }

    @GetMapping("/")
    public String inicio(Model model) {
        LocalDate hoy = LocalDate.now();
        YearMonth mesActual = YearMonth.from(hoy);
        Periodo periodoMes = Periodo.de(mesActual, TipoPeriodo.MES);

        List<Movimiento> movimientosMes = movimientoService.listar(
                new FiltroMovimiento(periodoMes.desde(), periodoMes.hasta(), null, null));
        BalancePeriodo balanceMes = movimientoService.calcularBalance(movimientosMes);
        List<Movimiento> ultimosMovimientos = movimientosMes.subList(
                0, Math.min(LIMITE_ULTIMOS_MOVIMIENTOS, movimientosMes.size()));

        ResumenPresupuestosMes presupuestos = presupuestoService.obtenerResumenMensual(mesActual);
        List<ResumenCategoriaPresupuesto> presupuestosDestacados = presupuestos.categorias().stream()
                .filter(c -> c.estado() != EstadoPresupuesto.SIN_PRESUPUESTO)
                .limit(LIMITE_PRESUPUESTOS_DESTACADOS)
                .toList();

        Optional<EntradaDiario> notaHoy = diarioService.obtenerPorFecha(hoy);
        List<ResumenTarjeta> tarjetas = tarjetaService.obtenerResumenTarjetas(
                hoy, TarjetaService.DIAS_AVISO_PAGO_POR_DEFECTO);
        List<AlertaPagoTarjeta> alertasPago = tarjetaService.obtenerAlertasPagoProximo(
                hoy, TarjetaService.DIAS_AVISO_PAGO_POR_DEFECTO);
        BigDecimal totalProximoCorte = tarjetaService.calcularTotalEstimadoProximoCorte(tarjetas);

        List<CompraMSI> comprasMsi = compraMSIService.listarTodas();
        BigDecimal compromisoMsi = compraMSIService.calcularCompromisoMensualActual(comprasMsi);
        int pendientesSinCategoria = importacionCsvService.listarPendientesRevision().size();

        model.addAttribute("paginaActiva", "inicio");
        model.addAttribute("etiquetaMes", periodoMes.etiqueta());
        model.addAttribute("balanceMes", balanceMes);
        model.addAttribute("ultimosMovimientos", ultimosMovimientos);
        model.addAttribute("totalMovimientosMes", movimientosMes.size());
        model.addAttribute("presupuestosResumen", presupuestos);
        model.addAttribute("presupuestosDestacados", presupuestosDestacados);
        model.addAttribute("notaHoy", notaHoy.orElse(null));
        model.addAttribute("alertasPago", alertasPago);
        model.addAttribute("totalTarjetas", tarjetas.size());
        model.addAttribute("totalProximoCorte", totalProximoCorte);
        model.addAttribute("compromisoMsi", compromisoMsi);
        model.addAttribute("pendientesSinCategoria", pendientesSinCategoria);
        return "inicio/index";
    }
}
