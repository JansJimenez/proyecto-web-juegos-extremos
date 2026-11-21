package com.losvegas.juegosextremos.model;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad mapeada a la colección 'atracciones' en Firebase Firestore.
 * Utiliza exclusivamente tipos nativos soportados por el serializador de Firestore.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Atraccion {

    private String id;
    private String nombre;
    private String descripcionCorta;
    private String descripcionDetallada;
    private Double precio;
    /** Valores esperados: "MEDIO", "ALTO", "EXTREMO" */
    private String nivelAdrenalina;
    private Integer alturaMetros;
    private Integer pesoMinKg;
    private Integer pesoMaxKg;
    private Integer edadMinima;
    private Integer duracionEstimadaMin;
    private String imagenUrl;
    private String iconoBadge;

    @Builder.Default
    private List<String> incluyeEquipo = new ArrayList<>();

    private Integer orden;
    private Boolean activo;
}
