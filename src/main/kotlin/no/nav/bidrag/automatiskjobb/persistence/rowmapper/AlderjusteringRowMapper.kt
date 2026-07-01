package no.nav.bidrag.automatiskjobb.persistence.rowmapper

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Behandlingstype
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.persistence.entity.metadata.AldersjusteringMetadata
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.transport.felles.commonObjectmapper
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
            id = rs.getIntOrNull("id"),
            batchId = rs.getString("batch_id"),
            vedtaksidBeregning = rs.getIntOrNull("vedtaksid_beregning"),
            barn = barnRepository.findById(rs.getInt("barn_id")).get(),
            aldersgruppe = rs.getInt("aldersgruppe"),
            lopendeBelop = rs.getBigDecimal("lopende_belop"),
            begrunnelse = (rs.getArray("begrunnelse")?.array as? Array<*>)?.map { it as String } ?: emptyList(),
            status = Status.valueOf(rs.getString("status")),
            behandlingstype = rs.getString("behandlingstype")?.let { Behandlingstype.valueOf(rs.getString("behandlingstype")) },
            vedtak = rs.getIntOrNull("vedtak"),
            oppgave = rs.getIntOrNull("oppgave"),
            opprettetTidspunkt = rs.getTimestamp("opprettet_tidspunkt"),
            fattetTidspunkt = rs.getTimestamp("fattet_tidspunkt"),
            b4Beløp = rs.getBigDecimal("b4_belop"),
            stønadstype = Stønadstype.valueOf(rs.getString("stonadstype")),
            resultatSisteVedtak = rs.getString("resultat_siste_vedtak"),
            metadata = rs.getString("metadata")?.let { commonObjectmapper.readValue<AldersjusteringMetadata>(it) },
        )
}
