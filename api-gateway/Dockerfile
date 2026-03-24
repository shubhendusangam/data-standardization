FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /build

# Install parent POM (non-recursive to skip missing module dirs)
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -N install -q

# Build service
COPY api-gateway ./api-gateway
RUN --mount=type=cache,target=/root/.m2 mvn -f api-gateway/pom.xml package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /build/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
