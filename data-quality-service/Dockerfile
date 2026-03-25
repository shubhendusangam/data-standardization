FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /build

# Install parent POM (non-recursive to skip missing module dirs)
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -N install -q

# Build and install common-dto (local dependency)
COPY common-dto ./common-dto
RUN --mount=type=cache,target=/root/.m2 mvn -f common-dto/pom.xml install -DskipTests -q

# Build service
COPY data-quality-service ./data-quality-service
RUN --mount=type=cache,target=/root/.m2 mvn -f data-quality-service/pom.xml package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /build/data-quality-service/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
