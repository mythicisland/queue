package net.mythicisland.queue.runtime.type

import net.mythicisland.queue.shared.type.MatchFilter
import net.mythicisland.queue.shared.type.MatchRule
import net.mythicisland.queue.shared.type.MatchRuleType
import net.mythicisland.queue.shared.type.QueueType
import net.mythicisland.queue.shared.type.SpreadLimit
import net.mythicisland.queue.shared.type.Window

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
        for (key in duplicateKeys(match.rules.map { it.key })) {
            problems += "match.rules must not have more than one rule for key $key"
        }

        match.filters.forEachIndexed { index, filter -> problems += validateFilter("match.filters[$index]", filter) }
        for (key in duplicateKeys(match.filters.map { it.key })) {
            problems += "match.filters must not have more than one filter for key $key"
        }

        val teamBalance = match.quality.teamBalance
        match.quality.spread.forEachIndexed { index, limit -> problems += validateSpread("match.quality.spread[$index]", limit) }
        for (key in duplicateKeys(match.quality.spread.map { it.key })) {
            problems += "match.quality.spread must not have more than one limit for key $key"
        }
        if (teamBalance != null) {
            problems += validateWindow("match.quality.team-balance", teamBalance)
        }
        if (teamBalance != null && balanceBy == null) {
            problems += "match.quality.team-balance needs match.teams.balance-by"
        }

        return problems
    }

    private fun validateSpread(path: String, limit: SpreadLimit): List<String> {
        val problems = mutableListOf<String>()
        if (limit.key.isBlank()) {
            problems += "$path.key must be set"
        }
        return problems + validateWindow(path, limit.window)
    }

    private fun validateFilter(path: String, filter: MatchFilter): List<String> {
        val values = filter.values
        val min = filter.min
        val max = filter.max
        val problems = mutableListOf<String>()

        if (filter.key.isBlank()) {
            problems += "$path.key must be set"
        }
        if (listOf(values != null, min != null || max != null, filter.present != null).count { it } != 1) {
            problems += "$path must set exactly one of values, min and max, or present"
        }
        if (values != null && values.isEmpty()) {
            problems += "$path.values must not be empty"
        }
        if (min != null && !min.isFinite()) {
            problems += "$path.min must be a finite number"
        }
        if (max != null && !max.isFinite()) {
            problems += "$path.max must be a finite number"
        }
        if (min != null && max != null && min > max) {
            problems += "$path.min must not be larger than max"
        }
        return problems
    }

    private fun duplicateKeys(keys: List<String>): Set<String> =
        keys.filter { it.isNotBlank() }.groupingBy { it }.eachCount().filterValues { it > 1 }.keys

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
        return problems + validateWindow(path, rule.window)
    }

    private fun validateWindow(path: String, window: Window): List<String> {
        val start = window.start
        val widen = window.widen
        val every = window.every
        val max = window.max
        val problems = mutableListOf<String>()

        if (start == null) {
            problems += "$path.start must be set"
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
