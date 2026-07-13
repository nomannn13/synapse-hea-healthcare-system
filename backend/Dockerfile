FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src src
RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache su-exec
RUN addgroup -S synapse && adduser -S synapse -G synapse
WORKDIR /app
RUN mkdir -p /app/data/uploads && chown -R synapse:synapse /app
COPY --from=build /workspace/target/hea-backend-*.jar app.jar
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh
RUN chown synapse:synapse /app/app.jar
EXPOSE 8080
ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
