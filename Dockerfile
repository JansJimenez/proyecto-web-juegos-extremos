# ==============================================================================
# ETAPA 1: BUILD (Compilación con Maven Wrapper + JDK 17)
# ==============================================================================
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /workspace

# Copiar configuración de Maven primero para aprovechar la caché de capas de Docker
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copiar el código fuente y empaquetar el JAR ejecutable
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# ==============================================================================
# ETAPA 2: RUNTIME (Imagen ligera JRE 17 optimizada para Render Free 512MB RAM)
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Crear usuario no-root por seguridad en producción
RUN addgroup -S losvegas && adduser -S losvegas -G losvegas
USER losvegas:losvegas

# Copiar el artefacto compilado desde la etapa anterior
COPY --from=build /workspace/target/*.jar app.jar

# Variables de entorno por defecto y optimización de memoria JVM (512MB RAM en Render)
ENV PORT=8080
ENV JAVA_OPTS="-Dfile.encoding=UTF-8 -XX:MaxRAMPercentage=75.0 -XX:+UseSerialGC -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

# Arranque respetando dinámicamente la variable PORT inyectada por Render
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
