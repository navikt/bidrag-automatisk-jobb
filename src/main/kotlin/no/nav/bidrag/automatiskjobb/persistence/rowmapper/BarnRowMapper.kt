package no.nav.bidrag.automatiskjobb.persistence.rowmapper

import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet

class BarnRowMapper : RowMapper<Barn> {
    override fun mapRow(
        rs: ResultSet,
        rowNum: Int,
    ): Barn =
        Barn(
            id = rs.getInt("id"),
            opprettet = rs.getTimestamp("opprettet")?.toLocalDateTime(),
            bidragFra = rs.getTimestamp("bidrag_fra")?.toLocalDateTime()?.toLocalDate(),
            bidragTil = rs.getTimestamp("bidrag_til")?.toLocalDateTime()?.toLocalDate(),
            forskuddFra = rs.getTimestamp("forskudd_fra")?.toLocalDateTime()?.toLocalDate(),
            forskuddTil = rs.getTimestamp("forskudd_til")?.toLocalDateTime()?.toLocalDate(),
            skyldner = rs.getString("skyldner"),
            kravhaver = rs.getString("kravhaver"),
            saksnummer = rs.getString("saksnummer"),
            fødselsdato = rs.getTimestamp("fodselsdato")?.toLocalDateTime()?.toLocalDate(),
        )
}
