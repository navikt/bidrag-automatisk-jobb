package no.nav.bidrag.automatiskjobb.testdata

import io.mockk.every
import io.mockk.mockkObject
import no.nav.bidrag.commons.service.organisasjon.EnhetProvider

fun stubSaksbehandlernavnProvider() {
    mockkObject(EnhetProvider)
    every { EnhetProvider.hentSaksbehandlernavn(any()) } returns "Fornavn Etternavn"
}
