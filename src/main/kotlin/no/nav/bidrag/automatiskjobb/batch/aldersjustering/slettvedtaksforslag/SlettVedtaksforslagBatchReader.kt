package no.nav.bidrag.automatiskjobb.batch.aldersjustering.slettvedtaksforslag

import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.database.JdbcPagingItemReader
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
@StepScope
class SlettVedtaksforslagBatchReader(
    private val dataSource: DataSource,
) : JdbcPagingItemReader<Aldersjustering>() {
    init {
        val sqlPagingQuaryPoviderFactoryBean =
            SqlPagingQueryProviderFactoryBean().apply {
                setDataSource(dataSource)
                setSelectClause("SELECT *")
                setFromClause("FROM aldersjustering")
                setWhereClause("WHERE status LIKE 'SLETTES'")
                setSortKey("id")
            }
        try {
            this.setQueryProvider(sqlPagingQuaryPoviderFactoryBean.`object`)
            this.pageSize = 100
            this.setDataSource(dataSource)
            this.setRowMapper(BeanPropertyRowMapper(Aldersjustering::class.java))
        } catch (e: Exception) {
            throw RuntimeException("Failed to create JdbcPagingItemReader", e)
        }
    }
}
