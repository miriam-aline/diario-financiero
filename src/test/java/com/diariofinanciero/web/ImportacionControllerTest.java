package com.diariofinanciero.web;

import com.diariofinanciero.domain.Categoria;
import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.TipoCuenta;
import com.diariofinanciero.service.CategoriaService;
import com.diariofinanciero.service.CuentaService;
import com.diariofinanciero.service.ImportacionCsvService;
import com.diariofinanciero.service.RecursoNoEncontradoException;
import com.diariofinanciero.service.ReglaCategoriaService;
import com.diariofinanciero.service.ResultadoConfirmacionImportacion;
import com.diariofinanciero.service.VistaPreviaImportacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class ImportacionControllerTest {

    @Mock
    private ImportacionCsvService importacionCsvService;
    @Mock
    private ReglaCategoriaService reglaCategoriaService;
    @Mock
    private CuentaService cuentaService;
    @Mock
    private CategoriaService categoriaService;

    @InjectMocks
    private ImportacionController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /importacion debe cargar formulario y excluir Sin categoría del catálogo asignable")
    void mostrarFormulario() throws Exception {
        Cuenta cta = new Cuenta("Débito", TipoCuenta.DEBITO);
        ReflectionTestUtils.setField(cta, "id", 1L);
        Categoria comida = new Categoria("Comida");
        Categoria sinCat = new Categoria("Sin categoría");

        when(cuentaService.listarTodas()).thenReturn(List.of(cta));
        when(categoriaService.listarTodas()).thenReturn(List.of(comida, sinCat));
        when(reglaCategoriaService.listarTodas()).thenReturn(List.of());
        when(importacionCsvService.listarPendientesRevision()).thenReturn(List.of());

        mockMvc.perform(get("/importacion"))
                .andExpect(status().isOk())
                .andExpect(view().name("importacion/index"))
                .andExpect(model().attribute("categorias", List.of(comida)))
                .andExpect(model().attribute("pendientesCount", 0));
    }

    @Test
    @DisplayName("POST /importacion/vista-previa debe validar extensión .csv y mostrar vista previa válida")
    void vistaPrevia() throws Exception {
        MockMultipartFile txt = new MockMultipartFile("archivo", "datos.txt", "text/plain", "a,b".getBytes(StandardCharsets.UTF_8));
        when(cuentaService.listarTodas()).thenReturn(List.of());
        when(categoriaService.listarTodas()).thenReturn(List.of());
        when(reglaCategoriaService.listarTodas()).thenReturn(List.of());
        when(importacionCsvService.listarPendientesRevision()).thenReturn(List.of());

        mockMvc.perform(multipart("/importacion/vista-previa").file(txt).param("cuentaId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("importacion/index"))
                .andExpect(model().attribute("mensajeError", "Solo se permiten archivos con extensión .csv"));

        MockMultipartFile csv = new MockMultipartFile("archivo", "banco.csv", "text/csv", "Fecha,Desc,Cargo\n".getBytes(StandardCharsets.UTF_8));
        VistaPreviaImportacion vista = new VistaPreviaImportacion("tok", 1L, "Débito", Instant.now(), 1, 1, 0, 0, 0, List.of(), List.of());
        when(importacionCsvService.generarVistaPrevia(any(), any())).thenReturn(vista);

        mockMvc.perform(multipart("/importacion/vista-previa").file(csv).param("cuentaId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("importacion/vista-previa"))
                .andExpect(model().attribute("vista", vista));
    }

    @Test
    @DisplayName("POST /importacion/confirmar debe redirigir a revisión si hay sin categoría o a movimientos si no")
    void confirmar() throws Exception {
        when(importacionCsvService.confirmarImportacion("tok-rev"))
                .thenReturn(new ResultadoConfirmacionImportacion(3, 1, 2));

        mockMvc.perform(post("/importacion/confirmar").param("token", "tok-rev"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/importacion/revision"))
                .andExpect(flash().attributeExists("mensajeExito"));

        when(importacionCsvService.confirmarImportacion("tok-ok"))
                .thenReturn(new ResultadoConfirmacionImportacion(2, 0, 0));

        mockMvc.perform(post("/importacion/confirmar").param("token", "tok-ok"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movimientos"));

        when(importacionCsvService.confirmarImportacion("tok-bad"))
                .thenThrow(new IllegalArgumentException("Expirado"));

        mockMvc.perform(post("/importacion/confirmar").param("token", "tok-bad"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/importacion"))
                .andExpect(flash().attribute("mensajeError", "Expirado"));
    }

    @Test
    @DisplayName("Debe atender revisión, clasificación, reglas, ejemplo.csv y 404")
    void revisionReglasEjemploY404() throws Exception {
        when(importacionCsvService.listarPendientesRevision()).thenReturn(List.of());
        when(categoriaService.listarTodas()).thenReturn(List.of(new Categoria("Comida")));

        mockMvc.perform(get("/importacion/revision"))
                .andExpect(status().isOk())
                .andExpect(view().name("importacion/revision"));

        when(importacionCsvService.clasificarPendiente(5L, 2L, true, "cafe")).thenReturn(3);
        mockMvc.perform(post("/importacion/revision/5")
                        .param("categoriaId", "2")
                        .param("guardarRegla", "true")
                        .param("textoRegla", "cafe"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/importacion/revision"))
                .andExpect(flash().attributeExists("mensajeExito"));

        mockMvc.perform(post("/importacion/reglas").param("textoContiene", "uber").param("categoriaId", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/importacion"));

        mockMvc.perform(post("/importacion/reglas/8/eliminar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/importacion"));

        when(importacionCsvService.generarCsvEjemplo()).thenReturn("Fecha,Descripción\n");
        mockMvc.perform(get("/importacion/ejemplo.csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"ejemplo-banco.csv\""))
                .andExpect(content().string("Fecha,Descripción\n"));

        doThrow(new RecursoNoEncontradoException("Regla no existe")).when(reglaCategoriaService).eliminar(99L);
        mockMvc.perform(post("/importacion/reglas/99/eliminar"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }
}
