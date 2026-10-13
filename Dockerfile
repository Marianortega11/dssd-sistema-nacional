# Etapa 1: compilacion
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Etapa 2: imagen final liviana
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN useradd --system --no-create-home appuser
COPY --from=build /app/target/app.jar app.jar
USER appuser
# Render/Heroku definen PORT en runtime (ver server.port en application.properties)
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
