# Paso 1: Usar una imagen de Java ligera
FROM eclipse-temurin:17-jdk-alpine

# Paso 2: Crear un directorio para la app
WORKDIR /app

# Paso 3: Copiar el archivo .jar (el que generas con Maven)
# Asegúrate de haber hecho 'mvn clean package' antes
COPY target/*.jar app.jar

# Paso 4: Exponer el puerto de Spring
EXPOSE 8080

# Paso 5: Comando para arrancar la app
ENTRYPOINT ["java", "-jar", "app.jar"]