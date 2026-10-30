package net.mythicisland.queue.runtime.metrics

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.instrumentation.runtimetelemetry.RuntimeTelemetry
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory

class Telemetry(
    val otel: OpenTelemetry,
    private val sdk: OpenTelemetrySdk?,
    private val runtime: RuntimeTelemetry?,
) {

    fun shutdown() {
        runtime?.close()
        sdk?.close()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Telemetry::class)
        private val INSTANCE_ID = AttributeKey.stringKey("service.instance.id")
        private val DEFAULTS = mapOf(
            "otel.service.name" to "queue",
            "otel.exporter.otlp.protocol" to "http/protobuf",
            "otel.traces.exporter" to "none",
            "otel.logs.exporter" to "none",
        )

        fun create(instanceId: String): Telemetry {
            val endpoint = System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT")
            if (endpoint.isNullOrBlank()) {
                logger.info("OpenTelemetry disabled", Field("reason", "OTEL_EXPORTER_OTLP_ENDPOINT is not set"))
                return Telemetry(OpenTelemetry.noop(), null, null)
            }

            val sdk = AutoConfiguredOpenTelemetrySdk.builder()
                .addPropertiesSupplier { DEFAULTS }
                .addResourceCustomizer { resource, _ -> withInstanceId(resource, instanceId) }
                .disableShutdownHook()
                .build()
                .openTelemetrySdk

            logger.info("OpenTelemetry enabled", Field("endpoint", endpoint))
            return Telemetry(sdk, sdk, RuntimeTelemetry.create(sdk))
        }

        private fun withInstanceId(resource: Resource, instanceId: String): Resource {
            if (resource.getAttribute(INSTANCE_ID) != null) {
                return resource
            }
            return resource.merge(Resource.create(Attributes.of(INSTANCE_ID, instanceId)))
        }
    }

}
