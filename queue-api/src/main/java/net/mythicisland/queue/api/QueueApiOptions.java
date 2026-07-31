package net.mythicisland.queue.api;

/**
 * Configuration options for the {@link QueueApi}.
 *
 * @param grpcHost the gRPC server host
 * @param grpcPort the gRPC server port
 * @param natsUrl the NATS server URL (e.g., nats://localhost:4222)
 * @param natsUser the user for NATS authentication
 * @param natsSecret the secret/password for NATS authentication
 * @param token the shared auth token sent with every gRPC call
 */
public record QueueApiOptions(
        String grpcHost,
        int grpcPort,
        String natsUrl,
        String natsUser,
        String natsSecret,
        String token
) {

    /**
     * Default options, loaded from environment variables.
     */
    public static final QueueApiOptions DEFAULT = builder().build();

    /**
     * Validates the options.
     *
     * @throws IllegalArgumentException if the host is null or blank, or the port is out of range
     */
    public QueueApiOptions {
        if (grpcHost == null || grpcHost.isBlank()) {
            throw new IllegalArgumentException("grpcHost must not be null or blank");
        }
        if (grpcPort < 1 || grpcPort > 65535) {
            throw new IllegalArgumentException("grpcPort must be between 1 and 65535, got: " + grpcPort);
        }
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
        private String token;

        /**
         * Initializes a new builder with default values.
         */
        public Builder() {
            this.grpcHost = System.getenv().getOrDefault("QUEUE_GRPC_HOST", "localhost");
            this.grpcPort = parsePort(System.getenv("QUEUE_GRPC_PORT"));
            this.natsUrl = System.getenv().getOrDefault("QUEUE_NATS_URL", "nats://localhost:4222");
            this.natsUser = System.getenv().getOrDefault("QUEUE_NATS_USER", "admin");
            this.natsSecret = System.getenv().getOrDefault("QUEUE_NATS_SECRET", "sup3rS3cr3t");
            this.token = System.getenv().getOrDefault("QUEUE_TOKEN", "");
        }

        /**
         * Sets the gRPC host.
         *
         * @param grpcHost the host address
         * @return this builder
         */
        public Builder grpcHost(String grpcHost) {
            this.grpcHost = grpcHost;
            return this;
        }

        /**
         * Sets the gRPC port.
         *
         * @param grpcPort the port number
         * @return this builder
         */
        public Builder grpcPort(int grpcPort) {
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
         * Sets the shared auth token sent with every gRPC call.
         *
         * @param token the auth token
         * @return this builder
         */
        public Builder token(String token) {
            this.token = token;
            return this;
        }

        /**
         * Builds the {@link QueueApiOptions} instance.
         *
         * @return the configured options
         * @throws IllegalArgumentException if the host is null or blank, or the port is out of range
         */
        public QueueApiOptions build() {
            return new QueueApiOptions(grpcHost, grpcPort, natsUrl, natsUser, natsSecret, token);
        }

        private static int parsePort(String raw) {
            if (raw == null || raw.isBlank()) {
                return 4564;
            }
            try {
                return Integer.parseInt(raw.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid value: '" + raw + "'", e);
            }
        }

    }
}
