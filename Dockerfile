FROM eclipse-temurin:25-jre

ARG JAR=queue-runtime/build/libs/queue-runtime.jar

WORKDIR /app

RUN mkdir -p types .secrets logs

COPY ${JAR} queue-runtime.jar

ENTRYPOINT ["java","-jar", "queue-runtime.jar"]
