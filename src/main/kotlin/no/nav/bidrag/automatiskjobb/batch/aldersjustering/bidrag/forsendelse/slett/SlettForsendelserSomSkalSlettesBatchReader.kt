package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.forsendelse.slett

import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.PAGE_SIZE
import no.nav.bidrag.automatiskjobb.persistence.entity.ForsendelseBestilling
import no.nav.bidrag.automatiskjobb.persistence.repository.AldersjusteringRepository
import no.nav.bidrag.automatiskjobb.persistence.rowmapper.ForsendelseBestillingRowMapper
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.database.JdbcPagingItemReader
import org.springframework.batch.item.database.Order
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
@StepScope
class SlettForsendelserSomSkalSlettesBatchReader(
    private val dataSource: DataSource,
    aldersjusteringRepository: AldersjusteringRepository,
) : JdbcPagingItemReader<ForsendelseBestilling>() {
    init {
        val sqlPagingQuaryPoviderFactoryBean =
            SqlPagingQueryProviderFactoryBean().apply {
                setDataSource(dataSource)
                setSelectClause("SELECT *")
                setFromClause("FROM forsendelse_bestilling")
                setWhereClause(
                    "WHERE forsendelse_id IS NOT NULL " +
                        "AND skal_slettes = true " +
                        "AND distribuert_tidspunkt IS NULL " +
                        "AND journalpost_id IS NULL " +
                        "AND slettet_tidspunkt IS NULL ",
                )
                setSortKeys(mapOf("id" to Order.ASCENDING))
            }
        try {
            this.setQueryProvider(sqlPagingQuaryPoviderFactoryBean.`object`)
            this.pageSize = PAGE_SIZE
            this.setFetchSize(PAGE_SIZE)
            this.setDataSource(dataSource)
            this.setRowMapper(ForsendelseBestillingRowMapper(aldersjusteringRepository))
        } catch (e: Exception) {
            throw RuntimeException("Failed to create JdbcPagingItemReader", e)
        }
    }
}
