FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /build

# Install parent POM (non-recursive to skip missing module dirs)
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -N install -q

# Build and install common-dto (local dependency)
COPY common-dto ./common-dto
RUN --mount=type=cache,target=/root/.m2 mvn -f common-dto/pom.xml install -DskipTests -q

# Build service
COPY standardization-service ./standardization-service
RUN --mount=type=cache,target=/root/.m2 mvn -f standardization-service/pom.xml package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /build/standardization-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
