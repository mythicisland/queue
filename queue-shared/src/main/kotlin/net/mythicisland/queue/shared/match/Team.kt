package net.mythicisland.queue.shared.match

import build.buf.gen.mythicisland.queue.v1.team
import java.util.UUID
import build.buf.gen.mythicisland.queue.v1.Team as TeamProto

data class Team(
    val playerIds: List<UUID>,
) {

    fun toProto(): TeamProto = team {
        playerIds += this@Team.playerIds.map(UUID::toString)
    }

    companion object {
        fun fromProto(proto: TeamProto): Team = Team(proto.playerIdsList.map(UUID::fromString))
    }

}
