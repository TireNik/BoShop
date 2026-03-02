FROM amazoncorretto:17-alpine
WORKDIR /app
COPY target/*.jar app.jar
RUN apk add --no-cache curl
ENTRYPOINT ["java", "-jar", "app.jar"]