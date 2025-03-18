package no.nav.bidrag.automatiskjobb.testdata

import com.fasterxml.jackson.databind.node.POJONode
import no.nav.bidrag.automatiskjobb.service.skyldnerNav
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.person.AldersgruppeForskudd
import no.nav.bidrag.domene.enums.person.Bostatuskode
import no.nav.bidrag.domene.enums.person.Sivilstandskode
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.vedtak.BehandlingsrefKilde
import no.nav.bidrag.domene.enums.vedtak.Beslutningstype
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakskilde
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.felles.grunnlag.BostatusPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningBarnIHusstand
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningBidragspliktigesAndel
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningSumInntekt
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.InntektsrapporteringPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.Person
import no.nav.bidrag.transport.behandling.felles.grunnlag.SivilstandPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.SluttberegningBarnebidrag
import no.nav.bidrag.transport.behandling.felles.grunnlag.SluttberegningForskudd
import no.nav.bidrag.transport.behandling.felles.grunnlag.SluttberegningSærbidrag
import no.nav.bidrag.transport.behandling.felles.grunnlag.SøknadGrunnlag
import no.nav.bidrag.transport.behandling.stonad.response.StønadDto
import no.nav.bidrag.transport.behandling.stonad.response.StønadPeriodeDto
import no.nav.bidrag.transport.behandling.vedtak.Behandlingsreferanse
import no.nav.bidrag.transport.behandling.vedtak.Sporingsdata
import no.nav.bidrag.transport.behandling.vedtak.Stønadsendring
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import no.nav.bidrag.transport.behandling.vedtak.response.BehandlingsreferanseDto
import no.nav.bidrag.transport.behandling.vedtak.response.EngangsbeløpDto
import no.nav.bidrag.transport.behandling.vedtak.response.HentVedtakForStønadResponse
import no.nav.bidrag.transport.behandling.vedtak.response.StønadsendringDto
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakDto
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakForStønad
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakPeriodeDto
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

val SOKNAD_ID = 12412421414L
val saksnummer = "21321312321"
val personIdentSøknadsbarn1 = "213213213213"
val personIdentSøknadsbarn2 = "213213333213213"
val personIdentBidragsmottaker = "123213333"
val personIdentBidragspliktig = "55555"
val SAKSBEHANDLER_IDENT = "Z999999"

val persongrunnlagBM =
    GrunnlagDto(
        type = Grunnlagstype.PERSON_BIDRAGSMOTTAKER,
        referanse = personIdentBidragsmottaker,
        innhold = POJONode(Person(ident = Personident(personIdentBidragsmottaker))),
    )
val persongrunnlagBA =
    GrunnlagDto(
        type = Grunnlagstype.PERSON_SØKNADSBARN,
        referanse = personIdentSøknadsbarn1,
        innhold = POJONode(Person(ident = Personident(personIdentSøknadsbarn1), fødselsdato = LocalDate.now().minusYears(15))),
    )
val persongrunnlagBP =
    GrunnlagDto(
        type = Grunnlagstype.PERSON_BARN_BIDRAGSPLIKTIG,
        referanse = personIdentBidragspliktig,
        innhold = POJONode(Person(ident = Personident(personIdentBidragspliktig))),
    )

fun opprettBostatatusperiode(referanse: String = "BOSTATUS_PERIODE") =
    GrunnlagDto(
        referanse = referanse,
        type = Grunnlagstype.BOSTATUS_PERIODE,
        grunnlagsreferanseListe = emptyList(),
        gjelderReferanse = persongrunnlagBM.referanse,
        gjelderBarnReferanse = persongrunnlagBA.referanse,
        innhold =
            POJONode(
                BostatusPeriode(
                    periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                    bostatus = Bostatuskode.MED_FORELDER,
                    manueltRegistrert = false,
                    relatertTilPart = persongrunnlagBA.referanse,
                ),
            ),
    )

fun opprettGrunnlagSøknad(referanse: String = "SØKNAD") =
    GrunnlagDto(
        referanse = referanse,
        type = Grunnlagstype.SØKNAD,
        grunnlagsreferanseListe = emptyList(),
        innhold =
            POJONode(
                SøknadGrunnlag(
                    mottattDato = LocalDate.now(),
                    søktFraDato = LocalDate.parse("2024-01-01"),
                    søktAv = SøktAvType.BIDRAGSMOTTAKER,
                ),
            ),
    )

fun opprettDelberegningBarnIHusstand(referanse: String = "DELBEREGNING_BARN_I_HUSSTAND") =
    GrunnlagDto(
        referanse = referanse,
        type = Grunnlagstype.DELBEREGNING_BARN_I_HUSSTAND,
        grunnlagsreferanseListe = listOf(opprettBostatatusperiode().referanse),
        gjelderReferanse = persongrunnlagBM.referanse,
        innhold =
            POJONode(
                DelberegningBarnIHusstand(
                    periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                    antallBarn = 1.0,
                ),
            ),
    )

