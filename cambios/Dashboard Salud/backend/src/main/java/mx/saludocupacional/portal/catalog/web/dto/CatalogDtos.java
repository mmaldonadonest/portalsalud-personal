package mx.saludocupacional.portal.catalog.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Objetos de entrada y salida de los catálogos maestros. */
public final class CatalogDtos {

    private CatalogDtos() {
    }

    /** Elemento de catálogo tal como lo consume la interfaz. */
    public record CatalogItem(
            Long id,
            String codigo,
            String nombre,
            boolean activo,
            Integer orden
    ) {
    }

    /** Alta o edición de un elemento de catálogo. */
    public record CatalogRequest(
            @Size(max = 40, message = "El código admite hasta 40 caracteres")
            String codigo,

            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 160, message = "El nombre admite hasta 160 caracteres")
            String nombre,

            Boolean activo,
            Integer orden
    ) {
    }

    /** Predio con sus nombres alternativos. */
    public record PredioItem(
            Long id,
            String codigo,
            String nombre,
            boolean activo,
            Integer orden,
            List<String> aliases
    ) {
    }

    /** Causa de atención, que además pertenece a una categoría. */
    public record AttentionCauseItem(
            Long id,
            String codigo,
            String nombre,
            String categoria,
            boolean activo,
            Integer orden
    ) {
    }

    /** Tipo de lesión, que se ubica en una región corporal. */
    public record InjuryTypeItem(
            Long id,
            String codigo,
            String nombre,
            String regionCorporal,
            boolean activo,
            Integer orden
    ) {
    }

    /**
     * Todos los catálogos en una sola respuesta.
     *
     * <p>Los formularios necesitan poblar entre cinco y ocho listas
     * desplegables; entregarlas juntas evita una ráfaga de peticiones al abrir
     * cada pantalla.
     */
    public record CatalogosResponse(
            List<PredioItem> predios,
            List<CatalogItem> cuentas,
            List<CatalogItem> agencias,
            List<CatalogItem> areas,
            List<CatalogItem> puestos,
            List<AttentionCauseItem> causasAtencion,
            List<InjuryTypeItem> tiposLesion,
            List<CatalogItem> tiposExamen,
            List<CatalogItem> resultadosExamen,
            List<CatalogItem> tiposIncapacidad,
            List<CatalogItem> tiposAccidente,
            List<CatalogItem> causasAccidente,
            List<CatalogItem> estatusAccidente,
            List<CatalogItem> tiposPrueba,
            List<CatalogItem> resultadosPrueba,
            List<CatalogItem> estatusPrueba
    ) {
    }
}
