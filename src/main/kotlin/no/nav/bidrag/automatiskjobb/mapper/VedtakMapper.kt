package no.nav.bidrag.automatiskjobb.mapper

import com.fasterxml.jackson.databind.node.POJONode
import no.nav.bidrag.automatiskjobb.consumer.BidragPersonConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragSamhandlerConsumer
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.Behandlingstype
import no.nav.bidrag.automatiskjobb.utils.bidragspliktig
import no.nav.bidrag.automatiskjobb.utils.hentBarn
import no.nav.bidrag.beregn.barnebidrag.service.VedtakService
import no.nav.bidrag.commons.util.IdentUtils
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.vedtak.BehandlingsrefKilde
import no.nav.bidrag.domene.enums.vedtak.Beslutningstype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakskilde
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.ident.SamhandlerId
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.sak.Stønadsid
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BeregnetBarnebidragResultat
import no.nav.bidrag.transport.behandling.felles.grunnlag.AldersjusteringDetaljerGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.BaseGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.Person
import no.nav.bidrag.transport.behandling.felles.grunnlag.hentPersonMedIdent
import no.nav.bidrag.transport.behandling.felles.grunnlag.tilGrunnlagstype
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettBehandlingsreferanseRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettGrunnlagRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettPeriodeRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettStønadsendringRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettVedtakRequestDto
import no.nav.bidrag.transport.sak.RolleDto
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.YearMonth

