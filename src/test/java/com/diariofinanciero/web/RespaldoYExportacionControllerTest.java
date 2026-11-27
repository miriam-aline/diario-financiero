package com.diariofinanciero.web;

import com.diariofinanciero.service.ArchivoRespaldo;
import com.diariofinanciero.service.CategoriaService;
import com.diariofinanciero.service.ExportacionCsvService;
import com.diariofinanciero.service.RecursoNoEncontradoException;
import com.diariofinanciero.service.RespaldoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class RespaldoYExportacionControllerTest {

    @Mock
    private ExportacionCsvService exportacionCsvService;
    @Mock
    private RespaldoService respaldoService;
    @Mock
    private CategoriaService categoriaService;

    @InjectMocks
    private RespaldoYExportacionController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /respaldo debe mostrar lista de respaldos, directorio y categorías")
    void verRespaldo() throws Exception {
        when(respaldoService.listarRespaldos()).thenReturn(List.of());
        when(respaldoService.getDirectorioRespaldos()).thenReturn(Path.of("backups"));
        when(categoriaService.listarTodas()).thenReturn(List.of());

        mockMvc.perform(get("/respaldo"))
                .andExpect(status().isOk())
                .andExpect(view().name("respaldo/index"))
                .andExpect(model().attribute("paginaActiva", "respaldo"))
                .andExpect(model().attribute("directorioRespaldos", "backups"));
    }

    @Test
    @DisplayName("POST /respaldo/crear debe crear respaldo o reportar error si falla")
    void crearRespaldo() throws Exception {
        ArchivoRespaldo archivo = new ArchivoRespaldo("respaldo.zip", LocalDateTime.now(), 1024L, "1.0 KB");
        when(respaldoService.crearRespaldo()).thenReturn(archivo);

        mockMvc.perform(post("/respaldo/crear"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/respaldo"))
                .andExpect(flash().attributeExists("mensajeExito"));

        when(respaldoService.crearRespaldo()).thenThrow(new IllegalStateException("Error IO"));
        mockMvc.perform(post("/respaldo/crear"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/respaldo"))
                .andExpect(flash().attributeExists("mensajeError"));
    }

    @Test
    @DisplayName("GET /respaldo/descargar y /exportar/movimientos.csv deben entregar archivos y manejar 404")
    void descargarYExportar() throws Exception {
        byte[] zip = new byte[]{1, 2, 3};
        when(respaldoService.leerRespaldo("backup.zip")).thenReturn(zip);

        mockMvc.perform(get("/respaldo/descargar/backup.zip"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"backup.zip\""))
                .andExpect(content().bytes(zip));

        when(respaldoService.leerRespaldo("invalido.zip")).thenThrow(new RecursoNoEncontradoException("No válido"));
        mockMvc.perform(get("/respaldo/descargar/invalido.zip"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));

        byte[] csv = "Fecha,Tipo\n".getBytes(StandardCharsets.UTF_8);
        when(exportacionCsvService.exportarMovimientos(any())).thenReturn(csv);

        mockMvc.perform(get("/exportar/movimientos.csv").param("texto", "cafe"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(csv));
    }
}
