# Compilación en una imagen con Maven; la imagen final solo lleva el resultado.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Las dependencias se resuelven primero para aprovechar la caché entre compilaciones.
COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY src ./src
RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# La aplicación corre sin privilegios de administrador.
RUN addgroup -S portal && adduser -S portal -G portal
COPY --from=build /build/target/*.jar app.jar
RUN chown portal:portal app.jar
USER portal

EXPOSE 8080
ENV TZ=America/Mexico_City

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/api/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
