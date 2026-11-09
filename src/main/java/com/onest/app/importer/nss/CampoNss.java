package com.onest.app.importer.nss;

import java.util.List;

/**
 * Una columna de un layout: como se llama en el Excel, a que columna de la tabla va y como se
 * valida.
 *
 * @param encabezado        texto del encabezado en el layout (se compara normalizado: sin acentos,
 *                          mayusculas, espacios simples)
 * @param columna           columna destino en SERV_MED_XLS_*
 * @param tipo              como se interpreta la celda
 * @param maximo            largo maximo para TEXTO (en caracteres)
 * @param catalogo          nombre del catalogo cerrado para CATALOGO (ver CatalogosNss)
 * @param obligatorio       si la celda vacia es un error
 * @param negativoConAviso  solo DECIMAL: un negativo entra tal cual y se reporta como aviso, en
 *                          vez de rechazar el renglon (decision del 12-oct-2026 para el COSTO de
 *                          accidentes)
 * @param alias             otro encabezado que se acepta para la misma columna (null si no hay).
 *                          Para plantillas que traen un error de dedo que despues se puede corregir:
 *                          el archivo se reconoce con cualquiera de las dos formas.
 * @param puedeFaltar       columna agregada despues a la plantilla: si el archivo no la trae (una
 *                          version anterior), el layout se reconoce igual y la columna entra vacia
 */
public record CampoNss(String encabezado, String columna, Tipo tipo, int maximo, String catalogo,
                       boolean obligatorio, boolean negativoConAviso, String alias, boolean puedeFaltar) {

    CampoNss(String encabezado, String columna, Tipo tipo, int maximo, String catalogo,
             boolean obligatorio, boolean negativoConAviso, String alias) {
        this(encabezado, columna, tipo, maximo, catalogo, obligatorio, negativoConAviso, alias, false);
    }

    /**
     * Entero opcional que se agrego a la plantilla despues de que ya habia archivos capturados
     * (15-oct-2026: SEMANA y MES GESTACION en maternidad). Los archivos viejos se siguen reconociendo.
     */
    static CampoNss enteroNuevo(String encabezado, String columna) {
        return new CampoNss(encabezado, columna, Tipo.ENTERO, 0, null, false, false, null, true);
    }

    /**
     * MARCA (13-oct-2026): casilla 1 / 0 / vacia, como las sustancias de antidoping (una columna por
     * sustancia, varias pueden venir en 1). No tiene columna propia en la tabla: ValidadorNss junta
     * las marcadas en una columna derivada (SUSTANCIA = "AMF, MET, THC").
     */
    public enum Tipo { NSS, TEXTO, FECHA, ENTERO, DECIMAL, CATALOGO, MARCA }

    static CampoNss marca(String encabezado) {
        return new CampoNss(encabezado, null, Tipo.MARCA, 0, null, false, false, null);
    }

    /** El encabezado oficial y, si hay, los alternos ({@code alias} admite varios separados por "|"). */
    public List<String> encabezadosAceptados() {
        if (alias == null) {
            return List.of(encabezado);
        }
        List<String> todos = new java.util.ArrayList<>();
        todos.add(encabezado);
        todos.addAll(List.of(alias.split("\\|")));
        return List.copyOf(todos);
    }

    static CampoNss nss() {
        return new CampoNss("NSS", "NSS", Tipo.NSS, 13, null, true, false, null);
    }

    static CampoNss texto(String encabezado, String columna, int maximo) {
        return new CampoNss(encabezado, columna, Tipo.TEXTO, maximo, null, false, false, null);
    }

    static CampoNss textoObligatorio(String encabezado, String columna, int maximo) {
        return new CampoNss(encabezado, columna, Tipo.TEXTO, maximo, null, true, false, null);
    }

    static CampoNss entero(String encabezado, String columna, boolean obligatorio) {
        return new CampoNss(encabezado, columna, Tipo.ENTERO, 0, null, obligatorio, false, null);
    }

    static CampoNss fecha(String encabezado, String columna, boolean obligatorio) {
        return new CampoNss(encabezado, columna, Tipo.FECHA, 0, null, obligatorio, false, null);
    }

    /** Fecha cuyo encabezado tambien se acepta escrito de otra forma (ver {@code alias}). */
    static CampoNss fecha(String encabezado, String alias, String columna, boolean obligatorio) {
        return new CampoNss(encabezado, columna, Tipo.FECHA, 0, null, obligatorio, false, alias);
    }

    static CampoNss entero(String encabezado, String columna) {
        return new CampoNss(encabezado, columna, Tipo.ENTERO, 0, null, false, false, null);
    }

    static CampoNss decimal(String encabezado, String columna) {
        return new CampoNss(encabezado, columna, Tipo.DECIMAL, 0, null, false, false, null);
    }

    /** Monto que puede venir negativo en el Excel del servicio medico: entra y se avisa. */
    static CampoNss decimalConSigno(String encabezado, String columna) {
        return new CampoNss(encabezado, columna, Tipo.DECIMAL, 0, null, false, true, null);
    }

    static CampoNss catalogo(String encabezado, String columna, String catalogo, boolean obligatorio) {
        return new CampoNss(encabezado, columna, Tipo.CATALOGO, 0, catalogo, obligatorio, false, null);
    }
}
