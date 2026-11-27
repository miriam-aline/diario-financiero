package com.diariofinanciero.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RespaldoServiceTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Debe crear respaldo ejecutando BACKUP TO y devolver metadatos del archivo")
    void crearRespaldo() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Path dirRespaldos = tempDir.resolve("backups");
        RespaldoService service = new RespaldoService(jdbcTemplate, dirRespaldos.toString());

        assertEquals(dirRespaldos.toAbsolutePath().normalize(), service.getDirectorioRespaldos());

        AtomicReference<Path> rutaCapturada = new AtomicReference<>();
        when(jdbcTemplate.execute(any(ConnectionCallback.class))).thenAnswer(inv -> {
            ConnectionCallback<?> cb = inv.getArgument(0);
            Connection conn = mock(Connection.class);
            PreparedStatement ps = mock(PreparedStatement.class);
            when(conn.prepareStatement("BACKUP TO ?")).thenReturn(ps);
            org.mockito.Mockito.doAnswer(setInv -> {
                Path p = Path.of((String) setInv.getArgument(1));
                rutaCapturada.set(p);
                Files.write(p, new byte[2048]);
                return null;
            }).when(ps).setString(org.mockito.ArgumentMatchers.eq(1), any(String.class));
            return cb.doInConnection(conn);
        });

        ArchivoRespaldo creado = service.crearRespaldo();

        assertTrue(Files.exists(rutaCapturada.get()));
        assertEquals(2048L, creado.tamanoBytes());
        assertTrue(creado.nombreArchivo().startsWith("diario-financiero-"));
        assertTrue(creado.nombreArchivo().endsWith(".zip"));
        assertTrue(creado.tamanoLegible().contains("KB"));
    }

    @Test
    @DisplayName("Debe listar respaldos válidos ordenados del más reciente al más antiguo")
    void listarRespaldos() throws IOException {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Path dirInexistente = tempDir.resolve("no-existe");
        RespaldoService serviceSinDir = new RespaldoService(jdbcTemplate, dirInexistente.toString());
        assertTrue(serviceSinDir.listarRespaldos().isEmpty());

        Path dir = tempDir.resolve("backups");
        Files.createDirectories(dir);

        Path antiguo = Files.write(dir.resolve("diario-financiero-2026-01-01_10-00-00.zip"), new byte[10]);
        Files.setLastModifiedTime(antiguo, FileTime.from(Instant.parse("2026-01-01T10:00:00Z")));

        Path reciente = Files.write(dir.resolve("diario-financiero-2026-05-01_12-00-00.zip"), new byte[20]);
        Files.setLastModifiedTime(reciente, FileTime.from(Instant.parse("2026-05-01T12:00:00Z")));

        Files.write(dir.resolve("archivo-ajeno.txt"), new byte[5]);

        RespaldoService service = new RespaldoService(jdbcTemplate, dir.toString());
        List<ArchivoRespaldo> lista = service.listarRespaldos();

        assertEquals(2, lista.size());
        assertEquals("diario-financiero-2026-05-01_12-00-00.zip", lista.get(0).nombreArchivo());
        assertEquals("diario-financiero-2026-01-01_10-00-00.zip", lista.get(1).nombreArchivo());
        assertEquals("20 B", lista.get(0).tamanoLegible());
    }

    @Test
    @DisplayName("Debe leer respaldo válido y rechazar nombres inválidos o inexistentes")
    void leerRespaldo() throws IOException {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Path dir = tempDir.resolve("backups");
        Files.createDirectories(dir);
        byte[] contenido = "contenido-zip".getBytes(StandardCharsets.UTF_8);
        String nombreValido = "diario-financiero-2026-05-01_12-00-00.zip";
        Files.write(dir.resolve(nombreValido), contenido);

        RespaldoService service = new RespaldoService(jdbcTemplate, dir.toString());

        assertArrayEquals(contenido, service.leerRespaldo(nombreValido));
        assertThrows(RecursoNoEncontradoException.class, () -> service.leerRespaldo(null));
        assertThrows(RecursoNoEncontradoException.class, () -> service.leerRespaldo("../secreto.zip"));
        assertThrows(RecursoNoEncontradoException.class, () -> service.leerRespaldo("diario-financiero-2026-05-01_12-00-01.zip"));
    }
}
