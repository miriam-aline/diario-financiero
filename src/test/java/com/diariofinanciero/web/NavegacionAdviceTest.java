package com.diariofinanciero.web;

import com.diariofinanciero.domain.Categoria;
import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.Movimiento;
import com.diariofinanciero.domain.TipoCuenta;
import com.diariofinanciero.domain.TipoMovimiento;
import com.diariofinanciero.service.AlertaPagoTarjeta;
import com.diariofinanciero.service.ImportacionCsvService;
import com.diariofinanciero.service.TarjetaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NavegacionAdviceTest {

    @Mock
    private ImportacionCsvService importacionCsvService;

    @Mock
    private TarjetaService tarjetaService;

    @InjectMocks
    private NavegacionAdvice navegacionAdvice;

    @Test
    @DisplayName("Debe devolver contadores globales de pendientes CSV y alertas de tarjetas")
    void contadoresGlobales() {
        Movimiento mov = Movimiento.manual(
                LocalDate.of(2026, 5, 1), BigDecimal.TEN, TipoMovimiento.GASTO,
                new Categoria("Sin categoría"), new Cuenta("Débito", TipoCuenta.DEBITO), "Cargo"
        );
        when(importacionCsvService.listarPendientesRevision()).thenReturn(List.of(mov, mov));
        when(tarjetaService.obtenerAlertasPagoProximo(any(LocalDate.class), eq(TarjetaService.DIAS_AVISO_PAGO_POR_DEFECTO)))
                .thenReturn(List.of(new AlertaPagoTarjeta(1L, "Oro", LocalDate.of(2026, 5, 5), 2L, BigDecimal.TEN, true)));

        assertEquals(2, navegacionAdvice.navPendientesCsv());
        assertEquals(1, navegacionAdvice.navAlertasTarjetas());
    }
}
