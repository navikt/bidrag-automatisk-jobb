package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.fattvedtak

import no.nav.bidrag.automatiskjobb.batch.utils.BatchConfiguration.Companion.PAGE_SIZE
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Behandlingstype
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
class FattVedtakOmAldersjusteringerBidragBatchReader(
    private val dataSource: DataSource,
    barnRepository: BarnRepository,
    @Value("#{jobParameters['barn']}") barn: String? = "",
    @Value("#{jobParameters['behandlingstyper']}") behandlingstyper: String? = "MANUELL,FATTET_FORSLAG,INGEN",
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
                val parameterValues = HashMap<String, Any>()
                val behandlingstypeListe = parseBehandlingstyper(behandlingstyper)

                if (barnListe.isEmpty()) {
                    whereClause.append("(TRUE")
                } else {
                    whereClause.append("(barn_id IN (:barnIds)")
                    parameterValues["barnIds"] = barnListe
                }
                whereClause.append(")")

                whereClause.append(" AND status = 'BEHANDLET'")
                whereClause.append(" AND behandlingstype IN (${behandlingstypeListe.joinToString(",") { "'$it'" }})")
                whereClause.append(" AND vedtak IS NOT NULL")
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
        fun parseBehandlingstyper(behandlingstyper: String?): List<Behandlingstype> {
            val defaultBehandlingstyper =
                listOf(
                    Behandlingstype.MANUELL,
                    Behandlingstype.FATTET_FORSLAG,
                    Behandlingstype.INGEN,
                )

            if (behandlingstyper.isNullOrBlank()) {
                return defaultBehandlingstyper
            }

            val behandlingstypeNavn =
                behandlingstyper
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

            if (behandlingstypeNavn.isEmpty()) {
                return defaultBehandlingstyper
            }

            return behandlingstypeNavn.map {
                try {
                    Behandlingstype.valueOf(it.uppercase())
                } catch (_: IllegalArgumentException) {
                    throw IllegalArgumentException("Ugyldig behandlingstype '$it'. Gyldige verdier er MANUELL, FATTET_FORSLAG, INGEN.")
                }
            }
        }
    }
}
