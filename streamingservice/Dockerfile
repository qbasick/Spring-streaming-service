FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:resolve

COPY src ./src

EXPOSE 8080/tcp
EXPOSE 1935/tcp

CMD ["./mvnw", "spring-boot:run"]
