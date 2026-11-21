package com.losvegas.juegosextremos.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.losvegas.juegosextremos.config.DataInitializer;
import com.losvegas.juegosextremos.dto.EstadoParqueDTO;
import com.losvegas.juegosextremos.model.ConfiguracionParque;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servicio para controlar en tiempo real el banner de estado climático y operativo
 * del parque en Huayobamba (documento único 'configuracion/estado_parque').
 * Implementa Soporte Dual (Firestore + Fallback en memoria local).
 */
@Slf4j
@Service
public class ConfiguracionService {

    private static final ZoneId ZONA_PERU = ZoneId.of("America/Lima");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a", Locale.forLanguageTag("es-PE"));

    private final Firestore firestore;
    private final DataInitializer dataInitializer;

    public ConfiguracionService(
            @Autowired(required = false) Firestore firestore,
            DataInitializer dataInitializer) {
        this.firestore = firestore;
        this.dataInitializer = dataInitializer;
    }

    public ConfiguracionParque obtenerConfiguracionActual() {
        if (firestore != null) {
            try {
                DocumentSnapshot snap = firestore
                        .collection(DataInitializer.COLLECTION_CONFIGURACION)
                        .document(DataInitializer.DOC_ESTADO_PARQUE)
                        .get()
                        .get();
                if (snap.exists()) {
                    ConfiguracionParque remota = snap.toObject(ConfiguracionParque.class);
                    if (remota != null) {
                        dataInitializer.setConfiguracionMemoria(remota);
                        return remota;
                    }
                }
            } catch (Exception e) {
                log.warn("No se pudo leer 'configuracion/estado_parque' de Firestore ({}). Usando estado en memoria.", e.getMessage());
            }
        }
        return dataInitializer.getConfiguracionMemoria();
    }

    /**
     * Acción rápida de 1 clic desde el panel administrativo para cambiar el clima/estado del parque en Huayobamba.
     */
    public ConfiguracionParque actualizarEstadoRapido(String estadoOperativo) {
        ConfiguracionParque actual = obtenerConfiguracionActual();

        switch (estadoOperativo != null ? estadoOperativo.toUpperCase(Locale.ROOT) : "ABIERTO_SOLEADO") {
            case "ABIERTO_SOLEADO" -> {
                actual.setEstadoOperativo("ABIERTO_SOLEADO");
                actual.setColorIndicador("GREEN");
                actual.setTemperaturaActual("Clima Soleado Ideal");
                actual.setMensajeBanner("¡Parque Abierto en Huayobamba! Clima soleado ideal para disfrutar la Tirolesa y el Puente Extremo.");
            }
            case "ABIERTO_NUBLADO" -> {
                actual.setEstadoOperativo("ABIERTO_NUBLADO");
                actual.setColorIndicador("AMBER");
                actual.setTemperaturaActual("Templado / Nublado");
                actual.setMensajeBanner("Parque Abierto en Huayobamba • Clima fresco y agradable, Tirolesa y Puente Extremo operativos al 100%.");
            }
            case "SUSPENDIDO_LLUVIA" -> {
                actual.setEstadoOperativo("SUSPENDIDO_LLUVIA");
                actual.setColorIndicador("RED");
                actual.setTemperaturaActual("Lluvia en el valle");
                actual.setMensajeBanner("Atención: Actividades suspendidas temporalmente por lluvia en Huayobamba por tu seguridad. Escríbenos para reprogramar.");
            }
            case "MANTENIMIENTO" -> {
                actual.setEstadoOperativo("MANTENIMIENTO");
                actual.setColorIndicador("RED");
                actual.setTemperaturaActual("Inspección Técnica");
                actual.setMensajeBanner("Hoy realizamos inspección preventiva de cables y arneses. ¡Reserva desde ya para este fin de semana!");
            }
            default -> {
                actual.setEstadoOperativo("ABIERTO_SOLEADO");
                actual.setColorIndicador("GREEN");
            }
        }

        actual.setUltimaActualizacion("Actualizado: " + ZonedDateTime.now(ZONA_PERU).format(FORMATO_HORA));
        return persistirConfiguracion(actual);
    }

    /**
     * Actualización personalizada de textos del banner, temperatura y promoción de fin de semana.
     */
    public ConfiguracionParque actualizarConfiguracionPersonalizada(EstadoParqueDTO dto) {
        ConfiguracionParque actual = obtenerConfiguracionActual();

        if (dto.getEstadoOperativo() != null && !dto.getEstadoOperativo().isBlank()) {
            actual.setEstadoOperativo(dto.getEstadoOperativo());
            actual.setColorIndicador(resolverColorIndicador(dto.getEstadoOperativo()));
        }
        if (dto.getMensajeBanner() != null && !dto.getMensajeBanner().isBlank()) {
            actual.setMensajeBanner(dto.getMensajeBanner().trim());
        }
        if (dto.getTemperaturaActual() != null && !dto.getTemperaturaActual().isBlank()) {
            actual.setTemperaturaActual(dto.getTemperaturaActual().trim());
        }
        if (dto.getPromoFinDeSemanaActiva() != null) {
            actual.setPromoFinDeSemanaActiva(dto.getPromoFinDeSemanaActiva());
        }
        if (dto.getTextoPromoFinDeSemana() != null) {
            actual.setTextoPromoFinDeSemana(dto.getTextoPromoFinDeSemana().trim());
        }

        actual.setUltimaActualizacion("Actualizado: " + ZonedDateTime.now(ZONA_PERU).format(FORMATO_HORA));
        return persistirConfiguracion(actual);
    }

    private String resolverColorIndicador(String estadoOperativo) {
        if ("SUSPENDIDO_LLUVIA".equalsIgnoreCase(estadoOperativo) || "MANTENIMIENTO".equalsIgnoreCase(estadoOperativo)) {
            return "RED";
        }
        if ("ABIERTO_NUBLADO".equalsIgnoreCase(estadoOperativo)) {
            return "AMBER";
        }
        return "GREEN";
    }

    private ConfiguracionParque persistirConfiguracion(ConfiguracionParque config) {
        dataInitializer.setConfiguracionMemoria(config);

        if (firestore != null) {
            try {
                firestore.collection(DataInitializer.COLLECTION_CONFIGURACION)
                        .document(DataInitializer.DOC_ESTADO_PARQUE)
                        .set(config)
                        .get();
                log.info("Configuración del parque actualizada en Firestore: {}", config.getEstadoOperativo());
            } catch (Exception e) {
                log.error("Error guardando configuración del parque en Firestore: {}", e.getMessage());
            }
        }
        return config;
    }
}
