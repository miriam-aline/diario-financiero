package com.diariofinanciero.web;

import com.diariofinanciero.service.ConfiguracionCsv;

public class ImportacionForm {

    private Long cuentaId;
    private String columnaFecha = "Fecha";
    private String columnaDescripcion = "Descripción";
    private String columnaCargo = "Cargo";
    private String columnaAbono = "Abono";
    private String formatoFecha = "dd/MM/yyyy";
    private String separador = ",";
    private boolean primeraFilaEsEncabezado = true;

    public ConfiguracionCsv aConfiguracion() {
        char sep = switch (separador == null ? "," : separador) {
            case ";" -> ';';
            case "\\t", "TAB" -> '\t';
            case "|" -> '|';
            default -> ',';
        };
        return new ConfiguracionCsv(
                cuentaId,
                columnaFecha,
                columnaDescripcion,
                columnaCargo,
                columnaAbono,
                formatoFecha,
                sep,
                primeraFilaEsEncabezado
        );
    }

    public Long getCuentaId() {
        return cuentaId;
    }

    public void setCuentaId(Long cuentaId) {
        this.cuentaId = cuentaId;
    }

    public String getColumnaFecha() {
        return columnaFecha;
    }

    public void setColumnaFecha(String columnaFecha) {
        this.columnaFecha = columnaFecha;
    }

    public String getColumnaDescripcion() {
        return columnaDescripcion;
    }

    public void setColumnaDescripcion(String columnaDescripcion) {
        this.columnaDescripcion = columnaDescripcion;
    }

    public String getColumnaCargo() {
        return columnaCargo;
    }

    public void setColumnaCargo(String columnaCargo) {
        this.columnaCargo = columnaCargo;
    }

    public String getColumnaAbono() {
        return columnaAbono;
    }

    public void setColumnaAbono(String columnaAbono) {
        this.columnaAbono = columnaAbono;
    }

    public String getFormatoFecha() {
        return formatoFecha;
    }

    public void setFormatoFecha(String formatoFecha) {
        this.formatoFecha = formatoFecha;
    }

    public String getSeparador() {
        return separador;
    }

    public void setSeparador(String separador) {
        this.separador = separador;
    }

    public boolean isPrimeraFilaEsEncabezado() {
        return primeraFilaEsEncabezado;
    }

    public void setPrimeraFilaEsEncabezado(boolean primeraFilaEsEncabezado) {
        this.primeraFilaEsEncabezado = primeraFilaEsEncabezado;
    }
}
