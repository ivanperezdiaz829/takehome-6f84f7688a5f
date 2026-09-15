# The build context is the PROJECT ROOT, not this directory: the front is a module of its own and
# this server reads it from disk, so the image needs both.
#
#   docker build -f backend/Dockerfile .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY backend/pom.xml ./
RUN mvn -q -B dependency:go-offline
COPY backend/src ./src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app/backend
COPY --from=build /src/target/occupancy-and-report.jar ./app.jar
COPY frontend /app/frontend
USER nobody
CMD ["java", "-jar", "app.jar"]
