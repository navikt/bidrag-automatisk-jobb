package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.oppgave.slettoppgave

import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.PAGE_SIZE
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import no.nav.bidrag.automatiskjobb.persistence.rowmapper.AlderjusteringRowMapper
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.database.JdbcPagingItemReader
import org.springframework.batch.item.database.Order
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
@StepScope
class SlettOppgaveAldersjusteringerBidragBatchReader(
    private val dataSource: DataSource,
    barnRepository: BarnRepository,
    @Value("#{jobParameters['barn']}") barn: String? = "",
    @Value("#{jobParameters['batchId']}") batchId: String,
) : JdbcPagingItemReader<Aldersjustering>() {
    init {
        val barnListe =
            barn
                ?.takeIf { it.isNotEmpty() }
                ?.split(",")
                ?.map { it.trim() }
                ?.map { Integer.valueOf(it) } ?: emptyList()
        val whereClause = StringBuilder()
        val parameterValues = HashMap<String, Any>()

        parameterValues["batchId"] = batchId
        if (barnListe.isEmpty()) {
            whereClause.append("(TRUE")
        } else {
            whereClause.append("(barn_id IN (:barnIds)")
            parameterValues["barnIds"] = barnListe
        }
        whereClause.append(")")

        whereClause.append(" AND batch_id = :batchId")
        whereClause.append(" AND oppgave IS NOT NULL")

        val sqlPagingQuaryPoviderFactoryBean =
            SqlPagingQueryProviderFactoryBean().apply {
                setDataSource(dataSource)
                setSelectClause("SELECT *")
                setFromClause("FROM aldersjustering")
                setWhereClause(whereClause.toString())
                setSortKeys(mapOf("id" to Order.ASCENDING))
            }

        this.setPageSize(PAGE_SIZE)
        try {
            this.setQueryProvider(sqlPagingQuaryPoviderFactoryBean.`object`)
            this.pageSize = PAGE_SIZE
            this.setFetchSize(PAGE_SIZE)
            this.setDataSource(dataSource)
            this.setRowMapper(AlderjusteringRowMapper(barnRepository))
            this.setParameterValues(parameterValues)
        } catch (e: Exception) {
            throw RuntimeException("Failed to create JdbcPagingItemReader", e)
        }
    }
}
