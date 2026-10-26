package net.mythicisland.queue.runtime.server.provision

import app.simplecloud.api.group.Group

class SlotsProvisioner : ServerProvisioner {

    override suspend fun provision(group: Group, servers: Int) = Unit

}
