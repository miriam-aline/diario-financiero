package com.diariofinanciero.service;

import com.diariofinanciero.domain.Movimiento;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ExportacionCsvService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private final MovimientoService movimientoService;

    public ExportacionCsvService(MovimientoService movimientoService) {
        this.movimientoService = movimientoService;
    }

    public byte[] exportarMovimientos(FiltroMovimiento filtro) {
        List<Movimiento> movimientos = movimientoService.listar(filtro);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            // BOM UTF-8 para compatibilidad directa con Excel en macOS y Windows
            out.write(UTF8_BOM);
            try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
                 CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                         .setHeader("Fecha", "Tipo", "Monto (MXN)", "Categoría", "Cuenta", "Descripción", "Origen")
                         .get())) {

                for (Movimiento m : movimientos) {
                    printer.printRecord(
                            FORMATO_FECHA.format(m.getFecha()),
                            m.getTipo().getEtiqueta(),
                            m.getMonto().toPlainString(),
                            sanitizarCeldaCsv(m.getCategoria().getNombre()),
                            sanitizarCeldaCsv(m.getCuenta().getNombre()),
                            sanitizarCeldaCsv(m.getDescripcion()),
                            m.getOrigen().getEtiqueta()
                    );
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Error al generar el archivo CSV de movimientos", ex);
        }

        return out.toByteArray();
    }

    /**
     * Previene CSV Formula Injection en hojas de cálculo (Excel / Sheets)
     * cuando un texto inicia con caracteres de fórmula (=, +, -, @, tab o CR).
     */
    private String sanitizarCeldaCsv(String valor) {
        if (valor == null || valor.isEmpty()) {
            return "";
        }
        char primero = valor.charAt(0);
        if (primero == '=' || primero == '+' || primero == '-' || primero == '@' || primero == '\t' || primero == '\r') {
            return "'" + valor;
        }
        return valor;
    }
}
