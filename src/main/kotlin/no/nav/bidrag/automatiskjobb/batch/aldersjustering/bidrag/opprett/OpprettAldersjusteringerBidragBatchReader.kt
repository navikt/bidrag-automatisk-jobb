package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.opprett

import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.PAGE_SIZE
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import no.nav.bidrag.automatiskjobb.persistence.rowmapper.BarnRowMapper
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.database.JdbcPagingItemReader
import org.springframework.batch.item.database.Order
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDate
import javax.sql.DataSource

@Component
@StepScope
class OpprettAldersjusteringerBidragBatchReader(
    private val dataSource: DataSource,
    @Value("#{jobParameters['aldersjusteringsdato']}") aldersjusteringsdato: LocalDate? =
        LocalDate
            .now()
            .withMonth(7)
            .withDayOfMonth(1),
    @Value("#{jobParameters['år']}") år: Long? = -1,
    barnRepository: BarnRepository,
) : JdbcPagingItemReader<Barn>() {
    init {
        val sqlPagingQuaryPoviderFactoryBean =
            SqlPagingQueryProviderFactoryBean().apply {
                setDataSource(dataSource)
                setSelectClause("SELECT *")
                setFromClause("FROM barn")
                setWhereClause(
                    """
                    WHERE :år - EXTRACT(YEAR FROM fodselsdato) in (6, 11, 15)
                    AND bidrag_fra <= :aldersjusteringsdato
                    AND (bidrag_til IS NULL OR bidrag_til > :aldersjusteringsdato)
                    """.trimMargin(),
                )
                setSortKeys(mapOf("id" to Order.ASCENDING))
            }
        try {
            this.setQueryProvider(sqlPagingQuaryPoviderFactoryBean.`object`)
            this.pageSize = PAGE_SIZE
            this.setFetchSize(PAGE_SIZE)
            this.setDataSource(dataSource)
            this.setParameterValues(
                mapOf(
                    "år" to år,
                    "aldersjusteringsdato" to aldersjusteringsdato,
                ),
            )
            this.setRowMapper(BarnRowMapper())
        } catch (e: Exception) {
            throw RuntimeException("Failed to create JdbcPagingItemReader", e)
        }
    }
}
