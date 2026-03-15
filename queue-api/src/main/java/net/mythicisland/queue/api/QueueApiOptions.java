package net.mythicisland.queue.api;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueueApiOptions {

    public static final QueueApiOptions DEFAULT = new Builder().build();

    private static final Pattern DURATION_PATTERN = Pattern.compile("^(\\d+)(ms|s|m|h)$");

    private final String grpcHost;
    private final int grpcPort;
    private final String natsUrl;
    private final Duration natsFailoverReconnectAfter;
    private final String natsUser;
    private final String natsSecret;

    private QueueApiOptions(Builder builder) {
        this.grpcHost = builder.grpcHost;
        this.grpcPort = builder.grpcPort;
        this.natsUrl = builder.natsUrl;
        this.natsFailoverReconnectAfter = builder.natsFailoverReconnectAfter;
        this.natsUser = builder.natsUser;
        this.natsSecret = builder.natsSecret;
    }

    public String getGrpcHost() {
        return grpcHost;
    }

    public int getGrpcPort() {
        return grpcPort;
    }

    public String getNatsUrl() {
        return natsUrl;
    }

    public Duration getNatsFailoverReconnectAfter() {
        return natsFailoverReconnectAfter;
    }

    public String getNatsUser() {
        return natsUser;
    }

    public String getNatsSecret() {
        return natsSecret;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String grpcHost;
        private int grpcPort;
        private String natsUrl;
        private Duration natsFailoverReconnectAfter;
        private String natsUser;
        private String natsSecret;

        public Builder() {
            this.grpcHost = System.getenv().getOrDefault("FRIENDS_GRPC_HOST", "localhost");
            this.grpcPort = parsePort(System.getenv("FRIENDS_GRPC_PORT"), 4564);
            this.natsUrl = System.getenv().getOrDefault("FRIENDS_NATS_URL", "nats://friends.mythicisland.net:4222");
            this.natsFailoverReconnectAfter = parseDuration(System.getenv("FRIENDS_NATS_FAILOVER_RECONNECT_AFTER"), Duration.ofSeconds(30));
            this.natsUser = System.getenv().getOrDefault("FRIENDS_NATS_USER", "mythicisland");
            this.natsSecret = System.getenv().getOrDefault("FRIENDS_NATS_SECRET", "t3s6z3ovgYQNk4AiDJhnZH1hp2fZeglo");
        }

        public Builder grpcHost(String grpcHost) {
            if (grpcHost == null || grpcHost.isBlank()) {
                throw new IllegalArgumentException("grpcHost must not be null or blank");
            }
            this.grpcHost = grpcHost;
            return this;
        }

        public Builder grpcPort(int grpcPort) {
            if (grpcPort < 1 || grpcPort > 65535) {
                throw new IllegalArgumentException("grpcPort must be between 1 and 65535, got: " + grpcPort);
            }
            this.grpcPort = grpcPort;
            return this;
        }

        public Builder natsUrl(String natsUrl) {
            this.natsUrl = natsUrl;
            return this;
        }

        public Builder natsFailoverReconnectAfter(Duration natsFailoverReconnectAfter) {
            if (natsFailoverReconnectAfter == null) {
                throw new IllegalArgumentException("natsFailoverReconnectAfter must not be null");
            }
            if (natsFailoverReconnectAfter.isNegative()) {
                throw new IllegalArgumentException("natsFailoverReconnectAfter must be >= 0");
            }
            this.natsFailoverReconnectAfter = natsFailoverReconnectAfter;
            return this;
        }

        public Builder natsUser(String natsUser) {
            this.natsUser = natsUser;
            return this;
        }

        public Builder natsSecret(String natsSecret) {
            this.natsSecret = natsSecret;
            return this;
        }

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
                    throw new IllegalArgumentException("FRIENDS_GRPC_PORT must be between 1 and 65535, got: " + port);
                }
                return port;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid FRIENDS_GRPC_PORT value: '" + raw + "'", e);
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
                throw new IllegalArgumentException(
                        "Invalid FRIENDS_NATS_FAILOVER_RECONNECT_AFTER value '" + raw +
                                "'. Use values like 30s, 2m, 1h, 500ms, or ISO-8601 (e.g. PT30S).",
                        e
                );
            }
        }
    }
}
