package no.nav.bidrag.automatiskjobb.configuration

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import no.nav.bidrag.beregn.barnebidrag.service.orkestrering.AldersjusteringOrchestrator
import no.nav.bidrag.commons.service.organisasjon.EnableSaksbehandlernavnProvider
import no.nav.bidrag.commons.unleash.EnableUnleashFeatures
import no.nav.bidrag.commons.util.EnableSjekkForNyIdent
import no.nav.bidrag.commons.web.DefaultCorsFilter
import no.nav.bidrag.commons.web.MdcFilter
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import org.springframework.retry.annotation.EnableRetry

@EnableAspectJAutoProxy
@OpenAPIDefinition(
    info = Info(title = "bidrag-automatisk-jobb", version = "v1"),
    security = [SecurityRequirement(name = "bearer-key")],
)
@SecurityScheme(bearerFormat = "JWT", name = "bearer-key", scheme = "bearer", type = SecuritySchemeType.HTTP)
@Configuration
@EnableJwtTokenValidation
@EnableOAuth2Client(cacheEnabled = true)
@EnableSaksbehandlernavnProvider
@EnableRetry
@EnableBatchProcessing
@EnableSjekkForNyIdent
@EnableUnleashFeatures
@Import(DefaultCorsFilter::class, MdcFilter::class, AldersjusteringOrchestrator::class)
class BidragAutomatiskJobbConfiguration
