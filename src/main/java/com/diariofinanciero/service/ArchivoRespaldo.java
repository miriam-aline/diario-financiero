package com.diariofinanciero.service;

import java.time.LocalDateTime;

public record ArchivoRespaldo(
        String nombreArchivo,
        LocalDateTime fechaCreacion,
        long tamanoBytes,
        String tamanoLegible
) {
}
