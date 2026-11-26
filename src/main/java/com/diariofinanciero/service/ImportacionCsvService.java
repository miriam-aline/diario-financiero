package com.diariofinanciero.service;

import com.diariofinanciero.domain.Categoria;
import com.diariofinanciero.domain.Cuenta;
import com.diariofinanciero.domain.Dinero;
import com.diariofinanciero.domain.HashImportacion;
import com.diariofinanciero.domain.Movimiento;
import com.diariofinanciero.domain.OrigenMovimiento;
import com.diariofinanciero.domain.ReglaCategoria;
import com.diariofinanciero.domain.TipoMovimiento;
import com.diariofinanciero.repository.CategoriaRepository;
import com.diariofinanciero.repository.MovimientoRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Procesa archivos CSV bancarios en memoria (sin almacenar archivos subidos en disco),
 * detecta duplicados con hash SHA-256 por ocurrencia y aplica ReglaCategoria.
 *
 * TODO(security): Al ser una herramienta local de un solo usuario que parsea texto plano
 * CSV en memoria (con límite de 5 MB y validación de extensión/bytes nulos), no integra
 * escáner antivirus externo.
 */
@Service
@Transactional(readOnly = true)
public class ImportacionCsvService {

    public static final String NOMBRE_SIN_CATEGORIA = "Sin categoría";
    public static final long MAX_TAMANO_BYTES = 5L * 1024L * 1024L;
    private static final int MAX_PREVIEWS_EN_MEMORIA = 10;
    private static final Duration EXPIRACION_PREVIEW = Duration.ofMinutes(30);

    private final CuentaService cuentaService;
    private final CategoriaService categoriaService;
    private final CategoriaRepository categoriaRepository;
    private final MovimientoRepository movimientoRepository;
    private final ReglaCategoriaService reglaCategoriaService;

    private final Map<String, VistaPreviaImportacion> vistasPrevias = new ConcurrentHashMap<>();

    public ImportacionCsvService(
            CuentaService cuentaService,
            CategoriaService categoriaService,
            CategoriaRepository categoriaRepository,
            MovimientoRepository movimientoRepository,
            ReglaCategoriaService reglaCategoriaService
    ) {
        this.cuentaService = cuentaService;
        this.categoriaService = categoriaService;
        this.categoriaRepository = categoriaRepository;
        this.movimientoRepository = movimientoRepository;
        this.reglaCategoriaService = reglaCategoriaService;
    }

