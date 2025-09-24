# ---------- Build ----------
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml for dependency resolution
COPY pom.xml ./

# Download dependencies first to leverage cache
RUN mvn -q -e -DskipTests dependency:go-offline

# Copy source code
COPY src ./src

# Build and package the project (skip tests)
RUN mvn -q -e -DskipTests package

# ---------- Runtime ----------
FROM eclipse-temurin:17-jre

WORKDIR /opt/app

# Copy the built JAR from build stage
COPY --from=build /app/target/shipping-ops-producer-*.jar app.jar

# Environment variable for JVM flags
ENV JAVA_OPTS=""

# Expose port 8087
EXPOSE 8087

# Start command
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]