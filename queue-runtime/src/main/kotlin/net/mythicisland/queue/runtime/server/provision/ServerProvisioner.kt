package net.mythicisland.queue.runtime.server.provision

import app.simplecloud.api.group.Group

interface ServerProvisioner {

    suspend fun provision(group: Group, servers: Int)

}
