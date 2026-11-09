package net.mythicisland.queue.runtime.type

import net.mythicisland.queue.shared.type.MatchRule
import net.mythicisland.queue.shared.type.MatchRuleType
import net.mythicisland.queue.shared.type.QueueType

object QueueTypeValidator {

    fun validate(type: QueueType): List<String> {
        val server = type.server
        val shared = server.shared
        val capacity = shared?.capacity
        val match = type.match
        val teams = match.teams
        val partySize = match.parties.maxSize
        val balanceBy = teams?.balanceBy
        val maxWait = match.maxWait
        val maxFailures = match.maxFailures
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
        if (maxWait != null && !maxWait.isPositive) {
            problems += "match.max-wait must be positive"
        }
        if (maxFailures != null && maxFailures < 1) {
            problems += "match.max-failures must be at least 1"
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
        if (balanceBy != null && balanceBy.isBlank()) {
            problems += "match.teams.balance-by must not be blank"
        }

        match.rules.forEachIndexed { index, rule -> problems += validateRule("match.rules[$index]", rule) }
        val duplicates = match.rules.map { it.key }.filter { it.isNotBlank() }.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        for (key in duplicates) {
            problems += "match.rules must not have more than one rule for key $key"
        }

        return problems
    }

    private fun validateRule(path: String, rule: MatchRule): List<String> {
        val relaxAfter = rule.relaxAfter
        val start = rule.start
        val widen = rule.widen
        val every = rule.every
        val max = rule.max
        val problems = mutableListOf<String>()

        if (rule.type == null) {
            problems += "$path.type must be set"
        }
        if (rule.key.isBlank()) {
            problems += "$path.key must be set"
        }

        if (rule.type != MatchRuleType.EQUAL && relaxAfter != null) {
            problems += "$path.relax-after is only allowed for equal rules"
        }
        if (relaxAfter != null && !relaxAfter.isPositive) {
            problems += "$path.relax-after must be positive"
        }

        if (rule.type != MatchRuleType.RANGE) {
            if (start != null || widen != null || every != null || max != null) {
                problems += "$path.start, widen, every and max are only allowed for range rules"
            }
            return problems
        }

        if (start == null) {
            problems += "$path.start must be set for range rules"
        }
        if (start != null && (start < 0 || !start.isFinite())) {
            problems += "$path.start must be a number that is not negative"
        }
        if (widen != null && (widen < 0 || !widen.isFinite())) {
            problems += "$path.widen must be a number that is not negative"
        }
        if (widen != null && widen > 0 && (every == null || !every.isPositive)) {
            problems += "$path.every must be set and positive when widen is set"
        }
        if (every != null && (widen == null || widen == 0.0)) {
            problems += "$path.every is only allowed when widen is set"
        }
        if (max != null && start != null && max < start) {
            problems += "$path.max must not be smaller than start"
        }
        return problems
    }

    fun validateGroups(types: Collection<QueueType>): List<String> =
        types.groupBy { it.server.group }
            .filterValues { group -> group.any { it.server.shared != null } && group.any { it.server.shared == null } }
            .keys
            .map { "server.group $it must not be used by shared and exclusive queue types at the same time" }

}
