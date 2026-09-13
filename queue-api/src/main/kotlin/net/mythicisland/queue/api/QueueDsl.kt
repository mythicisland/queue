package net.mythicisland.queue.api

/**
 * Marks the receivers of the queue DSL.
 */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class QueueDsl
