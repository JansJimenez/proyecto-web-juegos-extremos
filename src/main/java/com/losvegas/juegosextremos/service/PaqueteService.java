package com.losvegas.juegosextremos.service;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.losvegas.juegosextremos.config.DataInitializer;
import com.losvegas.juegosextremos.model.Paquete;
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
 * Servicio de negocio para gestionar la colección 'paquetes' (combos promocionales).
 * Implementa Soporte Dual (Firestore + Fallback en memoria local).
 */
@Slf4j
@Service
public class PaqueteService {

    private final Firestore firestore;
    private final DataInitializer dataInitializer;

    public PaqueteService(
            @Autowired(required = false) Firestore firestore,
            DataInitializer dataInitializer) {
        this.firestore = firestore;
        this.dataInitializer = dataInitializer;
    }

    public List<Paquete> listarActivos() {
        return listarTodos().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .collect(Collectors.toList());
    }

    public List<Paquete> listarTodos() {
        if (firestore != null) {
            try {
                List<QueryDocumentSnapshot> docs = firestore
                        .collection(DataInitializer.COLLECTION_PAQUETES)
                        .get()
                        .get()
                        .getDocuments();

                if (!docs.isEmpty()) {
                    List<Paquete> listaRemota = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : docs) {
                        Paquete paquete = doc.toObject(Paquete.class);
                        if (paquete.getId() == null || paquete.getId().isBlank()) {
                            paquete.setId(doc.getId());
                        }
                        listaRemota.add(paquete);
                        dataInitializer.getPaquetesMemoria().put(paquete.getId(), paquete);
                    }
                    listaRemota.sort(Comparator.comparingInt(p -> p.getOrden() != null ? p.getOrden() : 999));
                    return listaRemota;
                }
            } catch (Exception e) {
                log.warn("Fallo al consultar 'paquetes' en Firestore ({}). Usando respaldo en memoria.", e.getMessage());
            }
        }

        return dataInitializer.getPaquetesMemoria().values().stream()
                .sorted(Comparator.comparingInt(p -> p.getOrden() != null ? p.getOrden() : 999))
                .collect(Collectors.toList());
    }

    public Optional<Paquete> obtenerPorId(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        if (firestore != null) {
            try {
                var snap = firestore.collection(DataInitializer.COLLECTION_PAQUETES)
                        .document(id)
                        .get()
                        .get();
                if (snap.exists()) {
                    Paquete paquete = snap.toObject(Paquete.class);
                    if (paquete != null) {
                        if (paquete.getId() == null || paquete.getId().isBlank()) {
                            paquete.setId(snap.getId());
                        }
                        dataInitializer.getPaquetesMemoria().put(paquete.getId(), paquete);
                        return Optional.of(paquete);
                    }
                }
            } catch (Exception e) {
                log.warn("Error leyendo paquete '{}' desde Firestore ({}). Consultando memoria.", id, e.getMessage());
            }
        }

        return Optional.ofNullable(dataInitializer.getPaquetesMemoria().get(id));
    }

    public Paquete guardar(Paquete paquete) {
        if (paquete.getId() == null || paquete.getId().isBlank()) {
            paquete.setId(generarSlugId(paquete.getNombre()));
        }
        if (paquete.getActivo() == null) {
            paquete.setActivo(true);
        }
        if (paquete.getOrden() == null) {
            paquete.setOrden(dataInitializer.getPaquetesMemoria().size() + 1);
        }
        if (paquete.getMinimoPersonas() == null || paquete.getMinimoPersonas() < 1) {
            paquete.setMinimoPersonas(1);
        }

        double regular = paquete.getPrecioRegularTotal() != null ? paquete.getPrecioRegularTotal() : 0.0;
        double oferta = paquete.getPrecioOferta() != null ? paquete.getPrecioOferta() : regular;
        paquete.setAhorroSoles(Math.max(0.0, regular - oferta));

        dataInitializer.getPaquetesMemoria().put(paquete.getId(), paquete);

        if (firestore != null) {
            try {
                firestore.collection(DataInitializer.COLLECTION_PAQUETES)
                        .document(paquete.getId())
                        .set(paquete)
                        .get();
                log.info("Paquete '{}' guardado en Firestore.", paquete.getId());
            } catch (Exception e) {
                log.error("Error guardando paquete '{}' en Firestore: {}", paquete.getId(), e.getMessage());
            }
        }

        return paquete;
    }

    public void cambiarEstadoActivo(String id, boolean activo) {
        obtenerPorId(id).ifPresent(paquete -> {
            paquete.setActivo(activo);
            guardar(paquete);
        });
    }

    public void eliminar(String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        dataInitializer.getPaquetesMemoria().remove(id);
        if (firestore != null) {
            try {
                firestore.collection(DataInitializer.COLLECTION_PAQUETES)
                        .document(id)
                        .delete()
                        .get();
                log.info("Paquete '{}' eliminado de Firestore.", id);
            } catch (Exception e) {
                log.error("Error eliminando paquete '{}' de Firestore: {}", id, e.getMessage());
            }
        }
    }

    private String generarSlugId(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "combo-" + UUID.randomUUID().toString().substring(0, 6);
        }
        String normalizado = Normalizer.normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return normalizado.isBlank() ? "combo-" + UUID.randomUUID().toString().substring(0, 6) : normalizado;
    }
}
