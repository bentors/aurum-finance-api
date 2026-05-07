# Etapa 1: Build (Compila o JAR)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Etapa 2: Runtime (Executa o JAR de forma leve)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
# Configurações de memória para o plano gratuito do Render (512MB)
ENTRYPOINT ["sh", "-c", "java -Xmx300m -Dserver.port=${PORT:-8080} -jar app.jar"]