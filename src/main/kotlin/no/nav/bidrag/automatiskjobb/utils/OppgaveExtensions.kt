package no.nav.bidrag.automatiskjobb.utils

import no.nav.bidrag.automatiskjobb.service.model.ForskuddRedusertResultat
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse

val revurderForskuddBeskrivelse = "Revurder forskudd basert på inntekt fra nytt vedtak om barnebidrag."
val revurderForskuddBeskrivelseSærbidrag = "Revurder forskudd basert på inntekt fra nytt vedtak om særbidrag."
val revurderForskuddBeskrivelseAdresseendring =
    "Barnet er ikke lenger folkeregistrert på samme adresse som bidragsmottaker, og forskuddet må revurderes."
val oppgaveAldersjusteringBeskrivelse =
    "Automatisk aldersjustering av bidrag ble ikke foretatt. " +
        "Det må kontrolleres om saken skal aldersjusteres manuelt. Årsak til at saken ikke ble justert: {}"

fun VedtakHendelse.erForskudd() = stønadsendringListe?.any { it.type == Stønadstype.FORSKUDD } == true

fun ForskuddRedusertResultat.tilOppgaveBeskrivelse() =
    if (engangsbeløptype ==
        Engangsbeløptype.SÆRBIDRAG
    ) {
        revurderForskuddBeskrivelseSærbidrag
    } else {
        revurderForskuddBeskrivelse
    }
