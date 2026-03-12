package net.mythicisland.queue.runtime.config.reactive

data class ReactiveConfigInfo<T>(
    val clazz: Class<T>,
    val reactiveConfig: ReactiveConfig<T>
)