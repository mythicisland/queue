package net.mythicisland.queue.runtime.type

import net.mythicisland.queue.shared.type.QueueType

object QueueTypeValidator {

    fun validate(type: QueueType): List<String> {
        val server = type.server
        val shared = server.shared
        val capacity = shared?.capacity
        val match = type.match
        val teams = match.teams
        val partySize = match.parties.maxSize
        val problems = mutableListOf<String>()

        if (server.group.isBlank()) {
            problems += "server.group must be set"
        }
        if (!server.allocationTimeout.isPositive) {
            problems += "server.allocation-timeout must be positive"
        }
        if (shared != null && !shared.acceptTimeout.isPositive) {
            problems += "server.shared.accept-timeout must be positive"
        }
        if (capacity != null && capacity < 1) {
            problems += "server.shared.capacity must be at least 1"
        }

        if (match.minPlayers < 1) {
            problems += "match.min-players must be at least 1"
        }
        if (match.fillTime.isNegative) {
            problems += "match.fill-time must not be negative"
        }
        if (match.countdown.isNegative) {
            problems += "match.countdown must not be negative"
        }

        if (teams == null && match.maxPlayers == null) {
            problems += "match.max-players or match.teams must be set"
        }
        if (teams != null && match.maxPlayers != null) {
            problems += "match.max-players must not be set together with match.teams, it is always count * size"
        }
        if (teams != null && teams.count < 2) {
            problems += "match.teams.count must be at least 2"
        }
        if (teams != null && teams.size < 1) {
            problems += "match.teams.size must be at least 1"
        }
        if ((teams != null || match.maxPlayers != null) && match.minPlayers > match.effectiveMaxPlayers) {
            problems += "match.min-players must not be larger than the maximum of ${match.effectiveMaxPlayers} players"
        }

        if (partySize != null && partySize < 1) {
            problems += "match.parties.max-size must be at least 1"
        }
        if (partySize != null && teams != null && partySize > teams.size) {
            problems += "match.parties.max-size must not be larger than match.teams.size"
        }
        if (partySize != null && teams == null && partySize > match.effectiveMaxPlayers) {
            problems += "match.parties.max-size must not be larger than match.max-players"
        }

        return problems
    }

    fun validateGroups(types: Collection<QueueType>): List<String> =
        types.groupBy { it.server.group }
            .filterValues { group -> group.any { it.server.shared != null } && group.any { it.server.shared == null } }
            .keys
            .map { "server.group $it must not be used by shared and exclusive queue types at the same time" }

}
