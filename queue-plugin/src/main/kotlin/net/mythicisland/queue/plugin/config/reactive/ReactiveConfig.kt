package net.mythicisland.queue.plugin.config.reactive

import net.mythicisland.queue.plugin.config.YamlConfig

class ReactiveConfig<T>(
    val config: YamlConfig,
    val path: String?,
    val clazz: Class<T>
) {

    @Volatile
    private var currentValue: T? = config.loadDirect(path, clazz)

    init {
        config.registerReactiveConfig(path, clazz, this)
    }

    fun get(): T = currentValue?: throw NullPointerException("Reactive config is not initialized")

    internal fun update(newValue: T?) {
        currentValue = newValue
    }

    fun reload() {
        val newValue = config.loadDirect(path, clazz)
        update(newValue)
    }

}