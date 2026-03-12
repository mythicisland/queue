package net.mythicisland.queue.plugin.config.reactive

data class ReactiveConfigInfo<T>(
    val clazz: Class<T>,
    val reactiveConfig: ReactiveConfig<T>
)