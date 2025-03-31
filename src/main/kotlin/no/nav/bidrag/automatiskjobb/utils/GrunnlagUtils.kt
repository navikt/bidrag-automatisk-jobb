package no.nav.bidrag.automatiskjobb.utils

import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.beregning.Resultatkode.Companion.erAvslag
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningSumInntekt
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.Grunnlagsreferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.bidragsmottaker
import no.nav.bidrag.transport.behandling.felles.grunnlag.finnGrunnlagSomErReferertFraGrunnlagsreferanseListe
import no.nav.bidrag.transport.behandling.felles.grunnlag.innholdTilObjekt
import no.nav.bidrag.transport.behandling.vedtak.response.StønadsendringDto
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakPeriodeDto

fun StønadsendringDto.enesteResultatkode() =
    if (periodeListe.size > 1) {
        null
    } else {
        periodeListe
            .firstOrNull()
            ?.resultatkode
            ?.tilResultatkode()
    }

fun StønadsendringDto.erDirekteAvslag() =
    periodeListe.size == 1 &&
        periodeListe
            .firstOrNull()
            ?.resultatkode
            ?.tilResultatkode()
            ?.erAvslag() == true

fun String.tilResultatkode() =
    try {
        Resultatkode.valueOf(this)
    } catch (e: IllegalArgumentException) {
        null
    }

fun List<GrunnlagDto>.hentSivilstandPerioder(
    periodeDto: VedtakPeriodeDto,
    gjelderPersonReferanse: Grunnlagsreferanse,
): List<GrunnlagDto> {
    val sivilstandPerioder =
        finnGrunnlagSomErReferertFraGrunnlagsreferanseListe(Grunnlagstype.SIVILSTAND_PERIODE, periodeDto.grunnlagReferanseListe)
            .filter { it.gjelderReferanse == gjelderPersonReferanse }
    return sivilstandPerioder as List<GrunnlagDto>
}

fun List<GrunnlagDto>.hentBarnIHusstandPerioder(periodeDto: VedtakPeriodeDto): List<GrunnlagDto> {
    val delberegningBarnIHusstand =
        finnGrunnlagSomErReferertFraGrunnlagsreferanseListe(
            Grunnlagstype.DELBEREGNING_BARN_I_HUSSTAND,
            periodeDto.grunnlagReferanseListe,
        )

    val barnIHusstandPerioder =
        delberegningBarnIHusstand.flatMap {
            finnGrunnlagSomErReferertFraGrunnlagsreferanseListe(Grunnlagstype.BOSTATUS_PERIODE, it.grunnlagsreferanseListe)
        }
    val bidragsmottakerReferanse = this.bidragsmottaker?.referanse
    return (barnIHusstandPerioder as List<GrunnlagDto>).map {
        it.copy(
            // Før så ble barn satt som gjelderReferanse.
            // Dette ble i ettertid endret til at BM = gjelderReferanse og barn = gjelderBarnReferanse
            // Justerer på dette slik at beregningen får inn riktig data
            gjelderBarnReferanse =
                if (it.gjelderBarnReferanse.isNullOrEmpty() &&
                    it.gjelderReferanse != bidragsmottakerReferanse
                ) {
                    it.gjelderReferanse
                } else {
                    it.gjelderBarnReferanse
                },
        )
    }
}

fun List<GrunnlagDto>.hentDelberegningSumInntekt(
    grunnlagsreferanseListe: List<Grunnlagsreferanse>,
    gjelderPersonReferanse: Grunnlagsreferanse,
): DelberegningSumInntekt? {
    val delberegningBidragsEvne =
        finnGrunnlagSomErReferertFraGrunnlagsreferanseListe(
            Grunnlagstype.DELBEREGNING_BIDRAGSPLIKTIGES_ANDEL,
            grunnlagsreferanseListe,
        ).firstOrNull()

    return finnGrunnlagSomErReferertFraGrunnlagsreferanseListe(
        Grunnlagstype.DELBEREGNING_SUM_INNTEKT,
        delberegningBidragsEvne?.grunnlagsreferanseListe ?: grunnlagsreferanseListe,
    ).filter { it.gjelderReferanse == gjelderPersonReferanse }
        .firstOrNull()
        ?.innholdTilObjekt<DelberegningSumInntekt>()
}

fun List<GrunnlagDto>.hentInntekterSomGjelderBarn(
    grunnlagsreferanseListe: List<Grunnlagsreferanse>,
    gjelderPersonReferanse: Grunnlagsreferanse,
    gjelderBarnReferanse: Grunnlagsreferanse,
): List<GrunnlagDto> {
    val delberegningSumInntekt =
        finnGrunnlagSomErReferertFraGrunnlagsreferanseListe(
            Grunnlagstype.DELBEREGNING_SUM_INNTEKT,
            grunnlagsreferanseListe,
        ).filter { it.gjelderReferanse == gjelderPersonReferanse }

    val inntekter =
        delberegningSumInntekt.flatMap {
            finnGrunnlagSomErReferertFraGrunnlagsreferanseListe(Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE, it.grunnlagsreferanseListe)
                .filter { it.gjelderBarnReferanse == gjelderBarnReferanse }
                .filter { it.gjelderReferanse == gjelderPersonReferanse }
        }
    return inntekter as List<GrunnlagDto>
}

fun List<GrunnlagDto>.hentInntekter(
    grunnlagsreferanseListe: List<Grunnlagsreferanse>,
    gjelderPersonReferanse: Grunnlagsreferanse,
    gjelderBarnReferanse: Grunnlagsreferanse,
): List<GrunnlagDto> {
    val delberegningBidragsEvne =
        finnGrunnlagSomErReferertFraGrunnlagsreferanseListe(
            Grunnlagstype.DELBEREGNING_BIDRAGSPLIKTIGES_ANDEL,
            grunnlagsreferanseListe,
        ).firstOrNull() ?: return emptyList()

    val delberegningSumInntekt =
        finnGrunnlagSomErReferertFraGrunnlagsreferanseListe(
            Grunnlagstype.DELBEREGNING_SUM_INNTEKT,
            delberegningBidragsEvne.grunnlagsreferanseListe,
        ).filter { it.gjelderReferanse == gjelderPersonReferanse }

    val inntekter =
        delberegningSumInntekt.flatMap {
            finnGrunnlagSomErReferertFraGrunnlagsreferanseListe(Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE, it.grunnlagsreferanseListe)
                .filter { it.gjelderBarnReferanse == null || it.gjelderBarnReferanse == gjelderBarnReferanse }
                .filter { it.gjelderReferanse == gjelderPersonReferanse }
        }
    return inntekter as List<GrunnlagDto>
}
