package no.nav.bidrag.automatiskjobb.mapper

import com.fasterxml.jackson.databind.node.POJONode
import no.nav.bidrag.automatiskjobb.consumer.BidragPersonConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Behandlingstype
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
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.sak.Stønadsid
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BeregnetBarnebidragResultat
import no.nav.bidrag.transport.behandling.felles.grunnlag.AldersjusteringDetaljerGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.Person
import no.nav.bidrag.transport.behandling.felles.grunnlag.hentPersonMedIdent
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettBehandlingsreferanseRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettGrunnlagRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettPeriodeRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettStønadsendringRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettVedtakRequestDto
import no.nav.bidrag.transport.sak.BidragssakDto
import no.nav.bidrag.transport.sak.RolleDto
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.YearMonth

@Component
class VedtakMapper(
    val vedtakService: VedtakService,
    val personConsumer: BidragPersonConsumer,
    val sakConsumer: BidragSakConsumer,
    private val identUtils: IdentUtils,
) {
    fun hentBarn(
        sak: BidragssakDto,
        ident: String,
    ) = sak.roller.find {
        it.type == Rolletype.BARN &&
            identUtils.hentNyesteIdent(it.fødselsnummer!!) == identUtils.hentNyesteIdent(Personident(ident))
    }
        ?: error("Fant ikke barn med ident $ident i sak ${sak.saksnummer}")

    fun tilOpprettVedtakRequest(
        resultat: BeregnetBarnebidragResultat,
        stønad: Stønadsid,
        aldersjustering: Aldersjustering,
    ): OpprettVedtakRequestDto {
        val sak = sakConsumer.hentSak(aldersjustering.barn.saksnummer)
        val grunnlagPerson = resultat.grunnlagListe.hentPersonMedIdent(stønad.kravhaver.verdi)!!
        val sakrolleBarn = hentBarn(sak, aldersjustering.barn.kravhaver)
        val mottaker = reellMottakerEllerBidragsmottaker(sakrolleBarn, sak.roller)!!
        val grunnlagAldersjustering =
            opprettAldersjusteringDetaljerGrunnlag(
                aldersjustering,
                grunnlagPerson.referanse,
            )
        val grunnlagsliste =
            resultat.grunnlagListe.map { it.tilOpprettGrunnlagRequestDto() } + listOf(grunnlagAldersjustering)

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
                            førsteIndeksreguleringsår =
                                maxOf(
                                    aldersjustering.aldersjusteresForÅr + 1,
                                    YearMonth.now().year + 1,
                                ),
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

    private fun Personident.tilPersonGrunnlag(grunnlagstype: Grunnlagstype): OpprettGrunnlagRequestDto =
        OpprettGrunnlagRequestDto(
            referanse = "${grunnlagstype}_${this.verdi}",
            type = grunnlagstype,
            innhold =
                POJONode(
                    Person(
                        ident = this,
                        fødselsdato = personConsumer.hentPerson(this).fødselsdato ?: LocalDate.MIN,
                        navn = personConsumer.hentPerson(this).navn,
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
                    grunnlagFraVedtak = aldersjustering.vedtaksidBeregning,
                ),
            ),
    )

    fun tilOpprettVedtakRequestIngenAldersjustering(aldersjustering: Aldersjustering): OpprettVedtakRequestDto {
        val sak = sakConsumer.hentSak(aldersjustering.barn.saksnummer)
        val sakrolleBarn = hentBarn(sak, aldersjustering.barn.kravhaver)
        val faktiskMottaker = reellMottakerEllerBidragsmottaker(sakrolleBarn, sak.roller)!!
        val skyldnerIdent = Personident(aldersjustering.barn.skyldner!!)
        val mottakerIdent =
            sak.roller
                .find { it.type == Rolletype.BIDRAGSMOTTAKER }
                ?.fødselsnummer
                ?.let { identUtils.hentNyesteIdent(it) }
        val reelMottakerIdent = sakrolleBarn.reellMottaker?.ident?.personIdent()
        val grunnlagPerson = aldersjustering.barn.tilPersonobjekt(aldersjustering.stønadstype)
        val grunnlagAldersjustering =
            opprettAldersjusteringDetaljerGrunnlag(
                aldersjustering,
                grunnlagPerson.referanse,
            )

        return byggOpprettVedtakRequestObjekt()
            .copy(
                unikReferanse = aldersjustering.unikReferanse,
                grunnlagListe =
                    setOfNotNull(
                        grunnlagAldersjustering,
                        grunnlagPerson,
                        skyldnerIdent.tilPersonGrunnlag(Grunnlagstype.PERSON_BIDRAGSPLIKTIG),
                        mottakerIdent?.tilPersonGrunnlag(Grunnlagstype.PERSON_BIDRAGSMOTTAKER),
                        reelMottakerIdent?.tilPersonGrunnlag(Grunnlagstype.PERSON_REELL_MOTTAKER),
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
                            skyldner = skyldnerIdent,
                            mottaker = faktiskMottaker,
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

    fun reellMottakerEllerBidragsmottaker(
        rolle: RolleDto,
        rollerliste: List<RolleDto>,
    ) = rolle.reellMottaker
        ?.ident
        ?.verdi
        ?.let { Personident(it) }
        ?: rollerliste.find { it.type == Rolletype.BIDRAGSMOTTAKER }?.fødselsnummer?.let { identUtils.hentNyesteIdent(it) }
}

fun GrunnlagDto.tilOpprettGrunnlagRequestDto(): OpprettGrunnlagRequestDto =
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
