package com.losvegas.juegosextremos.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sub-documento embebido dentro de una Reserva para detallar cada juego o combo seleccionado.
 * Compatible con la serialización nativa de Firestore (@NoArgsConstructor).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemReserva {

    private String itemId;
    private String nombre;
    /** "ATRACCION" o "PAQUETE" */
    private String tipo;
    private Integer cantidad;
    private Double precioUnitario;
    private Double subtotal;
}
