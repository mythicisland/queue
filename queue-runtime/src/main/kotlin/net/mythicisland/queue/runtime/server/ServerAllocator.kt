package net.mythicisland.queue.runtime.server

import app.simplecloud.api.CloudApi
import app.simplecloud.api.group.ScalingMode
import app.simplecloud.api.server.ServerState
import app.simplecloud.api.server.UpdateServerRequest
import kotlinx.coroutines.future.await
import net.mythicisland.queue.runtime.match.MatchStore
import net.mythicisland.queue.runtime.server.provision.ServersProvisioner
import net.mythicisland.queue.runtime.server.provision.SlotsProvisioner
import net.mythicisland.queue.runtime.type.QueueTypeRegistry
import net.mythicisland.queue.shared.logger.Field
import net.mythicisland.queue.shared.logger.LoggerFactory
import net.mythicisland.queue.shared.match.Match

class ServerAllocator(
    private val api: CloudApi,
    private val types: QueueTypeRegistry,
    private val registry: ServerRegistry,
    private val matchStore: MatchStore,
) {

    private val logger = LoggerFactory.getLogger(ServerAllocator::class)
    private val slotsProvisioner = SlotsProvisioner()
    private val serversProvisioner = ServersProvisioner(api, registry)

    suspend fun recover() {
        val busy = matchStore.all().mapNotNull { it.assignment?.serverId }.toSet()
        for (group in types.groups()) {
            if (isShared(group)) {
                continue
            }

            for (server in registry.byGroup(group)) {
                if (server.state == ServerState.INGAME && server.matches.isEmpty() && server.id !in busy) {
                    logger.info("Freeing orphaned game server", Field("serverId", server.id), Field("server", server.name))
                    setState(server.id, ServerState.INGAME, ServerState.AVAILABLE)
                }
            }
        }
    }

    suspend fun allocateExclusive(group: String): GameServer? {
        val busy = matchStore.all().mapNotNull { it.assignment?.serverId }.toSet()
        val server = chooseExclusive(registry.byGroup(group), busy) ?: return null

        return if (setState(server.id, ServerState.AVAILABLE, ServerState.INGAME)) server else null
    }

    fun offer(group: String, matchId: String, excluded: Set<String>): GameServer? {
        val server = chooseShared(registry.byGroup(group), excluded) ?: return null

        registry.addMatch(server.id, matchId)
        return server
    }

    suspend fun host(match: Match) {
        val assignment = match.assignment ?: return
        registry.hostMatch(assignment.serverId, match.id)
    }

    suspend fun free(match: Match) {
        val assignment = match.assignment ?: return
        registry.removeMatch(assignment.serverId, match.id)
        makeAvailable(assignment.serverId)
    }

    suspend fun release(serverId: String, matchId: String): Boolean {
        if (!registry.removeMatch(serverId, matchId)) {
            return false
        }
        makeAvailable(serverId)
        return true
    }

    suspend fun provision(group: String, servers: Int) {
        val cloudGroup = api.group().getGroupByName(group).await()
        val provisioner = if (cloudGroup.scaling?.scalingMode == ScalingMode.PLAYERS) serversProvisioner else slotsProvisioner
        provisioner.provision(cloudGroup, servers)
    }

    private suspend fun makeAvailable(serverId: String) {
        val server = registry.get(serverId) ?: return
        if (!isShared(server.group) && server.matches.isEmpty()) {
            setState(serverId, ServerState.INGAME, ServerState.AVAILABLE)
        }
    }

    private suspend fun setState(serverId: String, expected: ServerState, state: ServerState): Boolean {
        val current = api.server().getServerById(serverId).await()
        if (ServerRegistry.stateOf(current) != expected) {
            registry.update(current)
            return false
        }

        val request = UpdateServerRequest.builder().state(state).build()
        registry.update(api.server().updateServer(serverId, request).await())
        return registry.get(serverId)?.state == state
    }

    private fun isShared(group: String): Boolean = types.byGroup(group).any { it.server.shared != null }

    companion object {
        private val SHARED_STATES = setOf(ServerState.AVAILABLE, ServerState.INGAME)

        fun chooseExclusive(servers: List<GameServer>, busy: Set<String>): GameServer? =
            servers.firstOrNull { it.state == ServerState.AVAILABLE && it.id !in busy }

        fun chooseShared(servers: List<GameServer>, excluded: Set<String>): GameServer? =
            servers.filter { it.state in SHARED_STATES && it.freeCapacity > 0 && it.id !in excluded }
                .maxByOrNull { it.matches.size }

    }

}
