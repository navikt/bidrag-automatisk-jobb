package no.nav.bidrag.automatiskjobb.mapper

import com.fasterxml.jackson.databind.node.POJONode
import no.nav.bidrag.automatiskjobb.utils.hentBarnIHusstandPerioder
import no.nav.bidrag.automatiskjobb.utils.hentDelberegningSumInntekt
import no.nav.bidrag.automatiskjobb.utils.hentInntekter
import no.nav.bidrag.automatiskjobb.utils.hentInntekterSomGjelderBarn
import no.nav.bidrag.automatiskjobb.utils.hentSivilstandPerioder
import no.nav.bidrag.automatiskjobb.utils.tilResultatkode
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.beregning.Resultatkode.Companion.erAvslag
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningSumInntekt
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.Grunnlagsreferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.InntektsrapporteringPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.SluttberegningBarnebidrag
import no.nav.bidrag.transport.behandling.felles.grunnlag.bidragsmottaker
import no.nav.bidrag.transport.behandling.felles.grunnlag.byggSluttberegningBarnebidragDetaljer
import no.nav.bidrag.transport.behandling.felles.grunnlag.filtrerBasertPåEgenReferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.finnGrunnlagSomErReferertFraGrunnlagsreferanseListe
import no.nav.bidrag.transport.behandling.felles.grunnlag.finnSluttberegningIReferanser
import no.nav.bidrag.transport.behandling.felles.grunnlag.hentAllePersoner
import no.nav.bidrag.transport.behandling.felles.grunnlag.hentPersonMedIdent
import no.nav.bidrag.transport.behandling.felles.grunnlag.innholdTilObjekt
import no.nav.bidrag.transport.behandling.felles.grunnlag.sluttberegningPeriode
import no.nav.bidrag.transport.behandling.vedtak.response.EngangsbeløpDto
import no.nav.bidrag.transport.behandling.vedtak.response.StønadsendringDto
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakDto
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakPeriodeDto

val StønadsendringDto.erBidrag get() = listOf(Stønadstype.BIDRAG, Stønadstype.BIDRAG18AAR).contains(type)

object GrunnlagMapper {
    fun byggGrunnlagForBeregning(
        vedtakFattet: VedtakDto,
        vedtakLøpendeForskudd: VedtakDto,
        gjelderBarn: Personident,
    ): List<GrunnlagDto> {
        val fattetVedtakInneholderBarn =
            vedtakFattet.stønadsendringListe.any { it.kravhaver == gjelderBarn } ||
                vedtakFattet.engangsbeløpListe.any { it.kravhaver == gjelderBarn }
        val grunnlagFattetVedtak = byggGrunnlagFattetVedtak(vedtakFattet, gjelderBarn, fattetVedtakInneholderBarn)

        val forskuddStønadBarn = vedtakLøpendeForskudd.stønadsendringListe.find { it.kravhaver == gjelderBarn }!!
        val grunnlagForskudd =
            hentGrunnlagFraForskudd(
                vedtakLøpendeForskudd,
                forskuddStønadBarn,
                inkluderInntekterSomGjelderBarn = !fattetVedtakInneholderBarn,
            )

        val personobjekter = vedtakLøpendeForskudd.grunnlagListe.hentAllePersoner() as List<GrunnlagDto>

        val søknad = vedtakLøpendeForskudd.grunnlagListe.filtrerBasertPåEgenReferanse(Grunnlagstype.SØKNAD).first() as GrunnlagDto
        val bidragsmottaker = personobjekter.bidragsmottaker!!
        val søknadsbarn = vedtakLøpendeForskudd.grunnlagListe.hentPersonMedIdent(gjelderBarn.verdi)!!
        val grunnlagBidragJustert =
            grunnlagFattetVedtak.map {
                it.copy(
                    gjelderReferanse = bidragsmottaker.referanse,
                    gjelderBarnReferanse = if (it.gjelderBarnReferanse != null) søknadsbarn.referanse else null,
                    innhold =
                        if (it.type == Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE) {
                            val inntektInnhold = it.innholdTilObjekt<InntektsrapporteringPeriode>()
                            POJONode(
                                inntektInnhold.copy(
                                    gjelderBarn = søknadsbarn.referanse,
                                ),
                            )
                        } else {
                            it.innhold
                        },
                )
            }
        return (grunnlagBidragJustert + grunnlagForskudd + listOf(søknad) + personobjekter).toList()
    }

