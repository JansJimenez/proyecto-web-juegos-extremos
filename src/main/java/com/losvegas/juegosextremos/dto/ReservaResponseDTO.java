package com.losvegas.juegosextremos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta devuelta al frontend tras registrar la pre-reserva en Firestore (o memoria local).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservaResponseDTO {

    private boolean exito;
    private String codigoReserva;
    private Double montoTotalSoles;
    private String whatsappUrl;
    private String mensajeConfirmacion;
}
