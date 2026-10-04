package com.ptero.sentinel.delivery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoundedDeliveryQueueTest {
    @Test
    fun `enqueue evicts oldest message at capacity`() {
        val queue = BoundedDeliveryQueue(capacity = 2)

        assertNull(queue.enqueue(message(1, "one")))
        assertNull(queue.enqueue(message(2, "two")))
        val evicted = queue.enqueue(message(3, "three"))

        assertEquals(message(1, "one"), evicted)
        assertEquals(message(2, "two"), queue.peek())
        assertEquals(2, queue.size())
    }

    @Test
    fun `acknowledge removes all messages through sequence`() {
        val queue = BoundedDeliveryQueue(capacity = 4)
        queue.enqueue(message(1, "one"))
        queue.enqueue(message(2, "two"))
        queue.enqueue(message(3, "three"))

        val removed = queue.acknowledgeThrough(2)

        assertEquals(2, removed)
        assertEquals(message(3, "three"), queue.peek())
    }

    private fun message(sequenceId: Long, jsonLine: String) = DeliveryMessage(
        sessionId = "session",
        sequenceId = sequenceId,
        jsonLine = jsonLine,
    )
}
