package com.diariofinanciero.domain;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Calcula un hash SHA-256 determinístico para detectar duplicados al re-importar
 * movimientos bancarios, incorporando el índice de ocurrencia dentro del mismo día
 * y descripción para no fusionar dos compras reales iguales el mismo día.
 */
public final class HashImportacion {

    private HashImportacion() {
    }

    public static String normalizarTexto(String texto) {
        if (texto == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static String claveBase(
            Long cuentaId,
            LocalDate fecha,
            TipoMovimiento tipo,
            BigDecimal monto,
            String descripcion
    ) {
        BigDecimal montoNormalizado = Dinero.validarPositivo(monto);
        return cuentaId + "|" + fecha + "|" + tipo.name() + "|"
                + montoNormalizado.toPlainString() + "|" + normalizarTexto(descripcion);
    }

    public static String calcular(
            Long cuentaId,
            LocalDate fecha,
            TipoMovimiento tipo,
            BigDecimal monto,
            String descripcion,
            int ocurrencia
    ) {
        if (ocurrencia <= 0) {
            throw new IllegalArgumentException("La ocurrencia debe ser mayor o igual a 1");
        }
        String carga = claveBase(cuentaId, fecha, tipo, monto, descripcion) + "#" + ocurrencia;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(carga.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Algoritmo SHA-256 no disponible", ex);
        }
    }
}
