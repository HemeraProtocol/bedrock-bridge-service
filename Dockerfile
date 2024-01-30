FROM maven:3.8.4-openjdk-11 as builder

WORKDIR /app

COPY . .

RUN mvn clean package

FROM openjdk:11-jdk-slim

WORKDIR /app

COPY --from=builder /app/target/ethereum-sync-1.0.0-SNAPSHOT-shaded.jar ./ethereum-sync-shaded.jar

ENTRYPOINT ["java", "-cp", "ethereum-sync-shaded.jar"]