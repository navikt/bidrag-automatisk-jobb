package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.forsendelse.distribuer

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
class DistribuerForsendelseAldersjusteringerBidragBatchReader(
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
                        "AND forsendelse_opprettet_tidspunkt IS NOT NULL " +
                        "AND slettet_tidspunkt IS NULL and skal_slettes = false " +
                        "AND distribuert_tidspunkt IS NULL",
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