    public VistaPreviaImportacion generarVistaPrevia(byte[] contenidoBytes, ConfiguracionCsv config) {
        validarContenidoTexto(contenidoBytes);
        Cuenta cuenta = cuentaService.obtenerPorId(config.cuentaId());
        DateTimeFormatter formatter = construirFormatter(config.formatoFecha());

        String textoCsv = decodificarSinBom(contenidoBytes);
        Categoria sinCategoria = obtenerOCrearSinCategoriaLectura();
        List<ReglaCategoria> reglas = reglaCategoriaService.listarTodas();

        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setDelimiter(config.separador())
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .setIgnoreSurroundingSpaces(true)
                .get();

        List<FilaTemporal> parseadas = new ArrayList<>();
        List<String> avisos = new ArrayList<>();
        Map<String, Integer> ocurrenciasEnArchivo = new HashMap<>();
        int totalLeidas = 0;

        try (CSVParser parser = CSVParser.parse(new StringReader(textoCsv), formato)) {
            List<CSVRecord> registros = parser.getRecords();
            if (registros.isEmpty()) {
                throw new IllegalArgumentException("El archivo CSV está vacío");
            }

            Map<String, Integer> mapaEncabezados = new HashMap<>();
            int filaInicio = 0;
            if (config.primeraFilaEsEncabezado()) {
                CSVRecord encabezado = registros.getFirst();
                for (int i = 0; i < encabezado.size(); i++) {
                    String clave = HashImportacion.normalizarTexto(encabezado.get(i));
                    if (!clave.isEmpty()) {
                        mapaEncabezados.putIfAbsent(clave, i);
                    }
                }
                filaInicio = 1;
            }

            int idxFecha = resolverIndiceColumna(config.columnaFecha(), mapaEncabezados, config.primeraFilaEsEncabezado(), "fecha");
            int idxDesc = resolverIndiceColumna(config.columnaDescripcion(), mapaEncabezados, config.primeraFilaEsEncabezado(), "descripción");
            Integer idxCargo = config.columnaCargo() == null
                    ? null
                    : resolverIndiceColumna(config.columnaCargo(), mapaEncabezados, config.primeraFilaEsEncabezado(), "cargo");
            Integer idxAbono = config.columnaAbono() == null
                    ? null
                    : resolverIndiceColumna(config.columnaAbono(), mapaEncabezados, config.primeraFilaEsEncabezado(), "abono");

            for (int r = filaInicio; r < registros.size(); r++) {
                CSVRecord record = registros.get(r);
                int numeroLinea = r + 1;
                totalLeidas++;

                if (esFilaVacia(record)) {
                    avisos.add("Línea " + numeroLinea + ": fila vacía omitida.");
                    continue;
                }

                String rawFecha = obtenerCelda(record, idxFecha);
                String rawDesc = obtenerCelda(record, idxDesc);

                if (esFilaDeTotales(rawFecha, rawDesc)) {
                    avisos.add("Línea " + numeroLinea + ": fila de totales/resumen omitida.");
                    continue;
                }

                LocalDate fecha = intentarParsearFecha(rawFecha, formatter);
                if (fecha == null) {
                    avisos.add("Línea " + numeroLinea + ": fecha inválida (\"" + rawFecha + "\"), omitida.");
                    continue;
                }

                if (rawDesc.isBlank()) {
                    avisos.add("Línea " + numeroLinea + ": descripción vacía, omitida.");
                    continue;
                }

                MontoTipo montoTipo = extraerMontoYTipo(record, idxCargo, idxAbono);
                if (montoTipo == null) {
                    avisos.add("Línea " + numeroLinea + ": sin cargo ni abono válido (> 0), omitida.");
                    continue;
                }

                String descripcionLimpia = rawDesc.trim();
                if (descripcionLimpia.length() > 255) {
                    descripcionLimpia = descripcionLimpia.substring(0, 255);
                }

                String claveBase = HashImportacion.claveBase(
                        cuenta.getId(), fecha, montoTipo.tipo(), montoTipo.monto(), descripcionLimpia);
                int ocurrencia = ocurrenciasEnArchivo.merge(claveBase, 1, Integer::sum);
                String hash = HashImportacion.calcular(
                        cuenta.getId(), fecha, montoTipo.tipo(), montoTipo.monto(), descripcionLimpia, ocurrencia);

                Optional<ReglaCategoria> regla = reglaCategoriaService.buscarCoincidencia(descripcionLimpia, reglas);
                Categoria categoriaAsignada = regla.map(ReglaCategoria::getCategoria).orElse(sinCategoria);
                boolean esSinCat = regla.isEmpty();

                parseadas.add(new FilaTemporal(
                        numeroLinea,
                        fecha,
                        descripcionLimpia,
                        montoTipo.tipo(),
                        montoTipo.monto(),
                        categoriaAsignada.getId(),
                        categoriaAsignada.getNombre(),
                        regla.map(ReglaCategoria::getTextoContiene).orElse(null),
                        hash,
                        esSinCat
                ));
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo leer el archivo CSV: " + ex.getMessage());
        }

        Set<String> todosHashes = new HashSet<>();
        for (FilaTemporal f : parseadas) {
            todosHashes.add(f.hash());
        }
        Set<String> existentesEnBd = todosHashes.isEmpty()
                ? Set.of()
                : movimientoRepository.findHashesExistentes(todosHashes);

        List<FilaImportacionPreview> filasPreview = new ArrayList<>();
        int nuevos = 0;
        int duplicados = 0;
        int sinCatNuevos = 0;

        for (FilaTemporal f : parseadas) {
            boolean esDuplicado = existentesEnBd.contains(f.hash());
            if (esDuplicado) {
                duplicados++;
            } else {
                nuevos++;
                if (f.sinCategoria()) {
                    sinCatNuevos++;
                }
            }
            filasPreview.add(new FilaImportacionPreview(
                    f.numeroLinea(),
                    f.fecha(),
                    f.descripcion(),
                    f.tipo(),
                    f.monto(),
                    f.categoriaId(),
                    f.categoriaNombre(),
                    f.reglaAplicada(),
                    f.hash(),
                    esDuplicado,
                    f.sinCategoria()
            ));
        }

        limpiarVistasExpiradas();
        String token = UUID.randomUUID().toString();
        VistaPreviaImportacion vistaPrevia = new VistaPreviaImportacion(
                token,
                cuenta.getId(),
                cuenta.getNombre(),
                Instant.now(),
                totalLeidas,
                nuevos,
                duplicados,
                avisos.size(),
                sinCatNuevos,
                List.copyOf(filasPreview),
                List.copyOf(avisos)
        );
        vistasPrevias.put(token, vistaPrevia);
        return vistaPrevia;
    }

    @Transactional
    public ResultadoConfirmacionImportacion confirmarImportacion(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token de confirmación inválido o expirado");
        }
        VistaPreviaImportacion vista = vistasPrevias.remove(token);
        if (vista == null) {
            throw new IllegalArgumentException("La vista previa expiró o ya fue confirmada. Sube el archivo nuevamente.");
        }

        Cuenta cuenta = cuentaService.obtenerPorId(vista.cuentaId());
        List<FilaImportacionPreview> candidatas = vista.filas().stream()
                .filter(f -> !f.duplicado())
                .toList();

        if (candidatas.isEmpty()) {
            return new ResultadoConfirmacionImportacion(0, vista.totalDuplicados(), 0);
        }

        Set<String> hashesCandidatos = new HashSet<>();
        for (FilaImportacionPreview f : candidatas) {
            hashesCandidatos.add(f.hashImportacion());
        }
        Set<String> yaExisten = movimientoRepository.findHashesExistentes(hashesCandidatos);

        Map<Long, Categoria> cacheCategorias = new HashMap<>();
        int importados = 0;
        int duplicadosAlGuardar = vista.totalDuplicados();
        int sinCategoriaGuardados = 0;

        for (FilaImportacionPreview f : candidatas) {
            if (yaExisten.contains(f.hashImportacion())) {
                duplicadosAlGuardar++;
                continue;
            }
            Categoria categoria = cacheCategorias.computeIfAbsent(
                    f.categoriaSugeridaId(), categoriaService::obtenerPorId);

            Movimiento movimiento = new Movimiento(
                    f.fecha(),
                    f.monto(),
                    f.tipo(),
                    categoria,
                    cuenta,
                    f.descripcion(),
                    OrigenMovimiento.IMPORTADO,
                    f.hashImportacion()
            );
            movimientoRepository.save(movimiento);
            importados++;
            if (f.sinCategoria()) {
                sinCategoriaGuardados++;
            }
        }

        return new ResultadoConfirmacionImportacion(importados, duplicadosAlGuardar, sinCategoriaGuardados);
    }

