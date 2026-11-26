package com.diariofinanciero.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Crea respaldos consistentes en caliente de la base de datos H2 mediante el comando
 * nativo BACKUP TO (en formato .zip con marca de fecha y hora) dentro de un directorio
 * controlado fuera del web root.
 */
@Service
public class RespaldoService {

    private static final DateTimeFormatter FORMATO_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final Pattern PATRON_NOMBRE_VALIDO =
            Pattern.compile("^diario-financiero-\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.zip$");

    private final JdbcTemplate jdbcTemplate;
    private final Path directorioRespaldos;

    public RespaldoService(
            JdbcTemplate jdbcTemplate,
            @Value("${diario.respaldos.directorio:./backups}") String directorioConfigurado
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.directorioRespaldos = Path.of(directorioConfigurado).toAbsolutePath().normalize();
    }

    public Path getDirectorioRespaldos() {
        return directorioRespaldos;
    }

    public ArchivoRespaldo crearRespaldo() {
        try {
            Files.createDirectories(directorioRespaldos);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo crear el directorio de respaldos", ex);
        }

        String nombreArchivo = "diario-financiero-" + FORMATO_TIMESTAMP.format(LocalDateTime.now()) + ".zip";
        Path destino = resolverDentroDeDirectorio(nombreArchivo);

        jdbcTemplate.execute((Connection conn) -> {
            try (PreparedStatement ps = conn.prepareStatement("BACKUP TO ?")) {
                ps.setString(1, destino.toString());
                ps.execute();
            }
            return null;
        });

        return describirArchivo(destino);
    }

    public List<ArchivoRespaldo> listarRespaldos() {
        if (!Files.isDirectory(directorioRespaldos)) {
            return List.of();
        }
        try (Stream<Path> archivos = Files.list(directorioRespaldos)) {
            return archivos
                    .filter(Files::isRegularFile)
                    .filter(p -> PATRON_NOMBRE_VALIDO.matcher(p.getFileName().toString()).matches())
                    .map(this::describirArchivo)
                    .sorted(Comparator.comparing(ArchivoRespaldo::fechaCreacion).reversed())
                    .toList();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo listar el directorio de respaldos", ex);
        }
    }

    public byte[] leerRespaldo(String nombreArchivo) {
        if (nombreArchivo == null || !PATRON_NOMBRE_VALIDO.matcher(nombreArchivo).matches()) {
            throw new RecursoNoEncontradoException("Archivo de respaldo no válido");
        }
        Path archivo = resolverDentroDeDirectorio(nombreArchivo);
        if (!Files.isRegularFile(archivo)) {
            throw new RecursoNoEncontradoException("Respaldo no encontrado: " + nombreArchivo);
        }
        try {
            return Files.readAllBytes(archivo);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer el archivo de respaldo", ex);
        }
    }

    private Path resolverDentroDeDirectorio(String nombreArchivo) {
        Path soloNombre = Path.of(nombreArchivo).getFileName();
        Path resuelto = directorioRespaldos.resolve(soloNombre).normalize();
        String prefijoSeguro = directorioRespaldos.toString() + File.separator;
        if (!resuelto.toString().startsWith(prefijoSeguro)) {
            throw new IllegalArgumentException("Ruta de respaldo fuera del directorio permitido");
        }
        return resuelto;
    }

    private ArchivoRespaldo describirArchivo(Path ruta) {
        try {
            long bytes = Files.size(ruta);
            Instant modificado = Files.getLastModifiedTime(ruta).toInstant();
            LocalDateTime fecha = LocalDateTime.ofInstant(modificado, ZoneId.systemDefault());
            return new ArchivoRespaldo(ruta.getFileName().toString(), fecha, bytes, formatearTamano(bytes));
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudieron leer los metadatos del respaldo", ex);
        }
    }

    private String formatearTamano(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024.0) {
            return String.format("%.1f KB", kb);
        }
        return String.format("%.2f MB", kb / 1024.0);
    }
}
