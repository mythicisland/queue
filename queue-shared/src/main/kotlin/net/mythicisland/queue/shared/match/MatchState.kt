package net.mythicisland.queue.shared.match

import build.buf.gen.mythicisland.queue.v1.MatchState as MatchStateProto

enum class MatchState {
    ALLOCATING,
    OFFERED,
    COUNTDOWN,
    TRANSFERRING,
    COMPLETED,
    FAILED,
    CANCELLED;

    fun toProto(): MatchStateProto = when (this) {
        ALLOCATING -> MatchStateProto.MATCH_STATE_ALLOCATING
        OFFERED -> MatchStateProto.MATCH_STATE_OFFERED
        COUNTDOWN -> MatchStateProto.MATCH_STATE_COUNTDOWN
        TRANSFERRING -> MatchStateProto.MATCH_STATE_TRANSFERRING
        COMPLETED -> MatchStateProto.MATCH_STATE_COMPLETED
        FAILED -> MatchStateProto.MATCH_STATE_FAILED
        CANCELLED -> MatchStateProto.MATCH_STATE_CANCELLED
    }

    companion object {
        fun fromProto(proto: MatchStateProto): MatchState = when (proto) {
            MatchStateProto.MATCH_STATE_ALLOCATING -> ALLOCATING
            MatchStateProto.MATCH_STATE_OFFERED -> OFFERED
            MatchStateProto.MATCH_STATE_COUNTDOWN -> COUNTDOWN
            MatchStateProto.MATCH_STATE_TRANSFERRING -> TRANSFERRING
            MatchStateProto.MATCH_STATE_COMPLETED -> COMPLETED
            MatchStateProto.MATCH_STATE_FAILED -> FAILED
            MatchStateProto.MATCH_STATE_CANCELLED -> CANCELLED
            else -> throw IllegalArgumentException("Unknown match state $proto")
        }
    }

}
