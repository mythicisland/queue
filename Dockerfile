# The jar is built by gradle before the image is built:
#   ./gradlew :queue-runtime:shadowJar
FROM eclipse-temurin:25-jre

ARG JAR=queue-runtime/build/libs/queue-runtime.jar

WORKDIR /app

# The directories the runtime uses by default. Where they actually live and
# which port it listens on stays configurable through env vars or a mounted
# queue.properties, see QueueStartCommand.
RUN useradd --system --uid 1000 queue \
    && mkdir -p types .secrets logs \
    && chown -R queue:queue /app

COPY --chown=queue:queue ${JAR} queue-runtime.jar

USER queue

# Netty loads a native library and the protobuf shaded into the simplecloud api
# still uses sun.misc.Unsafe. Both only warn, the flags keep the log clean.
ENTRYPOINT ["java", \
    "--enable-native-access=ALL-UNNAMED", \
    "--sun-misc-unsafe-memory-access=allow", \
    "-jar", "queue-runtime.jar"]
