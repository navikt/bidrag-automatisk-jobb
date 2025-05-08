package no.nav.bidrag.automatiskjobb.batch.common

import org.springframework.batch.core.partition.support.Partitioner
import org.springframework.batch.item.ExecutionContext
import org.springframework.stereotype.Component

/**
 * A generic partitioner that assigns items to partitions based on their modulo value.
 * This ensures that each partition processes a different subset of items.
 */
@Component
class ModuloPartitioner : Partitioner {
    override fun partition(gridSize: Int): Map<String, ExecutionContext> {
        val partitions = mutableMapOf<String, ExecutionContext>()

        for (i in 0 until gridSize) {
            val executionContext = ExecutionContext()
            executionContext.putInt("partitionNumber", i)
            executionContext.putInt("gridSize", gridSize)
            partitions["partition$i"] = executionContext
        }

        return partitions
    }
}
