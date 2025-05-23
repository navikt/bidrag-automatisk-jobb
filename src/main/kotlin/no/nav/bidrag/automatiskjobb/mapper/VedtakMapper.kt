package no.nav.bidrag.automatiskjobb.mapper

import com.fasterxml.jackson.databind.node.POJONode
import no.nav.bidrag.automatiskjobb.consumer.BidragPersonConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.Behandlingstype
import no.nav.bidrag.automatiskjobb.utils.bidragsmottaker
import no.nav.bidrag.automatiskjobb.utils.bidragspliktig
import no.nav.bidrag.beregn.barnebidrag.service.VedtakService
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
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
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BeregnetBarnebidragResultat
import no.nav.bidrag.transport.behandling.felles.grunnlag.AldersjusteringDetaljerGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.Person
import no.nav.bidrag.transport.behandling.felles.grunnlag.bidragsmottaker
import no.nav.bidrag.transport.behandling.felles.grunnlag.hentPersonMedIdent
import no.nav.bidrag.transport.behandling.felles.grunnlag.personIdent
import no.nav.bidrag.transport.behandling.felles.grunnlag.tilGrunnlagstype
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettBehandlingsreferanseRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettGrunnlagRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettPeriodeRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettStønadsendringRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettVedtakRequestDto
import no.nav.bidrag.transport.sak.RolleDto
import org.springframework.stereotype.Component

@Component
class VedtakMapper(
    val vedtakService: VedtakService,
    val personConsumer: BidragPersonConsumer,
    val sakConsumer: BidragSakConsumer,
) {
    fun tilOpprettVedtakRequest(
        resultat: BeregnetBarnebidragResultat,
        stønad: Stønadsid,
        aldersjustering: Aldersjustering,
    ): OpprettVedtakRequestDto {
        val grunnlagPerson = resultat.grunnlagListe.hentPersonMedIdent(stønad.kravhaver.verdi)!!
        val grunnlagAldersjustering =
            opprettAldersjusteringDetaljerGrunnlag(
                aldersjustering,
                grunnlagPerson.referanse,
            )
        return byggOpprettVedtakRequestObjekt()
            .copy(
                unikReferanse = aldersjustering.unikReferanse,
                grunnlagListe =
                    resultat.grunnlagListe.map {
                        OpprettGrunnlagRequestDto(
                            referanse = it.referanse,
                            gjelderReferanse = it.gjelderReferanse,
                            gjelderBarnReferanse = it.gjelderBarnReferanse,
                            innhold = it.innhold,
                            grunnlagsreferanseListe = it.grunnlagsreferanseListe,
                            type = it.type,
                        )
                    } + listOf(grunnlagAldersjustering),
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
                            mottaker = Personident(resultat.grunnlagListe.bidragsmottaker!!.personIdent!!),
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

    private fun RolleDto.tilPersonobjekt(): OpprettGrunnlagRequestDto =
        OpprettGrunnlagRequestDto(
            referanse = "${type.tilGrunnlagstype()}_$fødselsnummer",
            type = type.tilGrunnlagstype(),
            innhold =
                POJONode(
                    Person(
                        ident = fødselsnummer,
                        fødselsdato = personConsumer.hentPerson(fødselsnummer!!).fødselsdato!!,
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
        innhold =
            POJONode(
                AldersjusteringDetaljerGrunnlag(
                    aldersjusteresManuelt = aldersjustering.behandlingstype == Behandlingstype.MANUELL,
                    aldersjustert = aldersjustering.behandlingstype == Behandlingstype.FATTET_FORSLAG,
                    begrunnelser = aldersjustering.begrunnelse,
                    grunnlagFraVedtak = aldersjustering.vedtaksidBeregning?.toLong(),
                ),
            ),
    )

    fun tilOpprettVedtakRequestIngenAldersjustering(aldersjustering: Aldersjustering): OpprettVedtakRequestDto {
        val sak = sakConsumer.hentSak(aldersjustering.barn.saksnummer)
        val bidragsmottaker = sak.bidragsmottaker!!
        val bidragspliktig = sak.bidragspliktig!!
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
                    setOf(
                        grunnlagAldersjustering,
                        grunnlagPerson,
                        bidragsmottaker.tilPersonobjekt(),
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
                            mottaker = bidragsmottaker.fødselsnummer!!,
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
}

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
