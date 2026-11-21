package com.losvegas.juegosextremos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa un juego extremo o paquete seleccionado dentro del simulador de reservas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemReservaDTO {

    @NotBlank(message = "El identificador del juego o paquete es obligatorio")
    private String itemId;

    @NotBlank(message = "El tipo de ítem (ATRACCION o PAQUETE) es obligatorio")
    private String tipo;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad mínima por ítem es 1")
    private Integer cantidad;
}