    public List<Movimiento> listarPendientesRevision() {
        return movimientoRepository.buscarPorCategoriaNombre(NOMBRE_SIN_CATEGORIA);
    }

    @Transactional
    public int clasificarPendiente(
            Long movimientoId,
            Long categoriaId,
            boolean guardarRegla,
            String textoRegla
    ) {
        Movimiento movimiento = movimientoRepository.findByIdConRelaciones(movimientoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Movimiento no encontrado con id: " + movimientoId));
        Categoria categoria = categoriaService.obtenerPorId(categoriaId);

        movimiento.actualizar(
                movimiento.getFecha(),
                movimiento.getMonto(),
                movimiento.getTipo(),
                categoria,
                movimiento.getCuenta(),
                movimiento.getDescripcion()
        );
        movimientoRepository.save(movimiento);

        int adicionalesActualizados = 0;
        if (guardarRegla) {
            String patron = (textoRegla == null || textoRegla.isBlank())
                    ? movimiento.getDescripcion()
                    : textoRegla;
            ReglaCategoria regla = reglaCategoriaService.guardarRegla(patron, categoriaId);

            // Aplica la regla recién creada al resto de movimientos aún en "Sin categoría"
            List<Movimiento> pendientes = movimientoRepository.buscarPorCategoriaNombre(NOMBRE_SIN_CATEGORIA);
            for (Movimiento pendiente : pendientes) {
                if (!pendiente.getId().equals(movimientoId) && regla.coincide(pendiente.getDescripcion())) {
                    pendiente.actualizar(
                            pendiente.getFecha(),
                            pendiente.getMonto(),
                            pendiente.getTipo(),
                            categoria,
                            pendiente.getCuenta(),
                            pendiente.getDescripcion()
                    );
                    movimientoRepository.save(pendiente);
                    adicionalesActualizados++;
                }
            }
        }

        return 1 + adicionalesActualizados;
    }

    public String generarCsvEjemplo() {
        return """
                Fecha,Descripción,Cargo,Abono
                24/11/2026,SUPERMERCADO DEL VALLE,"1,250.50",
                24/11/2026,CAFETERIA CENTRO,65.00,
                24/11/2026,CAFETERIA CENTRO,65.00,
                25/11/2026,TRANSPORTE URBANO METROPOLITANO,48.00,
                25/11/2026,DEPOSITO NOMINA QUINCENAL,,"15,000.00"
                26/11/2026,FARMACIA BIENESTAR,320.75,
                ,,,
                TOTAL,,"1,749.25","15,000.00"
                """;
    }

    private void validarContenidoTexto(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("El archivo está vacío");
        }
        if (bytes.length > MAX_TAMANO_BYTES) {
            throw new IllegalArgumentException("El archivo supera el límite máximo permitido de 5 MB");
        }
        int limiteInspeccion = Math.min(bytes.length, 4096);
        for (int i = 0; i < limiteInspeccion; i++) {
            if (bytes[i] == 0) {
                throw new IllegalArgumentException("El archivo no parece ser un CSV de texto plano válido");
            }
        }
    }