fun opprettSivilstandPeriode() =
    GrunnlagDto(
        referanse = "SIVILSTAND_PERIODE",
        type = Grunnlagstype.SIVILSTAND_PERIODE,
        grunnlagsreferanseListe = emptyList(),
        gjelderReferanse = persongrunnlagBM.referanse,
        innhold =
            POJONode(
                SivilstandPeriode(
                    periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                    sivilstand = Sivilstandskode.BOR_ALENE_MED_BARN,
                    manueltRegistrert = false,
                ),
            ),
    )

fun opprettInntektsrapportering() =
    GrunnlagDto(
        referanse = "INNTEKT_RAPPORTERING_PERIODE",
        type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
        grunnlagsreferanseListe = emptyList(),
        gjelderReferanse = persongrunnlagBM.referanse,
        innhold =
            POJONode(
                InntektsrapporteringPeriode(
                    periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                    beløp = BigDecimal(600000),
                    manueltRegistrert = true,
                    inntektsrapportering = Inntektsrapportering.LØNN_MANUELT_BEREGNET,
                    valgt = true,
                ),
            ),
    )

fun opprettDelberegningSumInntekt() =
    GrunnlagDto(
        referanse = "DELBEREGNING_SUM_INNTEKT",
        type = Grunnlagstype.DELBEREGNING_SUM_INNTEKT,
        grunnlagsreferanseListe = listOf(opprettInntektsrapportering().referanse),
        gjelderReferanse = persongrunnlagBM.referanse,
        innhold =
            POJONode(
                DelberegningSumInntekt(
                    periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                    totalinntekt = BigDecimal(600000),
                ),
            ),
    )

fun opprettGrunnlagDelberegningAndel() =
    GrunnlagDto(
        referanse = "DELBEREGNING_BIDRAGSPLIKTIGES_ANDEL",
        type = Grunnlagstype.DELBEREGNING_BIDRAGSPLIKTIGES_ANDEL,
        grunnlagsreferanseListe = listOf(opprettDelberegningSumInntekt().referanse),
        innhold =
            POJONode(
                DelberegningBidragspliktigesAndel(
                    periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                    endeligAndelFaktor = BigDecimal("0.3"),
                    andelBeløp = BigDecimal("0.5"),
                    beregnetAndelFaktor = BigDecimal("0.5"),
                    barnEndeligInntekt = BigDecimal("0.5"),
                    barnetErSelvforsørget = false,
                ),
            ),
    )

fun opprettGrunnlagSluttberegningForskudd() =
    GrunnlagDto(
        referanse = "sluttberegning_forskudd",
        type = Grunnlagstype.SLUTTBEREGNING_FORSKUDD,
        grunnlagsreferanseListe =
            listOf(
                opprettSivilstandPeriode().referanse,
                opprettDelberegningBarnIHusstand().referanse,
                opprettDelberegningSumInntekt().referanse,
            ),
        innhold =
            POJONode(
                SluttberegningForskudd(
                    periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                    beløp = BigDecimal(600000),
                    resultatKode = Resultatkode.REDUSERT_FORSKUDD_50_PROSENT,
                    aldersgruppe = AldersgruppeForskudd.ALDER_0_10_ÅR,
                ),
            ),
    )

fun opprettGrunnlagSluttberegningSærbidrag() =
    GrunnlagDto(
        referanse = "sluttberegning_særbidrag",
        type = Grunnlagstype.SLUTTBEREGNING_SÆRBIDRAG,
        grunnlagsreferanseListe = listOf(opprettGrunnlagDelberegningAndel().referanse),
        innhold =
            POJONode(
                SluttberegningSærbidrag(
                    periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                    beregnetBeløp = BigDecimal("100"),
                    resultatBeløp = BigDecimal("100"),
                    resultatKode = Resultatkode.SÆRBIDRAG_INNVILGET,
                ),
            ),
    )

fun opprettGrunnlagSluttberegningBidrag() =
    GrunnlagDto(
        referanse = "sluttberegning_barnebidrag",
        type = Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG,
        grunnlagsreferanseListe = listOf(opprettGrunnlagDelberegningAndel().referanse),
        innhold =
            POJONode(
                SluttberegningBarnebidrag(
                    periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                    beregnetBeløp = BigDecimal("100"),
                    resultatBeløp = BigDecimal("100"),
                    uMinusNettoBarnetilleggBM = BigDecimal("100"),
                    bruttoBidragEtterBarnetilleggBM = BigDecimal("100"),
                    nettoBidragEtterBarnetilleggBM = BigDecimal("100"),
                    bruttoBidragEtterBarnetilleggBP = BigDecimal("100"),
                    nettoBidragEtterSamværsfradrag = BigDecimal("100"),
                    bpAndelAvUVedDeltBostedBeløp = BigDecimal("100"),
                    bpAndelAvUVedDeltBostedFaktor = BigDecimal("100"),
                    bruttoBidragJustertForEvneOg25Prosent = BigDecimal("100"),
                ),
            ),
    )

