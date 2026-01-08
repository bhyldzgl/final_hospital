# Build Stage (Maven)
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# (Opsiyonel ama hızlı build için iyi) önce pom + wrapper kopyala
COPY pom.xml ./
COPY mvnw ./
COPY .mvn .mvn

# Bağımlılıkları cache'lemek için (ilk build'i hızlandırır)
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline

# Proje dosyalarını kopyala
COPY . .

# Maven build (testleri atla)
RUN ./mvnw -DskipTests clean package

# Run Stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Spring Boot jar genelde target/ altında olur
COPY --from=build /app/target/*.jar app.jar

# Create uploads directory
RUN mkdir -p /app/uploads

EXPOSE 9060
ENTRYPOINT ["java", "-jar", "app.jar"]
