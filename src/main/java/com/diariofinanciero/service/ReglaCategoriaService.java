package com.diariofinanciero.service;

import com.diariofinanciero.domain.Categoria;
import com.diariofinanciero.domain.HashImportacion;
import com.diariofinanciero.domain.ReglaCategoria;
import com.diariofinanciero.repository.ReglaCategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ReglaCategoriaService {

    private final ReglaCategoriaRepository reglaCategoriaRepository;
    private final CategoriaService categoriaService;

    public ReglaCategoriaService(
            ReglaCategoriaRepository reglaCategoriaRepository,
            CategoriaService categoriaService
    ) {
        this.reglaCategoriaRepository = reglaCategoriaRepository;
        this.categoriaService = categoriaService;
    }

    public List<ReglaCategoria> listarTodas() {
        return reglaCategoriaRepository.findAllConCategoriaOrdenadas();
    }

    @Transactional
    public ReglaCategoria guardarRegla(String textoContiene, Long categoriaId) {
        Categoria categoria = categoriaService.obtenerPorId(categoriaId);
        String textoNormalizado = HashImportacion.normalizarTexto(textoContiene);
        if (textoNormalizado.isEmpty()) {
            throw new IllegalArgumentException("El texto de la regla es obligatorio");
        }

        Optional<ReglaCategoria> existente = reglaCategoriaRepository.findByTextoContieneIgnoreCase(textoNormalizado);
        if (existente.isPresent()) {
            ReglaCategoria regla = existente.get();
            regla.actualizarCategoria(categoria);
            return reglaCategoriaRepository.save(regla);
        }

        return reglaCategoriaRepository.save(new ReglaCategoria(textoNormalizado, categoria));
    }

    @Transactional
    public void eliminar(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El identificador de la regla es obligatorio");
        }
        ReglaCategoria regla = reglaCategoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Regla no encontrada con id: " + id));
        reglaCategoriaRepository.delete(regla);
    }

    public Optional<ReglaCategoria> buscarCoincidencia(String descripcion, List<ReglaCategoria> reglas) {
        for (ReglaCategoria regla : reglas) {
            if (regla.coincide(descripcion)) {
                return Optional.of(regla);
            }
        }
        return Optional.empty();
    }
}
