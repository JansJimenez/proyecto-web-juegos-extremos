package com.losvegas.juegosextremos.model;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad mapeada a la colección 'paquetes' en Firebase Firestore.
 * Representa combos promocionales (individuales, dúos, familiares, delegaciones).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Paquete {

    private String id;
    private String nombre;
    private String descripcion;
    /** Valores esperados: "INDIVIDUAL", "DUO", "FAMILIAR", "ESCOLAR_DELEGACION" */
    private String tipoPublico;

    @Builder.Default
    private List<String> atraccionesIncluidasIds = new ArrayList<>();

    @Builder.Default
    private List<String> atraccionesIncluidasNombres = new ArrayList<>();

    private Double precioRegularTotal;
    private Double precioOferta;
    private Double ahorroSoles;
    private Integer minimoPersonas;
    private String etiquetaDestacada;
    private Integer orden;
    private Boolean activo;
}
