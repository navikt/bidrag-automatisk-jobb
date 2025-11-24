package no.nav.bidrag.automatiskjobb.consumer.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import no.nav.bidrag.domene.enums.behandling.Behandlingstatus
import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import no.nav.bidrag.domene.enums.behandling.Behandlingstype
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.ident.Personident
import java.math.BigDecimal
import java.time.LocalDate

data class OpprettBehandlingResponse(
    val id: Int,
)

data class OpprettBehandlingRequest(
    val søknadstype: Behandlingstype? = null,
    val behandlingstype: Behandlingstype? = null,
    val behandlingstema: Behandlingstema? = null,
    @param:Schema(required = true)
    val vedtakstype: Vedtakstype,
    @param:Schema(required = true)
    val søktFomDato: LocalDate,
    @param:Schema(required = true)
    val mottattdato: LocalDate,
    @param:Schema(required = true)
    val søknadFra: SøktAvType,
    @field:NotBlank(message = "Saksnummer kan ikke være blank")
    @field:Size(max = 7, min = 7, message = "Saksnummer skal ha sju tegn")
    val saksnummer: String,
    @field:NotBlank(message = "Enhet kan ikke være blank")
    @field:Size(min = 4, max = 4, message = "Enhet må være 4 tegn")
    val behandlerenhet: String,
    val roller: Set<@Valid OpprettRolleDto>,
    @param:Schema(required = true)
    var stønadstype: Stønadstype? = null,
    @param:Schema(required = true)
    var engangsbeløpstype: Engangsbeløptype? = null,
    @param:Schema(required = true)
    val søknadsid: Long?,
    val vedtaksid: Int? = null,
    val søknadsreferanseid: Long? = null,
    val kategori: OpprettKategoriRequestDto? = null,
    val innkrevingstype: Innkrevingstype? = Innkrevingstype.MED_INNKREVING,
    val opprettSøknad: Boolean = false,
)

@Schema(description = "Rolle beskrivelse som er brukte til å opprette nye roller")
data class OpprettRolleDto(
    @param:Schema(required = true, enumAsRef = true)
    val rolletype: Rolletype,
    @param:Schema(
        type = "String",
        description = "F.eks fødselsnummer. Påkrevd for alle rolletyper utenom for barn som ikke inngår i beregning.",
        required = false,
        nullable = true,
    )
    val ident: Personident?,
    @param:Schema(
        type = "String",
        description = "Navn på rolleinnehaver hvis ident er ukjent. Gjelder kun barn som ikke inngår i beregning",
        required = false,
        nullable = true,
    )
    val navn: String? = null,
    @param:Schema(type = "String", format = "date", description = "F.eks fødselsdato")
    val fødselsdato: LocalDate?,
    val innbetaltBeløp: BigDecimal? = null,
    val erSlettet: Boolean = false,
    val erUkjent: Boolean = false,
    val harGebyrsøknad: Boolean = false,
    val behandlingstatus: Behandlingstatus? = null,
    val behandlingstema: Behandlingstema? = null,
)

data class OpprettKategoriRequestDto(
    @param:Schema(required = true)
    val kategori: String,
    @param:Schema(
        required = false,
        description = "Beskrivelse av kategorien som er valgt. Er påkrevd hvis kategori er ANNET ",
    )
    val beskrivelse: String? = null,
)