@Component
class VedtakMapper(
    val vedtakService: VedtakService,
    val personConsumer: BidragPersonConsumer,
    val sakConsumer: BidragSakConsumer,
    val samhandlerConsumer: BidragSamhandlerConsumer,
    private val identUtils: IdentUtils,
) {
    fun tilOpprettVedtakRequest(
        resultat: BeregnetBarnebidragResultat,
        stønad: Stønadsid,
        aldersjustering: Aldersjustering,
    ): OpprettVedtakRequestDto {
        val sak = sakConsumer.hentSak(aldersjustering.barn.saksnummer)
        val grunnlagPerson = resultat.grunnlagListe.hentPersonMedIdent(stønad.kravhaver.verdi)!!
        val sakrolleBarn = sak.hentBarn(aldersjustering.barn.kravhaver)
        val mottaker = sak.roller.reellMottakerEllerBidragsmottaker(sakrolleBarn)!!
        val grunnlagAldersjustering =
            opprettAldersjusteringDetaljerGrunnlag(
                aldersjustering,
                grunnlagPerson.referanse,
            )
        val mottakerObjekt = mottaker.tilPersonObjekt(resultat.grunnlagListe, sak.roller)
        val grunnlagsliste =
            resultat.grunnlagListe.map { it.tilOpprettGrunnlagRequestDto() } + listOf(grunnlagAldersjustering) + listOf(mottakerObjekt)

        return byggOpprettVedtakRequestObjekt()
            .copy(
                unikReferanse = aldersjustering.unikReferanse,
                grunnlagListe = grunnlagsliste.toHashSet().toList(),
                behandlingsreferanseListe =
                    listOf(
                        OpprettBehandlingsreferanseRequestDto(
                            kilde =
                                when (stønad.type) {
                                    Stønadstype.BIDRAG -> BehandlingsrefKilde.ALDERSJUSTERING_BIDRAG
                                    else -> BehandlingsrefKilde.ALDERSJUSTERING_FORSKUDD
                                },
                            referanse = aldersjustering.batchId,
                        ),
                    ),
                stønadsendringListe =
                    listOf(
                        OpprettStønadsendringRequestDto(
                            type = stønad.type,
                            sak = stønad.sak,
                            kravhaver = stønad.kravhaver,
                            skyldner = stønad.skyldner,
                            mottaker = mottaker,
                            beslutning = Beslutningstype.ENDRING,
                            grunnlagReferanseListe = listOf(grunnlagAldersjustering.referanse),
                            innkreving = Innkrevingstype.MED_INNKREVING,
                            sisteVedtaksid = vedtakService.finnSisteVedtaksid(stønad),
                            periodeListe =
                                resultat.beregnetBarnebidragPeriodeListe.map {
                                    OpprettPeriodeRequestDto(
                                        periode = it.periode,
                                        beløp = it.resultat.beløp,
                                        valutakode = "NOK",
                                        resultatkode = Resultatkode.BEREGNET_BIDRAG.name,
                                        grunnlagReferanseListe = it.grunnlagsreferanseListe,
                                    )
                                },
                        ),
                    ),
            )
    }

    private fun Personident.tilPersonObjekt(
        grunnlagsliste: List<BaseGrunnlag>,
        roller: List<RolleDto>,
    ): OpprettGrunnlagRequestDto {
        val eksisterende = grunnlagsliste.hentPersonMedIdent(this.verdi)
        if (eksisterende != null) {
            return if (eksisterende is GrunnlagDto) {
                eksisterende.tilOpprettGrunnlagRequestDto()
            } else {
                OpprettGrunnlagRequestDto(
                    referanse = eksisterende.referanse,
                    type = eksisterende.type,
                    innhold = eksisterende.innhold,
                    gjelderReferanse = eksisterende.gjelderReferanse,
                    gjelderBarnReferanse = eksisterende.gjelderBarnReferanse,
                    grunnlagsreferanseListe = eksisterende.grunnlagsreferanseListe,
                )
            }
        }
        val rolletype = roller.find { it.fødselsnummer == this }?.type ?: Rolletype.REELMOTTAKER
        return OpprettGrunnlagRequestDto(
            referanse = "${rolletype.tilGrunnlagstype()}_${this.verdi}",
            type = rolletype.tilGrunnlagstype(),
            innhold =
                POJONode(
                    Person(
                        ident = this,
                        navn =
                            if (SamhandlerId(this.verdi).gyldig()) {
                                samhandlerConsumer.hentSamhandler(this.verdi)?.navn
                            } else {
                                personConsumer.hentPerson(this).navn!!
                            },
                        fødselsdato =
                            if (SamhandlerId(this.verdi).gyldig()) {
                                LocalDate.of(9999, 12, 1)
                            } else {
                                personConsumer.hentPerson(this).fødselsdato ?: LocalDate.MIN
                            },
                    ),
                ),
        )
    }

    private fun RolleDto.tilPersonobjekt(): OpprettGrunnlagRequestDto =
        OpprettGrunnlagRequestDto(
            referanse = "${type.tilGrunnlagstype()}_$fødselsnummer",
            type = type.tilGrunnlagstype(),
            innhold =
                POJONode(
                    Person(
                        ident = fødselsnummer,
                        fødselsdato = personConsumer.hentPerson(fødselsnummer!!).fødselsdato ?: LocalDate.MIN,
                        navn = personConsumer.hentPerson(fødselsnummer!!).navn,
                    ),
                ),
        )

    private fun Barn.tilPersonobjekt(stønadstype: Stønadstype) =
        OpprettGrunnlagRequestDto(
            referanse = "${Grunnlagstype.PERSON_SØKNADSBARN}_${this.tilStønadsid(stønadstype).toReferanse()}",
            type = Grunnlagstype.PERSON_SØKNADSBARN,
            innhold =
                POJONode(
                    Person(
                        ident = Personident(this.kravhaver),
                        fødselsdato = this.fødselsdato!!,
                        navn = personConsumer.hentPerson(Personident(this.kravhaver)).navn,
                    ),
                ),
        )

    private fun opprettAldersjusteringDetaljerGrunnlag(
        aldersjustering: Aldersjustering,
        søknadsbarnReferanse: String,
    ) = OpprettGrunnlagRequestDto(
        referanse = "${Grunnlagstype.ALDERSJUSTERING_DETALJER}_${aldersjustering.barn.tilStønadsid(
            aldersjustering.stønadstype,
        ).toReferanse()}",
        type = Grunnlagstype.ALDERSJUSTERING_DETALJER,
        gjelderBarnReferanse = søknadsbarnReferanse,
        gjelderReferanse = søknadsbarnReferanse,
        innhold =
            POJONode(
                AldersjusteringDetaljerGrunnlag(
                    periode = ÅrMånedsperiode(YearMonth.now().withYear(aldersjustering.aldersjusteresForÅr).withMonth(7), null),
                    aldersjusteresManuelt = aldersjustering.behandlingstype == Behandlingstype.MANUELL,
                    aldersjustert = aldersjustering.behandlingstype == Behandlingstype.FATTET_FORSLAG,
                    begrunnelser = aldersjustering.begrunnelse,
                    grunnlagFraVedtak = aldersjustering.vedtaksidBeregning?.toLong(),
                ),
            ),
    )

    fun tilOpprettVedtakRequestIngenAldersjustering(aldersjustering: Aldersjustering): OpprettVedtakRequestDto {
        val sak = sakConsumer.hentSak(aldersjustering.barn.saksnummer)
        val bidragspliktig = sak.bidragspliktig!!
        val sakrolleBarn = sak.hentBarn(aldersjustering.barn.kravhaver)
        val mottaker = sak.roller.reellMottakerEllerBidragsmottaker(sakrolleBarn)!!
        val grunnlagPerson = aldersjustering.barn.tilPersonobjekt(aldersjustering.stønadstype)
        val grunnlagAldersjustering =
            opprettAldersjusteringDetaljerGrunnlag(
                aldersjustering,
                grunnlagPerson.referanse,
            )

        val mottakerObjekt = mottaker.tilPersonObjekt(emptyList(), sak.roller)
        return byggOpprettVedtakRequestObjekt()
            .copy(
                unikReferanse = aldersjustering.unikReferanse,
                grunnlagListe =
                    setOfNotNull(
                        grunnlagAldersjustering,
                        grunnlagPerson,
                        mottakerObjekt,
                        bidragspliktig.tilPersonobjekt(),
                    ).toList(),
                behandlingsreferanseListe =
                    listOf(
                        OpprettBehandlingsreferanseRequestDto(
                            kilde =
                                when (aldersjustering.stønadstype) {
                                    Stønadstype.BIDRAG -> BehandlingsrefKilde.ALDERSJUSTERING_BIDRAG
                                    else -> BehandlingsrefKilde.ALDERSJUSTERING_FORSKUDD
                                },
                            referanse = aldersjustering.batchId,
                        ),
                    ),
                stønadsendringListe =
                    listOf(
                        OpprettStønadsendringRequestDto(
                            type = aldersjustering.stønadstype,
                            sak = Saksnummer(aldersjustering.barn.saksnummer),
                            kravhaver = Personident(aldersjustering.barn.kravhaver),
                            skyldner = Personident(aldersjustering.barn.skyldner!!),
                            mottaker = mottaker,
                            beslutning = Beslutningstype.AVVIST,
                            grunnlagReferanseListe = listOf(grunnlagAldersjustering.referanse),
                            innkreving = Innkrevingstype.MED_INNKREVING,
                            sisteVedtaksid =
                                vedtakService.finnSisteVedtaksid(
                                    aldersjustering.barn.tilStønadsid(aldersjustering.stønadstype),
                                ),
                            periodeListe = emptyList(),
                        ),
                    ),
            )
    }

    fun List<RolleDto>.reellMottakerEllerBidragsmottaker(rolle: RolleDto) =
        rolle.reellMottaker
            ?.ident
            ?.verdi
            ?.let { Personident(it) }
            ?: find { it.type == Rolletype.BIDRAGSMOTTAKER }?.fødselsnummer?.let { identUtils.hentNyesteIdent(it) }
}

private fun GrunnlagDto.tilOpprettGrunnlagRequestDto(): OpprettGrunnlagRequestDto =
    OpprettGrunnlagRequestDto(
        referanse = referanse,
        gjelderReferanse = gjelderReferanse,
        gjelderBarnReferanse = gjelderBarnReferanse,
        innhold = innhold,
        grunnlagsreferanseListe = grunnlagsreferanseListe,
        type = type,
    )

private fun byggOpprettVedtakRequestObjekt(): OpprettVedtakRequestDto =
    OpprettVedtakRequestDto(
        enhetsnummer = Enhetsnummer("9999"),
        vedtakstidspunkt = null,
        type = Vedtakstype.ALDERSJUSTERING,
        stønadsendringListe = emptyList(),
        engangsbeløpListe = emptyList(),
        behandlingsreferanseListe = emptyList(),
        grunnlagListe = emptyList(),
        kilde = Vedtakskilde.AUTOMATISK,
        fastsattILand = null,
        innkrevingUtsattTilDato = null,
        // Settes automatisk av bidrag-vedtak basert på token
        opprettetAv = null,
    )
