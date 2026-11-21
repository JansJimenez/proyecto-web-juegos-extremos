package com.losvegas.juegosextremos.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.losvegas.juegosextremos.config.DataInitializer;
import com.losvegas.juegosextremos.dto.ItemReservaDTO;
import com.losvegas.juegosextremos.dto.ReservaRequestDTO;
import com.losvegas.juegosextremos.dto.ReservaResponseDTO;
import com.losvegas.juegosextremos.model.Atraccion;
import com.losvegas.juegosextremos.model.ItemReserva;
import com.losvegas.juegosextremos.model.Paquete;
import com.losvegas.juegosextremos.model.Reserva;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Servicio central de Reservas:
 * - Valida y recalcula los precios en el servidor en Soles (S/) a partir de los IDs recibidos.
 * - Persiste la reserva con Soporte Dual (Firestore o Memoria Local en DataInitializer).
 * - Genera la URL oficial de WhatsApp (51953220851) codificada con URLEncoder.encode(mensaje, StandardCharsets.UTF_8).
 */
@Slf4j
@Service
public class ReservaService {

    private static final ZoneId ZONA_PERU = ZoneId.of("America/Lima");
    private static final DateTimeFormatter FORMATO_CODIGO_FECHA = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final Firestore firestore;
    private final DataInitializer dataInitializer;
    private final AtraccionService atraccionService;
    private final PaqueteService paqueteService;

    @Value("${app.parque.whatsapp:51953220851}")
    private String whatsappOficial;

    public ReservaService(
            @Autowired(required = false) Firestore firestore,
            DataInitializer dataInitializer,
            AtraccionService atraccionService,
            PaqueteService paqueteService) {
        this.firestore = firestore;
        this.dataInitializer = dataInitializer;
        this.atraccionService = atraccionService;
        this.paqueteService = paqueteService;
    }

    public ReservaResponseDTO procesarPreReserva(ReservaRequestDTO request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Debes seleccionar al menos un juego extremo o combo para reservar.");
        }

        List<ItemReserva> itemsValidados = new ArrayList<>();
        double montoTotal = 0.0;

        for (ItemReservaDTO itemDto : request.getItems()) {
            int cantidad = itemDto.getCantidad() != null && itemDto.getCantidad() > 0 ? itemDto.getCantidad() : 1;
            String tipo = itemDto.getTipo() != null ? itemDto.getTipo().trim().toUpperCase(Locale.ROOT) : "ATRACCION";

            if ("PAQUETE".equals(tipo) || "COMBO".equals(tipo)) {
                Paquete paquete = paqueteService.obtenerPorId(itemDto.getItemId())
                        .orElseThrow(() -> new IllegalArgumentException("El combo seleccionado no existe: " + itemDto.getItemId()));

                double precioUnitario = paquete.getPrecioOferta() != null ? paquete.getPrecioOferta() : paquete.getPrecioRegularTotal();
                double subtotal = redondearSoles(precioUnitario * cantidad);
                montoTotal += subtotal;

                itemsValidados.add(ItemReserva.builder()
                        .itemId(paquete.getId())
                        .nombre(paquete.getNombre())
                        .tipo("PAQUETE")
                        .cantidad(cantidad)
                        .precioUnitario(precioUnitario)
                        .subtotal(subtotal)
                        .build());
            } else {
                Atraccion atraccion = atraccionService.obtenerPorId(itemDto.getItemId())
                        .orElseThrow(() -> new IllegalArgumentException("La atracción seleccionada no existe: " + itemDto.getItemId()));

                double precioUnitario = atraccion.getPrecio() != null ? atraccion.getPrecio() : 0.0;
                double subtotal = redondearSoles(precioUnitario * cantidad);
                montoTotal += subtotal;

                itemsValidados.add(ItemReserva.builder()
                        .itemId(atraccion.getId())
                        .nombre(atraccion.getNombre())
                        .tipo("ATRACCION")
                        .cantidad(cantidad)
                        .precioUnitario(precioUnitario)
                        .subtotal(subtotal)
                        .build());
            }
        }

        montoTotal = redondearSoles(montoTotal);
        String codigoReserva = generarCodigoReservaUnico();

        Reserva nuevaReserva = Reserva.builder()
                .id(codigoReserva)
                .nombreCliente(request.getNombreCliente().trim())
                .telefonoCliente(request.getTelefonoCliente().trim())
                .origenVisitante(
                        request.getOrigenVisitante() != null && !request.getOrigenVisitante().isBlank()
                                ? request.getOrigenVisitante().trim()
                                : "San Marcos / Cajamarca"
                )
                .fechaVisita(request.getFechaVisita().trim())
                .turnoPreferido(request.getTurnoPreferido().trim())
                .cantidadPersonas(request.getCantidadPersonas())
                .itemsSeleccionados(itemsValidados)
                .montoTotalSoles(montoTotal)
                .comentariosAdicionales(
                        request.getComentariosAdicionales() != null ? request.getComentariosAdicionales().trim() : ""
                )
                .estado("PENDIENTE_WHATSAPP")
                .fechaCreacion(Timestamp.now())
                .build();

        guardarReserva(nuevaReserva);

        String whatsappUrl = construirEnlaceWhatsApp(nuevaReserva);

