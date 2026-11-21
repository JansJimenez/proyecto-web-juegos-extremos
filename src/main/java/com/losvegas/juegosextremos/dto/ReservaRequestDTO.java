package com.losvegas.juegosextremos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload enviado por el Simulador Inteligente (POST /api/reservas).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservaRequestDTO {

    @NotBlank(message = "Por favor ingresa tu nombre completo")
    private String nombreCliente;

    @NotBlank(message = "Por favor ingresa tu número de celular o WhatsApp")
    @Pattern(regexp = "^[0-9+\\s()-]{7,15}$", message = "Ingresa un número de teléfono válido")
    private String telefonoCliente;

    private String origenVisitante;

    @NotBlank(message = "Selecciona la fecha de tu visita a Huayobamba")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "La fecha debe tener el formato YYYY-MM-DD")
    private String fechaVisita;

    @NotBlank(message = "Selecciona tu turno preferido")
    private String turnoPreferido;

    @NotNull(message = "Indica la cantidad de personas")
    @Min(value = 1, message = "Debe asistir al menos 1 persona")
    private Integer cantidadPersonas;

    @NotEmpty(message = "Selecciona al menos un juego extremo o combo para tu reserva")
    @Valid
    private List<ItemReservaDTO> items;

    private String comentariosAdicionales;
}
