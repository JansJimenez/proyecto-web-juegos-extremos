package com.losvegas.juegosextremos.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad mapeada al documento único 'estado_parque' de la colección 'configuracion'.
 * Permite controlar en tiempo real el banner de clima y promociones en Huayobamba.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionParque {

    private String id;
    /** Valores: "ABIERTO_SOLEADO", "ABIERTO_NUBLADO", "SUSPENDIDO_LLUVIA", "MANTENIMIENTO" */
    private String estadoOperativo;
    /** Valores: "GREEN", "AMBER", "RED" */
    private String colorIndicador;
    private String mensajeBanner;
    private String temperaturaActual;
    private String horarioAtencion;
    private Boolean promoFinDeSemanaActiva;
    private String textoPromoFinDeSemana;
    private String whatsappOficial;
    private String ultimaActualizacion;
}
