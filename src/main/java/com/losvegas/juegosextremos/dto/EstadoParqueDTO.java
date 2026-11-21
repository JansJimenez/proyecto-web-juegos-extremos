package com.losvegas.juegosextremos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para actualizar en 1 clic el estado climático/operativo del parque en Huayobamba desde el Backoffice.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoParqueDTO {

    @NotBlank(message = "El estado operativo es obligatorio")
    private String estadoOperativo;

    private String mensajeBanner;
    private String temperaturaActual;
    private Boolean promoFinDeSemanaActiva;
    private String textoPromoFinDeSemana;
}
