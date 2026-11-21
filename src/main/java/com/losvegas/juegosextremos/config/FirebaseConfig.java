package com.losvegas.juegosextremos.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * Configuración híbrida de conexión a Firebase Firestore:
 * 1) Variable de entorno FIREBASE_CONFIG_JSON (Producción en Render / Docker)
 * 2) Archivo local firebase-key.json en classpath o raíz del proyecto (Desarrollo Local)
 * 3) Modo Fallback Resiliente en memoria si aún no se han configurado credenciales
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Getter
    private boolean firebaseConectado = false;

    @Bean
    public Firestore firestore() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                this.firebaseConectado = true;
                return FirestoreClient.getFirestore();
            }

            InputStream credentialsStream = resolveCredentialsStream();
            if (credentialsStream == null) {
                log.warn("=================================================================================");
                log.warn("[MODO DEMO LOCAL ACTIVO] No se detectó FIREBASE_CONFIG_JSON ni firebase-key.json.");
                log.warn("Juegos Extremos Los Vegas funcionará con almacenamiento en memoria local.");
                log.warn("Para conectar a Firestore real, coloca 'firebase-key.json' en src/main/resources/");
                log.warn("o define la variable de entorno FIREBASE_CONFIG_JSON.");
                log.warn("=================================================================================");
                this.firebaseConectado = false;
                return null;
            }

            try (InputStream is = credentialsStream) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(is))
                        .build();
                FirebaseApp.initializeApp(options);
                this.firebaseConectado = true;
                log.info("Conexión exitosa con Firebase Firestore para Juegos Extremos Los Vegas.");
                return FirestoreClient.getFirestore();
            }
        } catch (Exception e) {
            log.error("No se pudo inicializar Firebase Firestore ({}). Activando modo Fallback en memoria local.", e.getMessage());
            this.firebaseConectado = false;
            return null;
        }
    }

    private InputStream resolveCredentialsStream() throws Exception {
        // Prioridad 1: Variable de entorno FIREBASE_CONFIG_JSON (Render / Docker)
        String envConfigJson = System.getenv("FIREBASE_CONFIG_JSON");
        if (envConfigJson != null && !envConfigJson.isBlank()) {
            String cleanJson = envConfigJson.trim();
            if ((cleanJson.startsWith("'") && cleanJson.endsWith("'"))
                    || (cleanJson.startsWith("\"") && cleanJson.endsWith("\""))) {
                cleanJson = cleanJson.substring(1, cleanJson.length() - 1).trim();
            }
            if (cleanJson.startsWith("{")) {
                log.info("Cargando credenciales de Firebase desde la variable de entorno FIREBASE_CONFIG_JSON.");
                return new ByteArrayInputStream(cleanJson.getBytes(StandardCharsets.UTF_8));
            }
        }

        // Prioridad 2A: Archivo firebase-key.json en src/main/resources/
        ClassPathResource resource = new ClassPathResource("firebase-key.json");
        if (resource.exists()) {
            log.info("Cargando credenciales de Firebase desde classpath:firebase-key.json.");
            return resource.getInputStream();
        }

        // Prioridad 2B: Archivo firebase-key.json en el directorio raíz del proyecto
        File rootKeyFile = new File("firebase-key.json");
        if (rootKeyFile.exists() && rootKeyFile.isFile()) {
            log.info("Cargando credenciales de Firebase desde ./firebase-key.json en la raíz del proyecto.");
            return new FileInputStream(rootKeyFile);
        }

        return null;
    }
}
