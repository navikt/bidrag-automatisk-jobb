package no.nav.bidrag.automatiskjobb.mapper

import no.nav.bidrag.beregn.barnebidrag.service.VedtakService
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.vedtak.BehandlingsrefKilde
import no.nav.bidrag.domene.enums.vedtak.Beslutningstype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakskilde
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Stønadsid
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BeregnetBarnebidragResultat
import no.nav.bidrag.transport.behandling.felles.grunnlag.bidragsmottaker
import no.nav.bidrag.transport.behandling.felles.grunnlag.personIdent
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettBehandlingsreferanseRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettGrunnlagRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettPeriodeRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettStønadsendringRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettVedtakRequestDto
import org.springframework.stereotype.Component

@Component
class VedtakMapper(
    val vedtakService: VedtakService,
) {
    fun tilOpprettVedtakRequest(
        resultat: BeregnetBarnebidragResultat,
        stønad: Stønadsid,
        batchId: String,
    ) = byggOpprettVedtakRequestObjekt()
        .copy(
            unikReferanse = "aldersjustering_${batchId}_${stønad.toReferanse()}",
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
                },
            behandlingsreferanseListe =
                listOf(
                    OpprettBehandlingsreferanseRequestDto(
                        kilde =
                            when (stønad.type) {
                                Stønadstype.BIDRAG -> BehandlingsrefKilde.ALDERSJUSTERING_BIDRAG
                                else -> BehandlingsrefKilde.ALDERSJUSTERING_FORSKUDD
                            },
                        referanse = batchId,
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
                        grunnlagReferanseListe = emptyList(),
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
