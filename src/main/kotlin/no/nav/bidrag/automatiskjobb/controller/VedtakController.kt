package no.nav.bidrag.automatiskjobb.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.automatiskjobb.service.VedtakService
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@Protected
@RestController
class VedtakController(
    private val vedtakService: VedtakService,
) {
    @PostMapping("/vedtak")
    @Operation(
        summary = "Manuell opprettelse av vedtak",
        description = "Operasjon for å opprette vedtak manuelt.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Vedtak opprettet.",
            ),
        ],
    )
    suspend fun opprettVedtak(vedtakHendelse: VedtakHendelse): ResponseEntity<Any> {
        vedtakService.behandleVedtak(vedtakHendelse)
        return ResponseEntity.ok().build()
    }
}