fun opprettVedtakDto() =
    VedtakDto(
        kilde = Vedtakskilde.MANUELT,
        fastsattILand = "",
        type = Vedtakstype.ENDRING,
        opprettetAv = "",
        opprettetAvNavn = "",
        kildeapplikasjon = "bisys",
        vedtakstidspunkt = LocalDateTime.now(),
        enhetsnummer = Enhetsnummer("4444"),
        innkrevingUtsattTilDato = null,
        opprettetTidspunkt = LocalDateTime.now(),
        engangsbeløpListe = emptyList(),
        behandlingsreferanseListe = emptyList(),
        grunnlagListe = emptyList(),
        stønadsendringListe = emptyList(),
    )

fun opprettEngangsbeløpSærbidrag() =
    EngangsbeløpDto(
        type = Engangsbeløptype.SÆRBIDRAG,
        kravhaver = Personident(personIdentSøknadsbarn1),
        mottaker = Personident(personIdentBidragsmottaker),
        skyldner = Personident(personIdentBidragspliktig),
        sak = Saksnummer(saksnummer),
        innkreving = Innkrevingstype.MED_INNKREVING,
        beslutning = Beslutningstype.ENDRING,
        omgjørVedtakId = null,
        eksternReferanse = null,
        resultatkode = Resultatkode.SÆRBIDRAG_INNVILGET.name,
        beløp = BigDecimal(1000),
        valutakode = "",
        referanse = "",
        delytelseId = "",
        grunnlagReferanseListe = listOf(opprettGrunnlagSluttberegningSærbidrag().referanse),
    )

fun opprettStønadsendringBidrag() =
    StønadsendringDto(
        type = Stønadstype.BIDRAG,
        kravhaver = Personident(personIdentSøknadsbarn1),
        mottaker = Personident(personIdentBidragsmottaker),
        skyldner = Personident(personIdentBidragspliktig),
        sak = Saksnummer(saksnummer),
        førsteIndeksreguleringsår = null,
        innkreving = Innkrevingstype.MED_INNKREVING,
        beslutning = Beslutningstype.ENDRING,
        omgjørVedtakId = null,
        eksternReferanse = null,
        grunnlagReferanseListe = emptyList(),
        periodeListe =
            listOf(
                VedtakPeriodeDto(
                    periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                    resultatkode = Resultatkode.BEREGNET_BIDRAG.name,
                    beløp = BigDecimal(1000),
                    delytelseId = null,
                    valutakode = "NOK",
                    grunnlagReferanseListe = listOf(opprettGrunnlagSluttberegningBidrag().referanse),
                ),
            ),
    )

fun opprettStønadsendringForskudd() =
    StønadsendringDto(
        type = Stønadstype.FORSKUDD,
        kravhaver = Personident(personIdentSøknadsbarn1),
        mottaker = Personident(personIdentBidragsmottaker),
        skyldner = skyldnerNav,
        sak = Saksnummer(saksnummer),
        førsteIndeksreguleringsår = null,
        innkreving = Innkrevingstype.MED_INNKREVING,
        beslutning = Beslutningstype.ENDRING,
        omgjørVedtakId = null,
        eksternReferanse = null,
        grunnlagReferanseListe = emptyList(),
        periodeListe =
            listOf(
                VedtakPeriodeDto(
                    periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                    resultatkode = Resultatkode.FORHØYET_FORSKUDD_100_PROSENT.name,
                    beløp = BigDecimal(1000),
                    delytelseId = null,
                    valutakode = "NOK",
                    grunnlagReferanseListe = listOf(opprettGrunnlagSluttberegningForskudd().referanse),
                ),
            ),
    )

fun opprettStønadDto(
    periodeListe: List<StønadPeriodeDto>,
    stønadstype: Stønadstype = Stønadstype.BIDRAG,
    opprettetTidspunkt: LocalDateTime = LocalDateTime.parse("2025-01-01T00:00:00"),
) = StønadDto(
    sak = Saksnummer(saksnummer),
    skyldner = if (stønadstype == Stønadstype.BIDRAG) Personident(personIdentBidragspliktig) else skyldnerNav,
    kravhaver = Personident(personIdentSøknadsbarn1),
    mottaker = Personident(personIdentBidragsmottaker),
    førsteIndeksreguleringsår = 2025,
    innkreving = Innkrevingstype.MED_INNKREVING,
    opprettetAv = "",
    opprettetTidspunkt = opprettetTidspunkt,
    endretAv = null,
    endretTidspunkt = null,
    stønadsid = 1,
    type = stønadstype,
    periodeListe = periodeListe,
)

