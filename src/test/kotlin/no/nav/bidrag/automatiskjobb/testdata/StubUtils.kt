package no.nav.bidrag.automatiskjobb.testdata

import io.mockk.every
import io.mockk.mockkObject
import no.nav.bidrag.commons.service.organisasjon.SaksbehandlernavnProvider

fun stubSaksbehandlernavnProvider() {
    mockkObject(SaksbehandlernavnProvider)
    every { SaksbehandlernavnProvider.hentSaksbehandlernavn(any()) } returns "Fornavn Etternavn"
}