    private fun byggGrunnlagFattetVedtak(
        vedtakBidrag: VedtakDto,
        gjelderBarn: Personident,
        fattetVedtakInneholderBarn: Boolean,
    ): List<GrunnlagDto> =
        vedtakBidrag.stønadsendringListe.find { (!fattetVedtakInneholderBarn || it.kravhaver == gjelderBarn) && it.erBidrag }?.let {
            hentGrunnlagFraBidrag(vedtakBidrag, it)
        }
            ?: vedtakBidrag.engangsbeløpListe
                .find {
                    (!fattetVedtakInneholderBarn || it.kravhaver == gjelderBarn) &&
                        it.type == Engangsbeløptype.SÆRBIDRAG
                }?.let {
                    hentGrunnlagFraSærbidrag(vedtakBidrag, gjelderBarn, it)
                } ?: emptyList()

    private fun hentGrunnlagFraSærbidrag(
        vedtak: VedtakDto,
        gjelderBarn: Personident,
        engangsbeløpSærbidrag: EngangsbeløpDto,
    ): List<GrunnlagDto> {
        val bidragsmottaker = vedtak.grunnlagListe.bidragsmottaker!!
        val søknadsbarn = vedtak.grunnlagListe.hentPersonMedIdent(gjelderBarn.verdi)!!
        val inntekter =
            vedtak.grunnlagListe.hentInntekter(
                engangsbeløpSærbidrag.grunnlagReferanseListe,
                bidragsmottaker.referanse,
                søknadsbarn.referanse,
            )
        return inntekter
    }

    private fun hentGrunnlagFraBidrag(
        vedtak: VedtakDto,
        stønadsendring: StønadsendringDto,
    ): List<GrunnlagDto> {
        val periode = stønadsendring.periodeListe.hentSisteBeregnetPeriode(vedtak)
        val bidragsmottaker = vedtak.grunnlagListe.bidragsmottaker!!
        val søknadsbarn = vedtak.grunnlagListe.hentPersonMedIdent(stønadsendring.kravhaver.verdi)!!
        val inntekter =
            vedtak.grunnlagListe.hentInntekter(
                periode.grunnlagReferanseListe,
                bidragsmottaker.referanse,
                søknadsbarn.referanse,
            )
        return inntekter
    }

    private fun hentGrunnlagFraForskudd(
        vedtak: VedtakDto,
        stønadsendring: StønadsendringDto,
        inkluderInntekterSomGjelderBarn: Boolean,
    ): List<GrunnlagDto> {
        if (vedtak.grunnlagListe.isEmpty()) return emptyList()
        val periode = stønadsendring.periodeListe.maxBy { it.periode.fom }
        val bidragsmottaker = vedtak.grunnlagListe.bidragsmottaker!!
        val søknadsbarn = vedtak.grunnlagListe.hentPersonMedIdent(stønadsendring.kravhaver.verdi)!!
        val sivilstandPerioder = vedtak.grunnlagListe.hentSivilstandPerioder(periode, bidragsmottaker.referanse)
        val barnIHusstandPerioder = vedtak.grunnlagListe.hentBarnIHusstandPerioder(periode)
        val inntekterSomGjelderBarn =
            if (inkluderInntekterSomGjelderBarn) {
                vedtak.grunnlagListe.hentInntekterSomGjelderBarn(
                    periode.grunnlagReferanseListe,
                    bidragsmottaker.referanse,
                    søknadsbarn.referanse,
                )
            } else {
                emptyList()
            }
        return sivilstandPerioder + barnIHusstandPerioder + inntekterSomGjelderBarn
    }

