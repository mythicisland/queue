FROM eclipse-temurin:25-jre

ARG JAR=queue-runtime/build/libs/queue-runtime.jar

WORKDIR /app

RUN useradd --system --uid 1000 queue \
    && mkdir -p types .secrets logs \
    && chown -R queue:queue /app

COPY --chown=queue:queue ${JAR} queue-runtime.jar

USER queue

ENTRYPOINT ["java","-jar", "queue-runtime.jar"]
