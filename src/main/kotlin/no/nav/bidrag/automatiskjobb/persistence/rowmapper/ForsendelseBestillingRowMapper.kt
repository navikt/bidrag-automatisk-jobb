package no.nav.bidrag.automatiskjobb.persistence.rowmapper

import no.nav.bidrag.automatiskjobb.persistence.entity.ForsendelseBestilling
import no.nav.bidrag.automatiskjobb.persistence.repository.AldersjusteringRepository
import no.nav.bidrag.domene.enums.diverse.Språk
import no.nav.bidrag.domene.enums.rolle.Rolletype
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet

class ForsendelseBestillingRowMapper(
    private val aldersjusteringRepository: AldersjusteringRepository,
) : RowMapper<ForsendelseBestilling> {
    override fun mapRow(
        rs: ResultSet,
        rowNum: Int,
    ): ForsendelseBestilling =
        ForsendelseBestilling(
            id = rs.getIntOrNull("id"),
            aldersjustering = aldersjusteringRepository.findById(rs.getInt("aldersjustering_id")).get(),
            forsendelseId = rs.getLongOrNull("forsendelse_id"),
            journalpostId = rs.getLongOrNull("journalpost_id"),
            rolletype = rs.getString("rolletype")?.let { Rolletype.valueOf(it) },
            gjelder = rs.getString("gjelder"),
            mottaker = rs.getString("mottaker"),
            språkkode = rs.getString("sprakkode")?.let { Språk.valueOf(it) },
            dokumentmal = rs.getString("dokumentmal"),
            opprettetTidspunkt = rs.getTimestamp("opprettet_tidspunkt"),
            forsendelseOpprettetTidspunkt = rs.getTimestamp("forsendelse_opprettet_tidspunkt"),
            distribuertTidspunkt = rs.getTimestamp("distribuert_tidspunkt"),
            slettetTidspunkt = rs.getTimestamp("slettet_tidspunkt"),
            skalSlettes = rs.getBoolean("skal_slettes"),
            feilBegrunnelse = rs.getString("feil_begrunnelse"),
        )
}
