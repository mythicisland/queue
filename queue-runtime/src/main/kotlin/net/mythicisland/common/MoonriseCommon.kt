package net.mythicisland.common

import app.simplecloud.api.CloudApi
import app.simplecloud.api.CloudApiOptions
import org.apache.logging.log4j.LogManager

object MoonriseCommon {

    private val logger = LogManager.getLogger(MoonriseCommon::class.java)

    /**
     * Creates a Cloud API instance.
     */
    fun connectToController(
        networkId: String,
        networkSecret: String,
        controllerUrl: String,
        controllerNatsUrl: String
    ): CloudApi {
        logger.info("Connecting to your Network...")
        val api = CloudApi.create(
            CloudApiOptions.builder()
                .networkId(networkId)
                .networkSecret(networkSecret)
                .controllerUrl(controllerUrl)
                .natsUrl(controllerNatsUrl)
                .build()
        )
        logger.info("Successfully connected to your Network")
        logger.info("Network ID: {}", api.networkId)
        return api
    }

}