        return ReservaResponseDTO.builder()
                .exito(true)
                .codigoReserva(codigoReserva)
                .montoTotalSoles(montoTotal)
                .whatsappUrl(whatsappUrl)
                .mensajeConfirmacion("¡Pre-reserva registrada con éxito! Redirigiendo a WhatsApp de Juegos Extremos Los Vegas...")
                .build();
    }

    public void guardarReserva(Reserva reserva) {
        dataInitializer.getReservasMemoria().put(reserva.getId(), reserva);

        if (firestore != null) {
            try {
                firestore.collection(DataInitializer.COLLECTION_RESERVAS)
                        .document(reserva.getId())
                        .set(reserva)
                        .get();
                log.info("Reserva '{}' guardada en Firestore por S/ {}.", reserva.getId(), reserva.getMontoTotalSoles());
            } catch (Exception e) {
                log.error("Error guardando reserva '{}' en Firestore ({}). Conservada en memoria local.",
                        reserva.getId(), e.getMessage());
            }
        } else {
            log.info("[Modo Demo Local] Reserva '{}' registrada en memoria por S/ {}.",
                    reserva.getId(), reserva.getMontoTotalSoles());
        }
    }

    public List<Reserva> listarTodas() {
        if (firestore != null) {
            try {
                List<QueryDocumentSnapshot> docs = firestore
                        .collection(DataInitializer.COLLECTION_RESERVAS)
                        .get()
                        .get()
                        .getDocuments();

                if (!docs.isEmpty()) {
                    List<Reserva> remotas = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : docs) {
                        Reserva r = doc.toObject(Reserva.class);
                        if (r.getId() == null || r.getId().isBlank()) {
                            r.setId(doc.getId());
                        }
                        remotas.add(r);
                        dataInitializer.getReservasMemoria().put(r.getId(), r);
                    }
                    remotas.sort(Comparator.comparing(
                            (Reserva r) -> r.getFechaCreacion() != null ? r.getFechaCreacion().getSeconds() : 0L
                    ).reversed());
                    return remotas;
                }
            } catch (Exception e) {
                log.warn("Fallo al listar reservas desde Firestore ({}). Usando memoria local.", e.getMessage());
            }
        }

        return dataInitializer.getReservasMemoria().values().stream()
                .sorted(Comparator.comparing(
                        (Reserva r) -> r.getFechaCreacion() != null ? r.getFechaCreacion().getSeconds() : 0L
                ).reversed())
                .collect(Collectors.toList());
    }

    public Optional<Reserva> obtenerPorId(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        if (firestore != null) {
            try {
                var snap = firestore.collection(DataInitializer.COLLECTION_RESERVAS)
                        .document(id)
                        .get()
                        .get();
                if (snap.exists()) {
                    Reserva r = snap.toObject(Reserva.class);
                    if (r != null) {
                        dataInitializer.getReservasMemoria().put(r.getId(), r);
                        return Optional.of(r);
                    }
                }
            } catch (Exception e) {
                log.warn("Error consultando reserva '{}' en Firestore: {}", id, e.getMessage());
            }
        }
        return Optional.ofNullable(dataInitializer.getReservasMemoria().get(id));
    }

    public void actualizarEstadoReserva(String id, String nuevoEstado) {
        obtenerPorId(id).ifPresent(reserva -> {
            reserva.setEstado(nuevoEstado);
            guardarReserva(reserva);
        });
    }

    /**
     * Construye el enlace https://wa.me/51953220851?text=... empleando URLEncoder.encode(mensaje, StandardCharsets.UTF_8)
     * para garantizar compatibilidad total con emojis, tildes, S/ y saltos de línea en móvil y WhatsApp Web.
     */
    public String construirEnlaceWhatsApp(Reserva reserva) {
        StringBuilder sb = new StringBuilder();
        sb.append("¡Hola *Juegos Extremos Los Vegas*! 🎢🔥\n");
        sb.append("Quiero confirmar mi pre-reserva web para visitar *Huayobamba (San Marcos)*:\n\n");
        sb.append("📋 *Código:* ").append(reserva.getId()).append("\n");
        sb.append("👤 *Cliente:* ").append(reserva.getNombreCliente()).append("\n");
        sb.append("📱 *Contacto:* ").append(reserva.getTelefonoCliente()).append("\n");
        sb.append("📍 *Procedencia:* ").append(reserva.getOrigenVisitante()).append("\n");
        sb.append("📅 *Fecha de Visita:* ").append(reserva.getFechaVisita()).append("\n");
        sb.append("⏰ *Turno:* ").append(reserva.getTurnoPreferido()).append("\n");
        sb.append("👥 *N° de Personas:* ").append(reserva.getCantidadPersonas()).append("\n\n");
        sb.append("🎯 *Juegos / Combos Elegidos:*\n");

        for (ItemReserva item : reserva.getItemsSeleccionados()) {
            String precioStr = (item.getSubtotal() != null && item.getSubtotal() <= 0.0)
                    ? "GRATIS"
                    : "S/ " + String.format(Locale.US, "%.2f", item.getSubtotal());
            sb.append("• ").append(item.getCantidad()).append("x ")
                    .append(item.getNombre())
                    .append(" (").append(precioStr).append(")\n");
        }

        sb.append("\n💰 *TOTAL ESTIMADO: S/ ")
                .append(String.format(Locale.US, "%.2f", reserva.getMontoTotalSoles()))
                .append("*\n");

        if (reserva.getComentariosAdicionales() != null && !reserva.getComentariosAdicionales().isBlank()) {
            sb.append("📝 *Nota:* ").append(reserva.getComentariosAdicionales()).append("\n");
        }

        sb.append("\n¿Me confirman disponibilidad y datos para Yape/Plin o pago en boletería? ¡Gracias!");

        String encodedText = URLEncoder.encode(sb.toString(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return "https://wa.me/" + whatsappOficial + "?text=" + encodedText;
    }

    private String generarCodigoReservaUnico() {
        String fechaHoy = LocalDate.now(ZONA_PERU).format(FORMATO_CODIGO_FECHA);
        int aleatorio = ThreadLocalRandom.current().nextInt(100, 999);
        return "LV-" + fechaHoy + "-" + aleatorio;
    }

    private double redondearSoles(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
