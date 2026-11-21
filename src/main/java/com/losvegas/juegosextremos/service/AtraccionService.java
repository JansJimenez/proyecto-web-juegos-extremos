package com.losvegas.juegosextremos.service;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.losvegas.juegosextremos.config.DataInitializer;
import com.losvegas.juegosextremos.model.Atraccion;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servicio de negocio para gestionar la colección 'atracciones'.
 * Implementa Soporte Dual: opera sobre Firebase Firestore si está conectado,
 * o sobre la memoria local de DataInitializer en modo Demo Local sin lanzar NullPointerException.
 */
@Slf4j
@Service
public class AtraccionService {

    private final Firestore firestore;
    private final DataInitializer dataInitializer;

    public AtraccionService(
            @Autowired(required = false) Firestore firestore,
            DataInitializer dataInitializer) {
        this.firestore = firestore;
        this.dataInitializer = dataInitializer;
    }

    public List<Atraccion> listarActivas() {
        return listarTodas().stream()
                .filter(a -> Boolean.TRUE.equals(a.getActivo()))
                .collect(Collectors.toList());
    }

    public List<Atraccion> listarTodas() {
        if (firestore != null) {
            try {
                List<QueryDocumentSnapshot> docs = firestore
                        .collection(DataInitializer.COLLECTION_ATRACCIONES)
                        .get()
                        .get()
                        .getDocuments();

                if (!docs.isEmpty()) {
                    List<Atraccion> listaRemota = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : docs) {
                        Atraccion atraccion = doc.toObject(Atraccion.class);
                        if (atraccion.getId() == null || atraccion.getId().isBlank()) {
                            atraccion.setId(doc.getId());
                        }
                        if ("tirolesa-extrema".equals(atraccion.getId())
                                && "/images/aventurero-seguridad.jpg".equals(atraccion.getImagenUrl())) {
                            atraccion.setImagenUrl("/images/tirolesa-extrema.jpg");
                            firestore.collection(DataInitializer.COLLECTION_ATRACCIONES)
                                    .document(atraccion.getId())
                                    .update("imagenUrl", "/images/tirolesa-extrema.jpg");
                        }
                        listaRemota.add(atraccion);
                        dataInitializer.getAtraccionesMemoria().put(atraccion.getId(), atraccion);
                    }
                    listaRemota.sort(Comparator.comparingInt(a -> a.getOrden() != null ? a.getOrden() : 999));
                    return listaRemota;
                }
            } catch (Exception e) {
                log.warn("Fallo al consultar 'atracciones' en Firestore ({}). Usando respaldo en memoria.", e.getMessage());
            }
        }

        return dataInitializer.getAtraccionesMemoria().values().stream()
                .sorted(Comparator.comparingInt(a -> a.getOrden() != null ? a.getOrden() : 999))
                .collect(Collectors.toList());
    }

    public Optional<Atraccion> obtenerPorId(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        if (firestore != null) {
            try {
                var snap = firestore.collection(DataInitializer.COLLECTION_ATRACCIONES)
                        .document(id)
                        .get()
                        .get();
                if (snap.exists()) {
                    Atraccion atraccion = snap.toObject(Atraccion.class);
                    if (atraccion != null) {
                        if (atraccion.getId() == null || atraccion.getId().isBlank()) {
                            atraccion.setId(snap.getId());
                        }
                        dataInitializer.getAtraccionesMemoria().put(atraccion.getId(), atraccion);
                        return Optional.of(atraccion);
                    }
                }
            } catch (Exception e) {
                log.warn("Error leyendo atracción '{}' desde Firestore ({}). Consultando memoria.", id, e.getMessage());
            }
        }

        return Optional.ofNullable(dataInitializer.getAtraccionesMemoria().get(id));
    }

    public Atraccion guardar(Atraccion atraccion) {
        if (atraccion.getId() == null || atraccion.getId().isBlank()) {
            atraccion.setId(generarSlugId(atraccion.getNombre()));
        }
        if (atraccion.getActivo() == null) {
            atraccion.setActivo(true);
        }
        if (atraccion.getOrden() == null) {
            atraccion.setOrden(dataInitializer.getAtraccionesMemoria().size() + 1);
        }
        if (atraccion.getNivelAdrenalina() == null || atraccion.getNivelAdrenalina().isBlank()) {
            atraccion.setNivelAdrenalina("ALTO");
        }
        if (atraccion.getIconoBadge() == null || atraccion.getIconoBadge().isBlank()) {
            atraccion.setIconoBadge("fa-mountain");
        }
        if (atraccion.getIncluyeEquipo() == null || atraccion.getIncluyeEquipo().isEmpty()) {
            atraccion.setIncluyeEquipo(List.of("Arnés certificado", "Casco de seguridad", "Instructor guía"));
        }

        // Guardar en memoria local siempre para respuesta inmediata
        dataInitializer.getAtraccionesMemoria().put(atraccion.getId(), atraccion);

        // Persistir en Firestore si está conectado
        if (firestore != null) {
            try {
                firestore.collection(DataInitializer.COLLECTION_ATRACCIONES)
                        .document(atraccion.getId())
                        .set(atraccion)
                        .get();
                log.info("Atracción '{}' guardada en Firestore.", atraccion.getId());
            } catch (Exception e) {
                log.error("Error guardando atracción '{}' en Firestore: {}", atraccion.getId(), e.getMessage());
            }
        }

        return atraccion;
    }

    public void cambiarEstadoActivo(String id, boolean activo) {
        obtenerPorId(id).ifPresent(atraccion -> {
            atraccion.setActivo(activo);
            guardar(atraccion);
        });
    }

    public void eliminar(String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        dataInitializer.getAtraccionesMemoria().remove(id);
        if (firestore != null) {
            try {
                firestore.collection(DataInitializer.COLLECTION_ATRACCIONES)
                        .document(id)
                        .delete()
                        .get();
                log.info("Atracción '{}' eliminada de Firestore.", id);
            } catch (Exception e) {
                log.error("Error eliminando atracción '{}' de Firestore: {}", id, e.getMessage());
            }
        }
    }

    private String generarSlugId(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "juego-" + UUID.randomUUID().toString().substring(0, 6);
        }
        String normalizado = Normalizer.normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return normalizado.isBlank() ? "juego-" + UUID.randomUUID().toString().substring(0, 6) : normalizado;
    }
}
