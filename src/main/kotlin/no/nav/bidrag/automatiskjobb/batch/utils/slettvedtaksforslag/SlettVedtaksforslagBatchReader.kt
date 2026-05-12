package no.nav.bidrag.automatiskjobb.batch.utils.slettvedtaksforslag

import no.nav.bidrag.automatiskjobb.batch.utils.BatchConfiguration.Companion.PAGE_SIZE
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import no.nav.bidrag.automatiskjobb.persistence.rowmapper.AlderjusteringRowMapper
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.infrastructure.item.database.JdbcPagingItemReader
import org.springframework.batch.infrastructure.item.database.Order
import org.springframework.batch.infrastructure.item.database.support.SqlPagingQueryProviderFactoryBean
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
@StepScope
class SlettVedtaksforslagBatchReader(
    private val dataSource: DataSource,
    barnRepository: BarnRepository,
) : JdbcPagingItemReader<Aldersjustering>(
        dataSource,
        SqlPagingQueryProviderFactoryBean()
            .apply {
                setDataSource(dataSource)
                setSelectClause("SELECT *")
                setFromClause("FROM aldersjustering")
                setWhereClause("WHERE status = 'SLETTES'")
                setSortKeys(mapOf("id" to Order.ASCENDING))
            }.`object`,
    ) {
    init {
        try {
            this.pageSize = PAGE_SIZE
            this.setFetchSize(PAGE_SIZE)
            this.isSaveState = false
            this.setRowMapper(AlderjusteringRowMapper(barnRepository))
        } catch (e: Exception) {
            throw RuntimeException("Failed to create JdbcPagingItemReader", e)
        }
    }
}
