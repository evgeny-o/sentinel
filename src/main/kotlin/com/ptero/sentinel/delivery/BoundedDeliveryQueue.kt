package com.ptero.sentinel.delivery

data class DeliveryMessage(
    val sessionId: String,
    val sequenceId: Long,
    val jsonLine: String,
)

class BoundedDeliveryQueue(
    private val capacity: Int,
) {
    private val messages = ArrayDeque<DeliveryMessage>(capacity)

    init {
        require(capacity > 0) { "Queue capacity must be positive" }
    }

    @Synchronized
    fun enqueue(message: DeliveryMessage): DeliveryMessage? {
        val evicted = if (messages.size == capacity) {
            messages.removeFirst()
        } else {
            null
        }
        messages.addLast(message)
        return evicted
    }

    @Synchronized
    fun peek(): DeliveryMessage? = messages.firstOrNull()

    @Synchronized
    fun acknowledgeThrough(sequenceId: Long): Int {
        var removed = 0
        while (messages.firstOrNull()?.sequenceId?.let { it <= sequenceId } == true) {
            messages.removeFirst()
            removed += 1
        }
        return removed
    }

    @Synchronized
    fun size(): Int = messages.size
}
