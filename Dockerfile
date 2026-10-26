FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -q -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --system --uid 1001 appuser
COPY --from=build /app/target/movie-catalog-*.jar app.jar
USER appuser

ENV SPRING_H2_CONSOLE_SETTINGS_WEB_ALLOW_OTHERS=true

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]