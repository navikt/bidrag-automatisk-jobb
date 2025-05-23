package no.nav.bidrag.automatiskjobb.batch.aldersjustering.slettvedtaksforslag

import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.PAGE_SIZE
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import no.nav.bidrag.automatiskjobb.persistence.rowmapper.AlderjusteringRowMapper
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.database.JdbcPagingItemReader
import org.springframework.batch.item.database.Order
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
@StepScope
class SlettVedtaksforslagBatchReader(
    private val dataSource: DataSource,
    barnRepository: BarnRepository,
) : JdbcPagingItemReader<Aldersjustering>() {
    init {
        val sqlPagingQuaryPoviderFactoryBean =
            SqlPagingQueryProviderFactoryBean().apply {
                setDataSource(dataSource)
                setSelectClause("SELECT *")
                setFromClause("FROM aldersjustering")
                setWhereClause("WHERE status = 'SLETTES'")
                setSortKeys(mapOf("id" to Order.ASCENDING))
            }
        try {
            this.setQueryProvider(sqlPagingQuaryPoviderFactoryBean.`object`)
            this.pageSize = PAGE_SIZE
            this.setFetchSize(PAGE_SIZE)
            this.setDataSource(dataSource)
            this.isSaveState = true
            this.setRowMapper(AlderjusteringRowMapper(barnRepository))
        } catch (e: Exception) {
            throw RuntimeException("Failed to create JdbcPagingItemReader", e)
        }
    }
}
