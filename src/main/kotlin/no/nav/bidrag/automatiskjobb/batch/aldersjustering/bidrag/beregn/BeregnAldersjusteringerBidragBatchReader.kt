package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.beregn

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
class BeregnAldersjusteringerBidragBatchReader(
    barnRepository: BarnRepository,
    private val dataSource: DataSource,
    @Value("#{jobParameters['barn']}") barn: String? = "",
    @Value("#{jobParameters['inkluderBehandlet']}") inkluderBehandlet: Boolean = false,
    @Value("#{jobParameters['inkluderSlettet']}") inkluderSlettet: Boolean = false,
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
                val statusList = mutableListOf("UBEHANDLET", "FEILET", "SIMULERT")
                if (inkluderBehandlet) {
                    statusList.add("BEHANDLET")
                }
                if (inkluderSlettet) {
                    statusList.add("SLETTET")
                }
                val statusString = statusList.joinToString("', '", "'", "'")
                whereClause.append("AND status IN ($statusString)")
                whereClause.append("AND fattet_tidspunkt is null")
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
