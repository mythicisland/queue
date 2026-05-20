package net.mythicisland.queue.api;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configuration options for the {@link QueueApi}.
 *
 * <p>Contains connection details for both gRPC (data) and NATS (events).</p>
 */
public class QueueApiOptions {

    /**
     * Default options, loaded from environment variables or sensible defaults.
     */
    public static final QueueApiOptions DEFAULT = new Builder().build();

    private static final Pattern DURATION_PATTERN = Pattern.compile("^(\\d+)(ms|s|m|h)$");

    private final String grpcHost;
    private final int grpcPort;
    private final String natsUrl;
    private final String natsUser;
    private final String natsSecret;

    private QueueApiOptions(Builder builder) {
        this.grpcHost = builder.grpcHost;
        this.grpcPort = builder.grpcPort;
        this.natsUrl = builder.natsUrl;
        this.natsUser = builder.natsUser;
        this.natsSecret = builder.natsSecret;
    }

    /**
     * Gets the gRPC server host.
     *
     * @return the gRPC host
     */
    public String getGrpcHost() {
        return grpcHost;
    }

    /**
     * Gets the gRPC server port.
     *
     * @return the gRPC port
     */
    public int getGrpcPort() {
        return grpcPort;
    }

    /**
     * Gets the NATS server URL.
     *
     * @return the NATS URL
     */
    public String getNatsUrl() {
        return natsUrl;
    }

    /**
     * Gets the NATS user for authentication.
     *
     * @return the NATS user
     */
    public String getNatsUser() {
        return natsUser;
    }

    /**
     * Gets the NATS secret/password for authentication.
     *
     * @return the NATS secret
     */
    public String getNatsSecret() {
        return natsSecret;
    }

    /**
     * Creates a new builder for {@link QueueApiOptions}.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link QueueApiOptions}.
     *
     * <p>Initializes with values from environment variables where available.</p>
     */
    public static class Builder {
        private String grpcHost;
        private int grpcPort;
        private String natsUrl;
        private String natsUser;
        private String natsSecret;

        /**
         * Initializes a new builder with default values.
         */
        public Builder() {
            this.grpcHost = System.getenv().getOrDefault("QUEUE_GRPC_HOST", "localhost");
            this.grpcPort = parsePort(System.getenv("QUEUE_GRPC_PORT"), 4564);
            this.natsUrl = System.getenv().getOrDefault("QUEUE_NATS_URL", "nats://localhost:4222");
            this.natsUser = System.getenv().getOrDefault("QUEUE_NATS_USER", "admin");
            this.natsSecret = System.getenv().getOrDefault("QUEUE_NATS_SECRET", "sup3rS3cr3t");
        }

        /**
         * Sets the gRPC host.
         *
         * @param grpcHost the host address
         * @return this builder
         * @throws IllegalArgumentException if host is null or blank
         */
        public Builder grpcHost(String grpcHost) {
            if (grpcHost == null || grpcHost.isBlank()) {
                throw new IllegalArgumentException("grpcHost must not be null or blank");
            }
            this.grpcHost = grpcHost;
            return this;
        }

        /**
         * Sets the gRPC port.
         *
         * @param grpcPort the port number
         * @return this builder
         * @throws IllegalArgumentException if port is out of range
         */
        public Builder grpcPort(int grpcPort) {
            if (grpcPort < 1 || grpcPort > 65535) {
                throw new IllegalArgumentException("grpcPort must be between 1 and 65535, got: " + grpcPort);
            }
            this.grpcPort = grpcPort;
            return this;
        }

        /**
         * Sets the NATS server URL.
         *
         * @param natsUrl the NATS URL (e.g., nats://localhost:4222)
         * @return this builder
         */
        public Builder natsUrl(String natsUrl) {
            this.natsUrl = natsUrl;
            return this;
        }

        /**
         * Sets the NATS user.
         *
         * @param natsUser the user for NATS authentication
         * @return this builder
         */
        public Builder natsUser(String natsUser) {
            this.natsUser = natsUser;
            return this;
        }

        /**
         * Sets the NATS secret.
         *
         * @param natsSecret the secret for NATS authentication
         * @return this builder
         */
        public Builder natsSecret(String natsSecret) {
            this.natsSecret = natsSecret;
            return this;
        }

        /**
         * Builds the {@link QueueApiOptions} instance.
         *
         * @return the configured options
         */
        public QueueApiOptions build() {
            return new QueueApiOptions(this);
        }

        private static int parsePort(String raw, int defaultValue) {
            if (raw == null || raw.isBlank()) {
                return defaultValue;
            }
            try {
                int port = Integer.parseInt(raw.trim());
                if (port < 1 || port > 65535) {
                    throw new IllegalArgumentException("Port must be between 1 and 65535, got: " + port);
                }
                return port;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid value: '" + raw + "'", e);
            }
        }

        private static Duration parseDuration(String raw, Duration defaultValue) {
            if (raw == null || raw.isBlank()) {
                return defaultValue;
            }

            String value = raw.trim().toLowerCase();
            Matcher matcher = DURATION_PATTERN.matcher(value);
            if (matcher.matches()) {
                long amount = Long.parseLong(matcher.group(1));
                return switch (matcher.group(2)) {
                    case "ms" -> Duration.ofMillis(amount);
                    case "s" -> Duration.ofSeconds(amount);
                    case "m" -> Duration.ofMinutes(amount);
                    case "h" -> Duration.ofHours(amount);
                    default -> defaultValue;
                };
            }

            try {
                Duration duration = Duration.parse(raw);
                if (duration.isNegative()) {
                    throw new IllegalArgumentException("Duration must be >= 0");
                }
                return duration;
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }
    }
}