    private String decodificarSinBom(byte[] bytes) {
        String texto = new String(bytes, StandardCharsets.UTF_8);
        if (texto.startsWith("\uFEFF")) {
            return texto.substring(1);
        }
        return texto;
    }

    private DateTimeFormatter construirFormatter(String patron) {
        try {
            return DateTimeFormatter.ofPattern(patron);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Formato de fecha inválido: \"" + patron + "\". Ejemplo válido: dd/MM/yyyy");
        }
    }

    private int resolverIndiceColumna(
            String especificacion,
            Map<String, Integer> mapaEncabezados,
            boolean tieneEncabezado,
            String etiquetaCampo
    ) {
        if (especificacion.matches("\\d+")) {
            int numero = Integer.parseInt(especificacion);
            if (numero < 1) {
                throw new IllegalArgumentException("El número de columna para " + etiquetaCampo + " debe ser mayor o igual a 1");
            }
            return numero - 1;
        }
        if (!tieneEncabezado) {
            throw new IllegalArgumentException(
                    "Si el CSV no tiene encabezados, indica el número de columna (1, 2, 3...) para " + etiquetaCampo);
        }
        String clave = HashImportacion.normalizarTexto(especificacion);
        Integer indice = mapaEncabezados.get(clave);
        if (indice == null) {
            throw new IllegalArgumentException(
                    "No se encontró la columna \"" + especificacion + "\" (" + etiquetaCampo + ") en el encabezado del CSV");
        }
        return indice;
    }

    private boolean esFilaVacia(CSVRecord record) {
        for (int i = 0; i < record.size(); i++) {
            if (record.get(i) != null && !record.get(i).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private boolean esFilaDeTotales(String rawFecha, String rawDesc) {
        String f = HashImportacion.normalizarTexto(rawFecha);
        String d = HashImportacion.normalizarTexto(rawDesc);
        return f.startsWith("total") || f.startsWith("subtotal") || f.startsWith("saldo")
                || d.equals("total") || d.startsWith("total ") || d.startsWith("saldo final");
    }

    private String obtenerCelda(CSVRecord record, int indice) {
        if (indice < 0 || indice >= record.size()) {
            return "";
        }
        String valor = record.get(indice);
        return valor == null ? "" : valor.trim();
    }

    private LocalDate intentarParsearFecha(String rawFecha, DateTimeFormatter formatter) {
        if (rawFecha == null || rawFecha.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(rawFecha.trim(), formatter);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private MontoTipo extraerMontoYTipo(CSVRecord record, Integer idxCargo, Integer idxAbono) {
        // Caso 1: misma columna usada para monto con signo (+ ingreso, - gasto)
        if (idxCargo != null && idxCargo.equals(idxAbono)) {
            BigDecimal montoConSigno = parsearMontoConSigno(obtenerCelda(record, idxCargo));
            if (montoConSigno == null || montoConSigno.compareTo(BigDecimal.ZERO) == 0) {
                return null;
            }
            if (montoConSigno.signum() < 0) {
                return new MontoTipo(Dinero.validarPositivo(montoConSigno.abs()), TipoMovimiento.GASTO);
            }
            return new MontoTipo(Dinero.validarPositivo(montoConSigno), TipoMovimiento.INGRESO);
        }

        // Caso 2: columnas separadas de Cargo y Abono
        BigDecimal cargo = idxCargo == null ? null : parsearMontoAbsoluto(obtenerCelda(record, idxCargo));
        BigDecimal abono = idxAbono == null ? null : parsearMontoAbsoluto(obtenerCelda(record, idxAbono));

        if (cargo != null && cargo.compareTo(BigDecimal.ZERO) > 0) {
            return new MontoTipo(Dinero.validarPositivo(cargo), TipoMovimiento.GASTO);
        }
        if (abono != null && abono.compareTo(BigDecimal.ZERO) > 0) {
            return new MontoTipo(Dinero.validarPositivo(abono), TipoMovimiento.INGRESO);
        }
        return null;
    }

    private BigDecimal parsearMontoAbsoluto(String raw) {
        BigDecimal valor = parsearMontoConSigno(raw);
        return valor == null ? null : valor.abs();
    }

    private BigDecimal parsearMontoConSigno(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String limpio = raw.trim()
                .replace("$", "")
                .replace("MXN", "")
                .replace("mxn", "")
                .replace(" ", "")
                .replace(",", "");
        if (limpio.isEmpty() || limpio.equals("-") || limpio.equals("+")) {
            return null;
        }
        try {
            return new BigDecimal(limpio);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Categoria obtenerOCrearSinCategoriaLectura() {
        return categoriaRepository.findByNombreIgnoreCase(NOMBRE_SIN_CATEGORIA)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe la categoría base \"" + NOMBRE_SIN_CATEGORIA + "\" en la base de datos"));
    }

    private void limpiarVistasExpiradas() {
        Instant limite = Instant.now().minus(EXPIRACION_PREVIEW);
        vistasPrevias.entrySet().removeIf(entry -> entry.getValue().creadaEn().isBefore(limite));
        if (vistasPrevias.size() >= MAX_PREVIEWS_EN_MEMORIA) {
            vistasPrevias.clear();
        }
    }

    private record MontoTipo(BigDecimal monto, TipoMovimiento tipo) {
    }

    private record FilaTemporal(
            int numeroLinea,
            LocalDate fecha,
            String descripcion,
            TipoMovimiento tipo,
            BigDecimal monto,
            Long categoriaId,
            String categoriaNombre,
            String reglaAplicada,
            String hash,
            boolean sinCategoria
    ) {
    }
}
