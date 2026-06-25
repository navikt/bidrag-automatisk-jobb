package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.beregn

import no.nav.bidrag.automatiskjobb.batch.utils.BatchConfiguration.Companion.PAGE_SIZE
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
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
    @Value("#{jobParameters['statuser']}") statuser: String? = "UBEHANDLET,FEILET,SIMULERT",
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
                val statusListe = parseStatuser(statuser)
                val statusString = statusListe.joinToString("', '", "'", "'") { it.name }
                whereClause.append(" AND status IN ($statusString)")
                whereClause.append(" AND fattet_tidspunkt is null")
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

    private companion object {
        val DEFAULT_STATUSER = listOf(Status.UBEHANDLET, Status.FEILET, Status.SIMULERT)

        fun parseStatuser(statuser: String?): List<Status> {
            if (statuser.isNullOrBlank()) return DEFAULT_STATUSER
            val navn = statuser.split(",").map { it.trim() }.filter { it.isNotBlank() }
            if (navn.isEmpty()) return DEFAULT_STATUSER
            return navn.map {
                try {
                    Status.valueOf(it.uppercase())
                } catch (_: IllegalArgumentException) {
                    throw IllegalArgumentException(
                        "Ugyldig status '$it'. Gyldige verdier er: ${Status.entries.joinToString { s -> s.name }}.",
                    )
                }
            }
        }
    }
}