    private fun erBidragResultatAvslagEllerManglerInntekterTilParter(
        vedtak: VedtakDto,
        periode: VedtakPeriodeDto,
    ): Boolean {
        val sluttberegning =
            vedtak.grunnlagListe.finnSluttberegningIReferanser(periode.grunnlagReferanseListe) ?: return false

        return when (sluttberegning.type) {
            Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG -> {
                val innhold = vedtak.grunnlagListe.byggSluttberegningBarnebidragDetaljer(periode.grunnlagReferanseListe)
                innhold == null || innhold.erResultatAvslag ||
                    vedtak.grunnlagListe
                        .finnGrunnlagSomErReferertFraGrunnlagsreferanseListe(
                            Grunnlagstype.DELBEREGNING_BIDRAGSPLIKTIGES_ANDEL,
                            sluttberegning.grunnlagsreferanseListe,
                        ).isEmpty()
            }
            Grunnlagstype.SLUTTBEREGNING_FORSKUDD -> {
                periode.resultatkode.tilResultatkode()?.erAvslag() == true
            }
            else -> false
        }
    }

    private fun List<VedtakPeriodeDto>.hentSisteBeregnetPeriode(vedtak: VedtakDto) =
        sortedBy { it.periode.fom }
            .filter { it.resultatkode != Resultatkode.OPPHØR.name }
            .filter {
                !erBidragResultatAvslagEllerManglerInntekterTilParter(vedtak, it)
            }.maxBy { it.periode.fom }

    // Det under brukes for debugging og logging av siste inntekter
    fun hentSisteDelberegningInntektFraForskudd(
        vedtak: VedtakDto,
        gjelderBarn: Personident,
    ): DelberegningSumInntekt? =
        try {
            vedtak.stønadsendringListe.find { it.kravhaver == gjelderBarn && it.type == Stønadstype.FORSKUDD }?.let {
                val periode = it.periodeListe.hentSisteBeregnetPeriode(vedtak)
                val bidragsmottaker = vedtak.grunnlagListe.bidragsmottaker!!
                vedtak.grunnlagListe.hentDelberegningSumInntekt(
                    periode.grunnlagReferanseListe,
                    bidragsmottaker.referanse,
                )
            }
        } catch (e: Exception) {
            null
        }

    fun hentSisteInntektFraBeregning(
        sluttberegningReferanse: List<Grunnlagsreferanse>,
        grunnalagsliste: List<GrunnlagDto>,
    ) = try {
        val sluttberegning = grunnalagsliste.finnSluttberegningIReferanser(sluttberegningReferanse)!!
        val delberegning =
            grunnalagsliste
                .finnGrunnlagSomErReferertFraGrunnlagsreferanseListe(
                    Grunnlagstype.DELBEREGNING_SUM_INNTEKT,
                    sluttberegning.grunnlagsreferanseListe,
                ).firstOrNull()
                ?.innholdTilObjekt<DelberegningSumInntekt>()
        delberegning
    } catch (e: Exception) {
        null
    }

    fun hentSisteDelberegningInntektFattetVedtak(
        vedtak: VedtakDto,
        gjelderBarn: Personident,
    ) = try {
        hentSisteDelberegningInntektFraBidrag(vedtak, gjelderBarn)
            ?: hentSisteDelberegningInntektFraSærbidrag(vedtak, gjelderBarn)
    } catch (e: Exception) {
        null
    }

    private fun hentSisteDelberegningInntektFraSærbidrag(
        vedtak: VedtakDto,
        gjelderBarn: Personident,
    ): DelberegningSumInntekt? =
        vedtak.engangsbeløpListe.find { it.kravhaver == gjelderBarn && it.type == Engangsbeløptype.SÆRBIDRAG }?.let {
            val bidragsmottaker = vedtak.grunnlagListe.bidragsmottaker!!
            vedtak.grunnlagListe.hentDelberegningSumInntekt(
                it.grunnlagReferanseListe,
                bidragsmottaker.referanse,
            )
        }

    private fun hentSisteDelberegningInntektFraBidrag(
        vedtak: VedtakDto,
        gjelderBarn: Personident,
    ): DelberegningSumInntekt? =
        vedtak.stønadsendringListe.find { it.kravhaver == gjelderBarn && it.erBidrag }?.let {
            val periode = it.periodeListe.hentSisteBeregnetPeriode(vedtak)
            val bidragsmottaker = vedtak.grunnlagListe.bidragsmottaker!!
            vedtak.grunnlagListe.hentDelberegningSumInntekt(
                periode.grunnlagReferanseListe,
                bidragsmottaker.referanse,
            )
        }
}