fun opprettStønadPeriodeDto(
    periode: ÅrMånedsperiode = ÅrMånedsperiode(LocalDate.parse("2024-08-01"), null),
    beløp: BigDecimal? = BigDecimal.ONE,
    valutakode: String = "NOK",
    vedtakId: Int = 1,
) = StønadPeriodeDto(
    stønadsid = 1,
    periodeid = 1,
    periodeGjortUgyldigAvVedtaksid = null,
    vedtaksid = vedtakId,
    gyldigFra = LocalDateTime.parse("2024-01-01T00:00:00"),
    gyldigTil = null,
    periode = periode,
    beløp = beløp,
    valutakode = valutakode,
    resultatkode = "OK",
)

fun opprettVedtakhendelse(
    vedtakId: Int,
    stonadType: Stønadstype = Stønadstype.BIDRAG,
): VedtakHendelse =
    VedtakHendelse(
        type = Vedtakstype.FASTSETTELSE,
        opprettetAv = SAKSBEHANDLER_IDENT,
        stønadsendringListe =
            listOf(
                Stønadsendring(
                    type = stonadType,
                    eksternReferanse = "",
                    beslutning = Beslutningstype.ENDRING,
                    førsteIndeksreguleringsår = 2024,
                    innkreving = Innkrevingstype.MED_INNKREVING,
                    kravhaver = Personident(""),
                    mottaker = Personident(""),
                    omgjørVedtakId = 1,
                    periodeListe = emptyList(),
                    sak = Saksnummer(saksnummer),
                    skyldner = Personident(""),
                ),
            ),
        engangsbeløpListe = emptyList(),
        enhetsnummer = Enhetsnummer("4806"),
        id = vedtakId,
        kilde = Vedtakskilde.MANUELT,
        kildeapplikasjon = "bidrag-behandling",
        opprettetTidspunkt = LocalDateTime.now(),
        opprettetAvNavn = "",
        sporingsdata = Sporingsdata("sporing"),
        innkrevingUtsattTilDato = null,
        vedtakstidspunkt = LocalDateTime.now(),
        fastsattILand = null,
        behandlingsreferanseListe =
            listOf(
                Behandlingsreferanse(
                    BehandlingsrefKilde.BISYS_SØKNAD.name,
                    SOKNAD_ID.toString(),
                ),
            ),
    )

fun opprettVedtakForStønadRespons(
    kravhaver: String,
    stønadstype: Stønadstype,
) = HentVedtakForStønadResponse(
    listOf(
        opprettVedtakForStønad(kravhaver, stønadstype),
    ),
)

fun opprettVedtakForStønad(
    kravhaver: String,
    stønadstype: Stønadstype,
) = VedtakForStønad(
    vedtaksid = 1,
    type = Vedtakstype.FASTSETTELSE,
    kilde = Vedtakskilde.MANUELT,
    vedtakstidspunkt = LocalDateTime.parse("2024-01-01T00:00:00"),
    behandlingsreferanser =
        listOf(
            BehandlingsreferanseDto(
                kilde = BehandlingsrefKilde.BISYS_SØKNAD,
                referanse =
                    if (kravhaver == personIdentSøknadsbarn1) {
                        SOKNAD_ID.toString()
                    } else if (kravhaver == personIdentSøknadsbarn2) {
                        124124231414L.toString()
                    } else {
                        12412435521414L.toString()
                    },
            ),
        ),
    stønadsendring =
        StønadsendringDto(
            type = stønadstype,
            sak = Saksnummer(saksnummer),
            skyldner = Personident(personIdentBidragspliktig),
            kravhaver = Personident(kravhaver),
            mottaker = Personident(personIdentBidragsmottaker),
            førsteIndeksreguleringsår = 0,
            innkreving = Innkrevingstype.MED_INNKREVING,
            beslutning = Beslutningstype.ENDRING,
            omgjørVedtakId = null,
            eksternReferanse = "123456",
            grunnlagReferanseListe = emptyList(),
            periodeListe =
                listOf(
                    VedtakPeriodeDto(
                        periode = ÅrMånedsperiode(LocalDate.parse("2024-07-01"), null),
                        beløp = BigDecimal(5160),
                        valutakode = "NOK",
                        resultatkode = "KBB",
                        delytelseId = null,
                        grunnlagReferanseListe = emptyList(),
                    ),
                ),
        ),
)
