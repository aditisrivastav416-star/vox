package com.example.vox.domain.model

/** Pure split math in minor units. Shares always sum exactly to the input total. */
object SplitCalculator {

    /** Leftover minor units (e.g. 1 paisa) go to the first participants. */
    fun equal(totalMinor: Long, participantIds: List<String>): Map<String, Long> {
        if (participantIds.isEmpty()) return emptyMap()
        val base = totalMinor / participantIds.size
        val remainder = totalMinor % participantIds.size
        return participantIds.withIndex().associate { (i, id) -> id to base + if (i < remainder) 1 else 0 }
    }

    /**
     * Each item is split equally between its assignees; [taxMinor] is spread
     * proportionally to everyone's item subtotal.
     */
    fun byItems(items: List<Pair<Long, List<String>>>, taxMinor: Long): Map<String, Long> {
        val base = linkedMapOf<String, Long>()
        items.forEach { (amount, assignees) ->
            equal(amount, assignees).forEach { (id, share) -> base[id] = (base[id] ?: 0) + share }
        }
        val subtotal = base.values.sum()
        if (subtotal == 0L) return base
        val taxShares = base.mapValues { (_, share) -> taxMinor * share / subtotal }
        var leftover = taxMinor - taxShares.values.sum()
        return base.mapValues { (id, share) ->
            val extra = if (leftover > 0) 1L.also { leftover-- } else 0L
            share + taxShares.getValue(id) + extra
        }
    }
}
