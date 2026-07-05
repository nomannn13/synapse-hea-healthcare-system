FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src src
RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S synapse && adduser -S synapse -G synapse
WORKDIR /app
RUN mkdir -p /app/data/uploads && chown -R synapse:synapse /app
COPY --from=build /workspace/target/hea-backend-*.jar app.jar
RUN chown synapse:synapse /app/app.jar
USER synapse
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
