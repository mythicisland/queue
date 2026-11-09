package net.mythicisland.queue.runtime.match

import net.mythicisland.queue.shared.match.Team
import net.mythicisland.queue.shared.ticket.Ticket
import net.mythicisland.queue.shared.type.MatchRule
import net.mythicisland.queue.shared.type.MatchRuleType
import net.mythicisland.queue.shared.type.QueueType
import java.time.Duration
import java.time.Instant
import kotlin.math.abs

object TicketSelector {

    fun select(type: QueueType, searching: List<Ticket>, now: Instant): TicketSelection? {
        val sorted = searching.filter { it.isSearchingIn(type, now) }.sortedBy { it.createdAt }
        val rules = type.match.rules
        val anchors = if (rules.isEmpty()) sorted.take(1) else sorted

        return anchors.firstNotNullOfOrNull { anchor ->
            val waited = Duration.between(anchor.createdAt, now)
            val candidates = sorted.filter { it != anchor && allows(rules, anchor, it, waited) }
            selectFrom(type, listOf(anchor) + candidates, now)
        }
    }

    private fun selectFrom(type: QueueType, candidates: List<Ticket>, now: Instant): TicketSelection? {
        val config = type.match
        val selected = mutableListOf<Ticket>()
        val teams = mutableListOf<Team>()

        for (ticket in candidates) {
            if (players(selected) == config.effectiveMaxPlayers) {
                break
            }
            val candidate = selected + ticket
            if (players(candidate) > config.effectiveMaxPlayers) {
                continue
            }
            val assigned = TeamAssigner.assign(type, candidate) ?: continue
            selected.add(ticket)
            teams.clear()
            teams.addAll(assigned)
        }

        val players = players(selected)
        if (players < config.minPlayers) {
            return null
        }
        val selection = TicketSelection(selected.toList(), teams.toList())
        if (players == config.effectiveMaxPlayers) {
            return selection
        }

        val playerCounts = selected.runningFold(0) { sum, ticket -> sum + ticket.getPlayerCount() }.drop(1)
        val reachedMinWith = selected.take(playerCounts.indexOfFirst { it >= config.minPlayers } + 1)
        val reachedMinAt = reachedMinWith.maxOf { it.createdAt }

        return if (now.isBefore(reachedMinAt.plus(config.fillTime))) null else selection
    }

    private fun allows(rules: List<MatchRule>, anchor: Ticket, candidate: Ticket, waited: Duration): Boolean =
        rules.all { rule -> allows(rule, anchor, candidate, waited) }

    private fun allows(rule: MatchRule, anchor: Ticket, candidate: Ticket, waited: Duration): Boolean =
        when (rule.type) {
            MatchRuleType.EQUAL -> isRelaxed(rule, waited) || anchor.properties[rule.key] == candidate.properties[rule.key]
            MatchRuleType.RANGE -> isInRange(rule, anchor, candidate, waited)
            null -> false
        }

    private fun isRelaxed(rule: MatchRule, waited: Duration): Boolean {
        val relaxAfter = rule.relaxAfter ?: return false
        return waited >= relaxAfter
    }

    private fun isInRange(rule: MatchRule, anchor: Ticket, candidate: Ticket, waited: Duration): Boolean {
        val anchorValue = anchor.properties[rule.key]?.toDoubleOrNull() ?: return false
        val candidateValue = candidate.properties[rule.key]?.toDoubleOrNull() ?: return false
        return abs(anchorValue - candidateValue) <= window(rule, waited)
    }

    private fun window(rule: MatchRule, waited: Duration): Double {
        val start = rule.start ?: 0.0
        val widen = rule.widen ?: 0.0
        val every = rule.every
        if (every == null || !every.isPositive || widen == 0.0 || waited.isNegative) {
            return start
        }

        val grown = start + widen * (waited.toMillis() / every.toMillis())
        val max = rule.max ?: return grown
        return minOf(grown, max)
    }

    private fun players(tickets: List<Ticket>): Int = tickets.sumOf { it.getPlayerCount() }

}
