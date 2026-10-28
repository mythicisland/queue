package net.mythicisland.queue.runtime.leader

data class Lease(val holder: String, val address: String) {

    fun encode(): ByteArray = "$holder\n$address".toByteArray(Charsets.UTF_8)

    companion object {
        fun decode(bytes: ByteArray): Lease? {
            val parts = bytes.toString(Charsets.UTF_8).split('\n')
            if (parts.size != 2) {
                return null
            }
            return Lease(parts[0], parts[1])
        }
    }

}
