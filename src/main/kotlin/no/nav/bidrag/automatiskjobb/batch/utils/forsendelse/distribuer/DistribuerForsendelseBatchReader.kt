package no.nav.bidrag.automatiskjobb.batch.utils.forsendelse.distribuer

import no.nav.bidrag.automatiskjobb.batch.utils.BatchConfiguration.Companion.PAGE_SIZE
import no.nav.bidrag.automatiskjobb.persistence.entity.ForsendelseBestilling
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import no.nav.bidrag.automatiskjobb.persistence.rowmapper.ForsendelseBestillingRowMapper
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.infrastructure.item.database.JdbcPagingItemReader
import org.springframework.batch.infrastructure.item.database.Order
import org.springframework.batch.infrastructure.item.database.support.SqlPagingQueryProviderFactoryBean
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
@StepScope
class DistribuerForsendelseBatchReader(
    private val dataSource: DataSource,
    barnRepository: BarnRepository,
) : JdbcPagingItemReader<ForsendelseBestilling>(
        dataSource,
        SqlPagingQueryProviderFactoryBean()
            .apply {
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
            }.`object`,
    ) {
    init {
        try {
            this.pageSize = PAGE_SIZE
            this.setFetchSize(PAGE_SIZE)
            this.setRowMapper(ForsendelseBestillingRowMapper(barnRepository))
            this.isSaveState = false
        } catch (e: Exception) {
            throw RuntimeException("Failed to create JdbcPagingItemReader", e)
        }
    }
}
