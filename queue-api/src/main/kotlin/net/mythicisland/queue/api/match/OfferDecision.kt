package net.mythicisland.queue.api.match

/**
 * What [MatchApi.onOffered] does with an offered match.
 */
class OfferDecision private constructor(internal val accepted: Boolean, internal val reason: String) {

    companion object {

        /**
         * Accepts the match, the countdown starts right away.
         */
        @JvmStatic
        fun accept(): OfferDecision = OfferDecision(true, "")

        /**
         * Rejects the match with [reason]. It won't be offered to this server again.
         */
        @JvmStatic
        fun reject(reason: String): OfferDecision = OfferDecision(false, reason)

    }

}
