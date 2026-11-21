package com.losvegas.juegosextremos.model;

import com.google.cloud.Timestamp;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad mapeada a la colección 'reservas' en Firebase Firestore.
 * Nota: Se evita java.time.LocalDate directo para compatibilidad 100% con el POJO mapper de Firestore;
 * se emplea String en formato ISO YYYY-MM-DD para fechaVisita y com.google.cloud.Timestamp para fechaCreacion.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reserva {

    private String id;
    private String nombreCliente;
    private String telefonoCliente;
    private String origenVisitante;
    /** Fecha de visita en formato ISO YYYY-MM-DD */
    private String fechaVisita;
    /** Ej. "MAÑANA (9:00 AM - 1:00 PM)" o "TARDE (1:00 PM - 5:30 PM)" */
    private String turnoPreferido;
    private Integer cantidadPersonas;

    @Builder.Default
    private List<ItemReserva> itemsSeleccionados = new ArrayList<>();

    private Double montoTotalSoles;
    private String comentariosAdicionales;
    /** Valores: "PENDIENTE_WHATSAPP", "CONFIRMADA", "COMPLETADA", "CANCELADA" */
    private String estado;
    private Timestamp fechaCreacion;
}
