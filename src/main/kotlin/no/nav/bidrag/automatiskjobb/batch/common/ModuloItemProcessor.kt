package no.nav.bidrag.automatiskjobb.batch.common

import no.nav.bidrag.automatiskjobb.persistence.entity.EntityObject
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.data.RepositoryItemReader
import org.springframework.transaction.PlatformTransactionManager

open class ModuloItemProcessor<T : EntityObject>(
    private val partitionNumber: Int?,
    private val gridSize: Int?,
    private val transactionManager: PlatformTransactionManager,
) : RepositoryItemReader<T>(),
    ItemProcessor<T, T> {
    override fun process(item: T): T? {
        // If not in a partitioned context, process everything
        if (partitionNumber == null || gridSize == null) {
            return item
        }

        // Only process items where id % gridSize == partitionNumber
        return if (item.id!! % gridSize == partitionNumber) {
            item
        } else {
            null // Skip this item in this partition
        }
    }
}
