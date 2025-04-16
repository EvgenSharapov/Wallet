FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY target/Wallet-0.0.1-SNAPSHOT.war app.jar

ENV SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5434/wallet_db
ENV SPRING_DATASOURCE_USERNAME=root
ENV SPRING_DATASOURCE_PASSWORD=root

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
