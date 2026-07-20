package no.nav.bidrag.automatiskjobb.service

import no.nav.bidrag.automatiskjobb.consumer.BidragPersonConsumer
import no.nav.bidrag.commons.service.AppContext
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.erPerson
import no.nav.bidrag.transport.behandling.felles.grunnlag.hentAllePersoner
import no.nav.bidrag.transport.behandling.felles.grunnlag.personIdent
import no.nav.bidrag.transport.behandling.felles.grunnlag.stønadstype
import no.nav.bidrag.transport.person.PersonDto

fun hentPerson(ident: String?): PersonDto? =
    try {
        ident.takeIfNotNullOrEmpty {
            AppContext.getBean(BidragPersonConsumer::class.java).hentPerson(Personident(it))
        }
    } catch (e: Exception) {
        secureLogger.debug(e) { "Feil ved henting av person for ident $ident" }
        null
    }

fun hentNyesteIdent(ident: String?) = ident?.let { hentPerson(ident)?.ident ?: Personident(ident) }

fun Collection<GrunnlagDto>.hentPersonNyesteIdent(
    ident: String?,
    stønadstype: Stønadstype? = null,
) = hentAllePersoner()
    .find {
        (hentNyesteIdent(it.personIdent)?.verdi == hentNyesteIdent(ident)?.verdi || it.personIdent == ident) &&
            (stønadstype == null || it.stønadstype == null || stønadstype == it.stønadstype)
    }

fun <T, R> T?.takeIfNotNullOrEmpty(block: (T) -> R): R? =
    if (this == null ||
        this is String &&
        this.trim().isEmpty() ||
        this is List<*> &&
        this.isEmpty()
    ) {
        null
    } else {
        block(this)
    }
