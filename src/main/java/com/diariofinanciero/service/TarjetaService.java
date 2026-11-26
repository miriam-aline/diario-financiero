package com.diariofinanciero.service;

import com.diariofinanciero.domain.CicloFacturacion;
import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.Dinero;
import com.diariofinanciero.domain.Movimiento;
import com.diariofinanciero.domain.TipoCuenta;
import com.diariofinanciero.domain.TipoMovimiento;
import com.diariofinanciero.repository.MovimientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TarjetaService {

    public static final int DIAS_AVISO_PAGO_POR_DEFECTO = 5;
    private static final int CICLOS_HISTORIAL = 4;

    private final CuentaService cuentaService;
    private final MovimientoRepository movimientoRepository;

    public TarjetaService(
            CuentaService cuentaService,
            MovimientoRepository movimientoRepository
    ) {
        this.cuentaService = cuentaService;
        this.movimientoRepository = movimientoRepository;
    }

    public List<ResumenTarjeta> obtenerResumenTarjetas(LocalDate fechaReferencia, int diasAviso) {
        LocalDate hoy = fechaReferencia == null ? LocalDate.now() : fechaReferencia;
        int ventanaAviso = Math.max(0, diasAviso);

        List<Cuenta> tarjetas = cuentaService.listarPorTipo(TipoCuenta.CREDITO);
        List<ResumenTarjeta> resultado = new ArrayList<>();

        for (Cuenta tarjeta : tarjetas) {
            if (tarjeta.getDiaCorte() == null || tarjeta.getDiaPago() == null) {
                continue;
            }
            resultado.add(construirResumenTarjeta(tarjeta, hoy, ventanaAviso));
        }
        return resultado;
    }

    public List<AlertaPagoTarjeta> obtenerAlertasPagoProximo(LocalDate fechaReferencia, int diasAviso) {
        List<ResumenTarjeta> resumenes = obtenerResumenTarjetas(fechaReferencia, diasAviso);
        List<AlertaPagoTarjeta> alertas = new ArrayList<>();

        for (ResumenTarjeta resumen : resumenes) {
            ResumenCicloTarjeta cicloAlerta = resumen.proximoPagoAlerta();
            if (cicloAlerta != null) {
                alertas.add(new AlertaPagoTarjeta(
                        resumen.cuenta().getId(),
                        resumen.cuenta().getNombre(),
                        cicloAlerta.ciclo().fechaPago(),
                        cicloAlerta.diasParaPago(),
                        cicloAlerta.estimadoAPagar(),
                        cicloAlerta.cicloCerrado()
                ));
            }
        }
        return alertas;
    }

    public BigDecimal calcularTotalEstimadoProximoCorte(List<ResumenTarjeta> tarjetas) {
        BigDecimal total = Dinero.CERO;
        for (ResumenTarjeta t : tarjetas) {
            total = Dinero.sumar(total, t.cicloActual().estimadoAPagar());
        }
        return total;
    }

    private ResumenTarjeta construirResumenTarjeta(Cuenta tarjeta, LocalDate hoy, int ventanaAviso) {
        int diaCorte = tarjeta.getDiaCorte();
        int diaPago = tarjeta.getDiaPago();

        CicloFacturacion actual = CicloFacturacion.cicloActual(diaCorte, diaPago, hoy);
        List<CicloFacturacion> ciclos = new ArrayList<>();
        ciclos.add(actual);
        CicloFacturacion cursor = actual;
        for (int i = 1; i < CICLOS_HISTORIAL; i++) {
            cursor = cursor.anterior(diaCorte, diaPago);
            ciclos.add(cursor);
        }

        List<ResumenCicloTarjeta> historial = new ArrayList<>();
        List<Movimiento> movimientosCicloActual = List.of();

        for (int i = 0; i < ciclos.size(); i++) {
            CicloFacturacion ciclo = ciclos.get(i);
            List<Movimiento> movs = movimientoRepository.buscarPorCuentaEnPeriodo(
                    tarjeta.getId(), ciclo.inicio(), ciclo.fechaCorte());
            if (i == 0) {
                movimientosCicloActual = movs;
            }
            historial.add(resumirCiclo(ciclo, movs, hoy, ventanaAviso));
        }

        ResumenCicloTarjeta resumenActual = historial.getFirst();
        ResumenCicloTarjeta resumenAnterior = historial.size() > 1 ? historial.get(1) : null;

        // Prioriza alerta del ciclo recién cerrado si su pago está próximo; si no, evalúa el ciclo actual
        ResumenCicloTarjeta alerta = null;
        if (resumenAnterior != null && resumenAnterior.pagoProximo()) {
            alerta = resumenAnterior;
        } else if (resumenActual.pagoProximo()) {
            alerta = resumenActual;
        }

        return new ResumenTarjeta(
                tarjeta,
                resumenActual,
                resumenAnterior,
                historial,
                movimientosCicloActual,
                alerta
        );
    }

    private ResumenCicloTarjeta resumirCiclo(
            CicloFacturacion ciclo,
            List<Movimiento> movimientos,
            LocalDate hoy,
            int ventanaAviso
    ) {
        BigDecimal cargos = Dinero.CERO;
        BigDecimal abonos = Dinero.CERO;

        for (Movimiento m : movimientos) {
            if (m.getTipo() == TipoMovimiento.GASTO) {
                cargos = Dinero.sumar(cargos, m.getMonto());
            } else if (m.getTipo() == TipoMovimiento.INGRESO) {
                abonos = Dinero.sumar(abonos, m.getMonto());
            }
        }

        BigDecimal neto = Dinero.restar(cargos, abonos);
        BigDecimal estimado = neto.compareTo(BigDecimal.ZERO) > 0 ? neto : Dinero.CERO;

        long diasParaCorte = ciclo.diasHastaCorte(hoy);
        long diasParaPago = ciclo.diasHastaPago(hoy);
        boolean pagoProximo = diasParaPago >= 0 && diasParaPago <= ventanaAviso;
        boolean pagoHoy = diasParaPago == 0;
        boolean cicloCerrado = hoy.isAfter(ciclo.fechaCorte());

        return new ResumenCicloTarjeta(
                ciclo,
                cargos,
                abonos,
                estimado,
                movimientos.size(),
                diasParaCorte,
                diasParaPago,
                pagoProximo,
                pagoHoy,
                cicloCerrado
        );
    }
}
