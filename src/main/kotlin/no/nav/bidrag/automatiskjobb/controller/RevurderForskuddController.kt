package no.nav.bidrag.automatiskjobb.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.automatiskjobb.batch.revurdering.forskudd.opprett.OpprettRevurderForskuddBatch
import no.nav.security.token.support.core.api.Protected
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@Protected
@RestController
class RevurderForskuddController(
    private val opprettRevurderForskuddBatch: OpprettRevurderForskuddBatch,
) {
    @PostMapping("/revuderforskudd/opprett")
    @Operation(
        summary = "Oppretter revurderForskudd.",
        description = "Oppretter revurderForskudd for alle barn som ikke har hatt en revurdering siste 12 måneder.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun opprettRevurderForskudd() {
        opprettRevurderForskuddBatch.start()
    }
}
