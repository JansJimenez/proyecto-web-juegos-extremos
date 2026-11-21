package com.losvegas.juegosextremos.config;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.losvegas.juegosextremos.model.Atraccion;
import com.losvegas.juegosextremos.model.ConfiguracionParque;
import com.losvegas.juegosextremos.model.Paquete;
import com.losvegas.juegosextremos.model.Reserva;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Inicializador de datos semilla para "Juegos Extremos Los Vegas" (Huayobamba, San Marcos - Cajamarca).
 * Incluye las 3 atracciones solicitadas a S/ 10.00:
 * 1) Puente Extremo 'Los Vegas' (S/ 10.00)
 * 2) Tirolesa Extrema (S/ 10.00)
 * 3) Campo de Voleyball (S/ 10.00)
 * Horario oficial: 10:30 AM a 6:30 PM | Contacto: 953 220 851
 */
@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    public static final String COLLECTION_ATRACCIONES = "atracciones";
    public static final String COLLECTION_PAQUETES = "paquetes";
    public static final String COLLECTION_RESERVAS = "reservas";
    public static final String COLLECTION_CONFIGURACION = "configuracion";
    public static final String DOC_ESTADO_PARQUE = "estado_parque";

    private final Firestore firestore;

    @Getter
    private final Map<String, Atraccion> atraccionesMemoria = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, Paquete> paquetesMemoria = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, Reserva> reservasMemoria = new ConcurrentHashMap<>();

    @Getter
    private volatile ConfiguracionParque configuracionMemoria;

    public DataInitializer(@Autowired(required = false) Firestore firestore) {
        this.firestore = firestore;
    }

    public void setConfiguracionMemoria(ConfiguracionParque configuracionMemoria) {
        this.configuracionMemoria = configuracionMemoria;
    }

    @Override
    public void run(String... args) {
        cargarSemillaEnMemoria();

        if (firestore != null) {
            sincronizarSemillaConFirestore();
        }
    }

    private void cargarSemillaEnMemoria() {
        atraccionesMemoria.clear();
        List<Atraccion> atraccionesIniciales = construirAtraccionesSemilla();
        for (Atraccion a : atraccionesIniciales) {
            atraccionesMemoria.put(a.getId(), a);
        }

        paquetesMemoria.clear();
        List<Paquete> paquetesIniciales = construirPaquetesSemilla();
        for (Paquete p : paquetesIniciales) {
            paquetesMemoria.put(p.getId(), p);
        }

        this.configuracionMemoria = construirConfiguracionSemilla();
        log.info("Catálogo oficial cargado: {} atracciones a S/ 10.00 (Puente Extremo, Tirolesa y Campo de Voleyball).",
                atraccionesMemoria.size());
    }

    private void sincronizarSemillaConFirestore() {
        try {
            // 1. Actualizar configuración en Firestore
            DocumentReference configRef = firestore.collection(COLLECTION_CONFIGURACION).document(DOC_ESTADO_PARQUE);
            configRef.set(this.configuracionMemoria).get();

            // 2. Limpiar IDs antiguos de prueba si existieran en Firestore
            List<String> idsObsoletos = List.of(
                    "canopy-vuelo-condor", "bicicleta-aerea", "puente-tibetano",
                    "columpio-gigante", "mirador-mano-gigante", "giroscopio-orbital",
                    "campo-voley-mirador"
            );
            for (String oldId : idsObsoletos) {
                firestore.collection(COLLECTION_ATRACCIONES).document(oldId).delete();
            }

            // 3. Guardar/actualizar las 3 atracciones oficiales (asegurando imagen /images/tirolesa-extrema.jpg)
            for (Atraccion atraccion : atraccionesMemoria.values()) {
                firestore.collection(COLLECTION_ATRACCIONES)
                        .document(atraccion.getId())
                        .set(atraccion)
                        .get();
            }
            log.info("Firestore sincronizado: atracciones actualizadas (tirolesa-extrema -> /images/tirolesa-extrema.jpg).");

            // 4. Limpiar combos antiguos y sincronizar paquetes oficiales
            List<String> paquetesObsoletos = List.of(
                    "combo-adrenalina-total", "pack-duo-aventurero",
                    "pack-familiar-huayobamba", "delegacion-escolar-promo"
            );
            for (String oldPaqId : paquetesObsoletos) {
                firestore.collection(COLLECTION_PAQUETES).document(oldPaqId).delete();
            }

            for (Paquete paquete : paquetesMemoria.values()) {
                firestore.collection(COLLECTION_PAQUETES)
                        .document(paquete.getId())
                        .set(paquete)
                        .get();
            }
        } catch (Exception e) {
            log.warn("No se pudo sincronizar la semilla inicial con Firestore ({}). Se utilizarán los datos en memoria.", e.getMessage());
        }
    }

    private List<Atraccion> construirAtraccionesSemilla() {
        List<Atraccion> lista = new ArrayList<>();

        lista.add(Atraccion.builder()
                .id("puente-extremo")
                .nombre("Puente Extremo 'Los Vegas'")
                .descripcionCorta("Da el primer paso donde termina el miedo y empieza la verdadera libertad sobre el valle de Huayobamba.")
                .descripcionDetallada("Cruza nuestro imponente puente colgante multicolor suspendido entre las montañas de San Marcos. Cada paso es una experiencia única acompañada de paisajes inolvidables y equipamiento de protección.")
                .precio(10.00)
                .nivelAdrenalina("EXTREMO")
                .alturaMetros(1)
                .pesoMinKg(25)
                .pesoMaxKg(105)
                .edadMinima(7)
                .duracionEstimadaMin(20)
                .imagenUrl("/images/puente-extremo-portal.jpg")
                .iconoBadge("fa-road-bridge")
                .incluyeEquipo(List.of("Arnés de seguridad", "Casco protector", "Guantes de sujeción", "Orientación del guía"))
                .orden(1)
                .activo(true)
                .build());

        lista.add(Atraccion.builder()
                .id("tirolesa-extrema")
                .nombre("Tirolesa Extrema")
                .descripcionCorta("Abre los brazos, suelta la rutina y deslízate por el cielo de Huayobamba sintiendo la adrenalina pura.")
                .descripcionDetallada("Vive la sensación de volar de extremo a extremo contemplando toda la belleza natural del valle de San Marcos. Nuestro personal te equipa y asiste en todo momento para un recorrido seguro y emocionante.")
                .precio(10.00)
                .nivelAdrenalina("EXTREMO")
                .alturaMetros(1)
                .pesoMinKg(30)
                .pesoMaxKg(100)
                .edadMinima(8)
                .duracionEstimadaMin(15)
                .imagenUrl("/images/tirolesa-extrema.jpg")
                .iconoBadge("fa-wind")
                .incluyeEquipo(List.of("Arnés de seguridad", "Casco protector", "Guantes de protección", "Asistencia en salida y llegada"))
                .orden(2)
                .activo(true)
                .build());

        lista.add(Atraccion.builder()
                .id("campo-voley")
                .nombre("Campo de Voleyball")
                .descripcionCorta("Arma el partido totalmente gratis con tus amigos o familia en nuestra cancha al aire libre en Huayobamba.")
                .descripcionDetallada("Disfruta del deporte y la sana competencia sin costo adicional en nuestro Campo de Voleyball. El complemento ideal después de la adrenalina para compartir entre amigos, promociones y familias con acceso a nuestras glorietas y mirador.")
                .precio(0.00)
                .nivelAdrenalina("MEDIO")
                .alturaMetros(1)
                .pesoMinKg(10)
                .pesoMaxKg(120)
                .edadMinima(5)
                .duracionEstimadaMin(60)
                .imagenUrl("https://images.unsplash.com/photo-1612872087720-bb876e2e67d1?auto=format&fit=crop&w=1000&q=80")
                .iconoBadge("fa-volleyball")
                .incluyeEquipo(List.of("Uso de cancha GRATIS", "Red reglamentaria", "Glorietas de descanso", "Estacionamiento gratis"))
                .orden(3)
                .activo(true)
                .build());

        return lista;
    }

    private List<Paquete> construirPaquetesSemilla() {
        List<Paquete> lista = new ArrayList<>();

        lista.add(Paquete.builder()
                .id("combo-aventura-total")
                .nombre("Combo Aventura Total (Puente + Tirolesa)")
                .descripcion("Vive las dos atracciones extremas del parque por persona y disfruta gratis de nuestro Campo de Voleyball y Torre Mirador.")
                .tipoPublico("INDIVIDUAL")
                .atraccionesIncluidasIds(List.of("puente-extremo", "tirolesa-extrema", "campo-voley"))
                .atraccionesIncluidasNombres(List.of("1x Puente Extremo 'Los Vegas'", "1x Tirolesa Extrema", "Campo de Voleyball y Mirador GRATIS"))
                .precioRegularTotal(20.00)
                .precioOferta(18.00)
                .ahorroSoles(2.00)
                .minimoPersonas(1)
                .etiquetaDestacada("EXPERIENCIA COMPLETA")
                .orden(1)
                .activo(true)
                .build());

        lista.add(Paquete.builder()
                .id("pack-duo-complices")
                .nombre("Pack Dúo 'Cómplices de Aventura' (2 Personas)")
                .descripcion("Porque la emoción compartida se disfruta el doble. Ideal para parejas o amigos que quieren vivir ambos retos juntos.")
                .tipoPublico("DUO")
                .atraccionesIncluidasIds(List.of("puente-extremo", "tirolesa-extrema", "campo-voley"))
                .atraccionesIncluidasNombres(List.of("2x Puente Extremo 'Los Vegas'", "2x Tirolesa Extrema", "Campo de Voleyball y Glorietas GRATIS"))
                .precioRegularTotal(40.00)
                .precioOferta(35.00)
                .ahorroSoles(5.00)
                .minimoPersonas(2)
                .etiquetaDestacada("PAREJAS Y AMIGOS")
                .orden(2)
                .activo(true)
                .build());

        lista.add(Paquete.builder()
                .id("pack-familia-amigos")
                .nombre("Pack 'Amigos & Voleyball' (4 Personas)")
                .descripcion("Vengan en grupo a desafiar el Puente Extremo, la Tirolesa y armar el partido gratis en nuestro Campo de Voleyball.")
                .tipoPublico("FAMILIAR")
                .atraccionesIncluidasIds(List.of("puente-extremo", "tirolesa-extrema", "campo-voley"))
                .atraccionesIncluidasNombres(List.of("4x Puente Extremo 'Los Vegas'", "4x Tirolesa Extrema", "Campo de Voleyball y Glorietas GRATIS"))
                .precioRegularTotal(80.00)
                .precioOferta(70.00)
                .ahorroSoles(10.00)
                .minimoPersonas(4)
                .etiquetaDestacada("GRUPOS Y FAMILIAS")
                .orden(3)
                .activo(true)
                .build());

        lista.add(Paquete.builder()
                .id("pack-delegaciones-escolares")
                .nombre("Pack Promociones y Delegaciones (Grupal)")
                .descripcion("Tarifa especial por participante para colegios, promociones y grupos (desde 10 personas) con uso libre del Campo de Voleyball.")
                .tipoPublico("ESCOLAR_DELEGACION")
                .atraccionesIncluidasIds(List.of("puente-extremo", "tirolesa-extrema", "campo-voley"))
                .atraccionesIncluidasNombres(List.of("Puente Extremo + Tirolesa", "Campo de Voleyball GRATIS para el grupo", "Torre Mirador y Explanada"))
                .precioRegularTotal(20.00)
                .precioOferta(15.00)
                .ahorroSoles(5.00)
                .minimoPersonas(10)
                .etiquetaDestacada("COLEGIOS Y PROMOCIONES")
                .orden(4)
                .activo(true)
                .build());

        return lista;
    }

    private ConfiguracionParque construirConfiguracionSemilla() {
        return ConfiguracionParque.builder()
                .id(DOC_ESTADO_PARQUE)
                .estadoOperativo("ABIERTO_SOLEADO")
                .colorIndicador("GREEN")
                .mensajeBanner("Parque Abierto en Huayobamba • Puente Extremo, Tirolesa y Campo de Voleyball")
                .temperaturaActual("Clima Soleado Ideal")
                .horarioAtencion("10:30 AM a 6:30 PM")
                .promoFinDeSemanaActiva(true)
                .textoPromoFinDeSemana("✨ ¡ATRACCIONES A S/ 10.00! Disfruta el Puente Extremo, Tirolesa y Campo de Voleyball en Huayobamba.")
                .whatsappOficial("51953220851")
                .ultimaActualizacion("En vivo desde Huayobamba - San Marcos")
                .build();
    }
}
