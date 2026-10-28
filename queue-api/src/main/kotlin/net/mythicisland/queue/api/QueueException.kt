package net.mythicisland.queue.api

/**
 * Thrown when a call to the queue fails.
 */
class QueueException(val code: Code, message: String, cause: Throwable? = null) : RuntimeException(message, cause) {

    /**
     * Why a call to the queue failed.
     */
    enum class Code {

        /**
         * The ticket or match does not exist.
         */
        NOT_FOUND,

        /**
         * One of the players already has a ticket.
         */
        ALREADY_EXISTS,

        /**
         * The request is invalid, e.g. an unknown queue type or properties that exceed their limits.
         */
        INVALID_ARGUMENT,

        /**
         * The match is not in the right state anymore, e.g. an offer that already timed out.
         */
        FAILED_PRECONDITION,

        /**
         * The match belongs to another server.
         */
        PERMISSION_DENIED,

        /**
         * The auth secret is wrong.
         */
        UNAUTHENTICATED,

        /**
         * The queue can't be reached.
         */
        UNAVAILABLE,

        /**
         * Any other error, see the message of the [QueueException].
         */
        UNKNOWN,

    }

}
