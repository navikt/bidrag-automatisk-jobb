package no.nav.bidrag.automatiskjobb.batch

import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.entity.Behandlingstype
import no.nav.bidrag.automatiskjobb.persistence.entity.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet

class AlderjusteringRowMapper(
    private val barnRepository: BarnRepository,
) : RowMapper<Aldersjustering> {
    override fun mapRow(
        rs: ResultSet,
        rowNum: Int,
    ): Aldersjustering =
        Aldersjustering(
            id = rs.getInt("id"),
            batchId = rs.getString("batch_id"),
            vedtaksidBeregning = rs.getInt("vedtaksid_beregning"),
            barn = barnRepository.findById(rs.getInt("barn_id")).get(),
            aldersgruppe = rs.getInt("aldersgruppe"),
            lopendeBelop = rs.getBigDecimal("lopende_belop"),
            begrunnelse = (rs.getArray("begrunnelse")?.array as? Array<*>)?.map { it as String } ?: emptyList(),
            status = Status.valueOf(rs.getString("status")),
            behandlingstype = rs.getString("behandlingstype")?.let { Behandlingstype.valueOf(rs.getString("behandlingstype")) },
            vedtak = rs.getInt("vedtak"),
            oppgave = rs.getInt("oppgave"),
            opprettetTidspunkt = rs.getTimestamp("opprettet_tidspunkt"),
            fattetTidspunkt = rs.getTimestamp("fattet_tidspunkt"),
            stønadstype = Stønadstype.valueOf(rs.getString("stonadstype")),
            resultatSisteVedtak = rs.getString("resultat_siste_vedtak"),
        )
}
