package no.nav.bidrag.automatiskjobb.batch.utils.forsendelse.opprett

import no.nav.bidrag.automatiskjobb.batch.utils.BatchConfiguration.Companion.PAGE_SIZE
import no.nav.bidrag.automatiskjobb.persistence.entity.ForsendelseBestilling
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import no.nav.bidrag.automatiskjobb.persistence.rowmapper.ForsendelseBestillingRowMapper
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.infrastructure.item.database.JdbcPagingItemReader
import org.springframework.batch.infrastructure.item.database.Order
import org.springframework.batch.infrastructure.item.database.support.SqlPagingQueryProviderFactoryBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
@StepScope
class OpprettForsendelseBatchReader(
    private val dataSource: DataSource,
    barnRepository: BarnRepository,
    @Value("#{jobParameters['bestillingIds']}") bestillingIds: String? = null,
) : JdbcPagingItemReader<ForsendelseBestilling>(
        dataSource,
        SqlPagingQueryProviderFactoryBean()
            .apply {
                setDataSource(dataSource)
                setSelectClause("SELECT *")
                setFromClause("FROM forsendelse_bestilling")
                val idListe =
                    bestillingIds
                        ?.takeIf { it.isNotEmpty() }
                        ?.split(",")
                        ?.map { it.trim().toInt() } ?: emptyList()
                val whereClause =
                    if (idListe.isNotEmpty()) {
                        "WHERE id IN (:bestillingIds) AND slettet_tidspunkt IS NULL AND skal_slettes = false"
                    } else {
                        "WHERE slettet_tidspunkt IS NULL AND forsendelse_id IS NULL AND skal_slettes = false"
                    }
                setWhereClause(whereClause)
                setSortKeys(mapOf("id" to Order.ASCENDING))
            }.`object`,
    ) {
    init {
        val idListe =
            bestillingIds
                ?.takeIf { it.isNotEmpty() }
                ?.split(",")
                ?.map { it.trim().toInt() } ?: emptyList()
        val parameterValues = HashMap<String, Any>()
        if (idListe.isNotEmpty()) {
            parameterValues["bestillingIds"] = idListe
        }
        try {
            this.pageSize = PAGE_SIZE
            this.setFetchSize(PAGE_SIZE)
            this.setRowMapper(ForsendelseBestillingRowMapper(barnRepository))
            if (parameterValues.isNotEmpty()) {
                this.setParameterValues(parameterValues)
            }
            this.isSaveState = false
        } catch (e: Exception) {
            throw RuntimeException("Failed to create JdbcPagingItemReader", e)
        }
    }
}
