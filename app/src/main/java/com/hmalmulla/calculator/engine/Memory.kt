package com.hmalmulla.calculator.engine

import java.math.BigDecimal

/**
 * Calculator memory with multiple slots. Slot 0 is the main memory used by
 * MC / MR / M+ / M− / MS; MS adds a new slot at the top (up to [maxSlots]).
 */
class MemoryStore(initial: List<BigDecimal> = emptyList(), private val maxSlots: Int = 10) {
    private val slots = ArrayList(initial.take(maxSlots))

    val values: List<BigDecimal> get() = slots.toList()
    val hasValue: Boolean get() = slots.isNotEmpty()

    /** MC: clears all memory. */
    fun clear() = slots.clear()

    /** MR: the main memory value (0 when empty). */
    fun recall(index: Int = 0): BigDecimal = slots.getOrNull(index) ?: BigDecimal.ZERO

    /** MS: stores a value as a new slot. */
    fun store(v: BigDecimal) {
        slots.add(0, v)
        while (slots.size > maxSlots) slots.removeAt(slots.size - 1)
    }

    /** M+: adds to the main memory (creating it when empty). */
    fun add(v: BigDecimal) {
        if (slots.isEmpty()) slots.add(v) else slots[0] = slots[0].add(v)
    }

    /** M−: subtracts from the main memory. */
    fun subtract(v: BigDecimal) {
        if (slots.isEmpty()) slots.add(v.negate()) else slots[0] = slots[0].subtract(v)
    }

    fun remove(index: Int) {
        if (index in slots.indices) slots.removeAt(index)
    }
}
