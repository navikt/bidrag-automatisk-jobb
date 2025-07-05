package no.nav.bidrag.automatiskjobb.batch.generelt.oppdaterbarn

import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.PAGE_SIZE
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.rowmapper.BarnRowMapper
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.database.JdbcPagingItemReader
import org.springframework.batch.item.database.Order
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
@StepScope
class OppdaterBarnBatchReader(
    private val dataSource: DataSource,
    @Value("#{jobParameters['barn']}") barn: String? = "",
    @Value("#{jobParameters['dager'] ?: 1}") private val dager: Int,
) : JdbcPagingItemReader<Barn>() {
    init {
        val barnListe =
            barn
                ?.takeIf { it.isNotEmpty() }
                ?.split(",")
                ?.map { it.trim() }
                ?.map { Integer.valueOf(it) } ?: emptyList()
        val whereClause = StringBuilder()
        val parameterValues = HashMap<String, Any>()

        if (barnListe.isEmpty()) {
            whereClause.append("(TRUE")
        } else {
            whereClause.append("(id IN (:barnIds)")
            parameterValues["barnIds"] = barnListe
        }
        whereClause.append(")")
        whereClause.append(" AND (oppdatert IS NULL OR oppdatert < (NOW() - MAKE_INTERVAL(days => :dager)))")
        parameterValues["dager"] = dager

        whereClause.append(" AND fodselsdato >= NOW() - INTERVAL '18 year'")

        val sqlPagingQuaryPoviderFactoryBean =
            SqlPagingQueryProviderFactoryBean().apply {
                setDataSource(dataSource)
                setSelectClause("SELECT *")
                setFromClause("FROM barn")
                setWhereClause(whereClause.toString())
                setSortKeys(mapOf("id" to Order.ASCENDING))
            }
        try {
            this.setQueryProvider(sqlPagingQuaryPoviderFactoryBean.`object`)
            this.pageSize = PAGE_SIZE
            this.setFetchSize(PAGE_SIZE)
            this.setDataSource(dataSource)
            this.setRowMapper(BarnRowMapper())
            this.setParameterValues(parameterValues)
        } catch (e: Exception) {
            throw RuntimeException("Failed to create JdbcPagingItemReader", e)
        }
    }
}
