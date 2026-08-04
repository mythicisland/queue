package net.mythicisland.queue.api

/**
 * Marks the receivers of the queue DSL.
 *
 * Keeps a nested block from accidentally calling into the enclosing builder,
 * so every call inside a block belongs to that block.
 */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class QueueDsl
