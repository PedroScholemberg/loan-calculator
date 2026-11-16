FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl unzip \
    && rm -rf /var/lib/apt/lists/*

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Windows checkouts may use CRLF and do not preserve the executable bit.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
COPY src/ src/
RUN ./mvnw --batch-mode --no-transfer-progress verify

FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 app

COPY --from=build --chown=10001:10001 /app/target/loan-calculator-api-*.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
