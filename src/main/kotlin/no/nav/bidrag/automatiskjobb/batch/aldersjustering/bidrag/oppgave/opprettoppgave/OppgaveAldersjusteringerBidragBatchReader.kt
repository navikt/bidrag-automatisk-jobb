package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.oppgave.opprettoppgave

import no.nav.bidrag.automatiskjobb.batch.utils.BatchConfiguration.Companion.PAGE_SIZE
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import no.nav.bidrag.automatiskjobb.persistence.rowmapper.AlderjusteringRowMapper
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.infrastructure.item.database.JdbcPagingItemReader
import org.springframework.batch.infrastructure.item.database.Order
import org.springframework.batch.infrastructure.item.database.support.SqlPagingQueryProviderFactoryBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
@StepScope
class OppgaveAldersjusteringerBidragBatchReader(
    barnRepository: BarnRepository,
    private val dataSource: DataSource,
    @Value("#{jobParameters['barn']}") barn: String? = "",
) : JdbcPagingItemReader<Aldersjustering>(
        dataSource,
        SqlPagingQueryProviderFactoryBean()
            .apply {
                setDataSource(dataSource)
                setSelectClause("SELECT *")
                setFromClause("FROM aldersjustering")
                val barnListe =
                    barn
                        ?.takeIf { it.isNotEmpty() }
                        ?.split(",")
                        ?.map { it.trim() }
                        ?.map { Integer.valueOf(it) } ?: emptyList()
                val whereClause = StringBuilder()
                if (barnListe.isEmpty()) {
                    whereClause.append("(TRUE")
                } else {
                    whereClause.append("(barn_id IN (:barnIds)")
                }
                whereClause.append(")")
                whereClause.append(" AND behandlingstype = 'MANUELL'")
                whereClause.append(" AND status = 'BEHANDLET'")
                whereClause.append(" AND oppgave IS NULL")
                setWhereClause(whereClause.toString())
                setSortKeys(mapOf("id" to Order.ASCENDING))
            }.`object`,
    ) {
    init {
        val barnListe =
            barn
                ?.takeIf { it.isNotEmpty() }
                ?.split(",")
                ?.map { it.trim() }
                ?.map { Integer.valueOf(it) } ?: emptyList()
        val parameterValues = HashMap<String, Any>()
        if (barnListe.isNotEmpty()) {
            parameterValues["barnIds"] = barnListe
        }
        try {
            this.pageSize = PAGE_SIZE
            this.setFetchSize(PAGE_SIZE)
            this.setRowMapper(AlderjusteringRowMapper(barnRepository))
            this.setParameterValues(parameterValues)
            this.isSaveState = false
        } catch (e: Exception) {
            throw RuntimeException("Failed to create JdbcPagingItemReader", e)
        }
    }
}
