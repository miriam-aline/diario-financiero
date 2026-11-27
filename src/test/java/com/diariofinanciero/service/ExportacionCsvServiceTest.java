package com.diariofinanciero.service;

import com.diariofinanciero.domain.Categoria;
import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.Movimiento;
import com.diariofinanciero.domain.OrigenMovimiento;
import com.diariofinanciero.domain.TipoCuenta;
import com.diariofinanciero.domain.TipoMovimiento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportacionCsvServiceTest {

    @Mock
    private MovimientoService movimientoService;

    @InjectMocks
    private ExportacionCsvService exportacionCsvService;

    @Test
    @DisplayName("Debe generar CSV con BOM UTF-8, encabezados, formato de fecha y protección contra fórmulas")
    void exportarMovimientos() {
        Categoria cat = new Categoria("=Comida");
        Cuenta cuenta = new Cuenta("+Efectivo", TipoCuenta.EFECTIVO);
        Movimiento mov1 = Movimiento.manual(
                LocalDate.of(2026, 5, 14),
                new BigDecimal("125.50"),
                TipoMovimiento.GASTO,
                cat,
                cuenta,
                "@SUM(A1)"
        );
        Movimiento mov2 = new Movimiento(
                LocalDate.of(2026, 5, 15),
                new BigDecimal("2000.00"),
                TipoMovimiento.INGRESO,
                new Categoria("Salario"),
                new Cuenta("Débito", TipoCuenta.DEBITO),
                "-Descuento",
                OrigenMovimiento.IMPORTADO,
                "hash-1"
        );

        FiltroMovimiento filtro = FiltroMovimiento.vacio();
        when(movimientoService.listar(filtro)).thenReturn(List.of(mov1, mov2));

        byte[] bytes = exportacionCsvService.exportarMovimientos(filtro);

        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);

        String csv = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("Fecha,Tipo,Monto (MXN),Categoría,Cuenta,Descripción,Origen"));
        assertTrue(csv.contains("14/05/2026,Gasto,125.50,'=Comida,'+Efectivo,'@SUM(A1),Manual"));
        assertTrue(csv.contains("15/05/2026,Ingreso,2000.00,Salario,Débito,'-Descuento,Importado"));
    }
}
