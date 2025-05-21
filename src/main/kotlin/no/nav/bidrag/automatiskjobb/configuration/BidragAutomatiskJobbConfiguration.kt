package no.nav.bidrag.automatiskjobb.configuration

import io.getunleash.DefaultUnleash
import io.getunleash.UnleashContext
import io.getunleash.UnleashContextProvider
import io.getunleash.util.UnleashConfig
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import no.nav.bidrag.beregn.barnebidrag.service.AldersjusteringOrchestrator
import no.nav.bidrag.commons.service.organisasjon.EnableSaksbehandlernavnProvider
import no.nav.bidrag.commons.util.EnableSjekkForNyIdent
import no.nav.bidrag.commons.web.DefaultCorsFilter
import no.nav.bidrag.commons.web.MdcFilter
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.slf4j.MDC
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Scope
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
@Import(DefaultCorsFilter::class, MdcFilter::class, AldersjusteringOrchestrator::class)
class BidragAutomatiskJobbConfiguration {
    @Bean
    fun unleashConfig(
        @Value("\${NAIS_APP_NAME}") appName: String,
        @Value("\${UNLEASH_SERVER_API_URL}") apiUrl: String,
        @Value("\${UNLEASH_SERVER_API_TOKEN}") apiToken: String,
        @Value("\${UNLEASH_SERVER_API_ENV}") environment: String,
        @Value("\${UNLEASH_FETCH_SYNC:true}") fetchInSync: Boolean,
    ) = UnleashConfig
        .builder()
        .appName(appName)
        .unleashAPI("$apiUrl/api/")
        .instanceId(appName)
        .environment(environment)
        .synchronousFetchOnInitialisation(true)
        .apiKey(apiToken)
        .unleashContextProvider(DefaultUnleashContextProvider())
        .build()

    @Bean
    @Scope("prototype")
    fun unleashInstance(unleashConfig: UnleashConfig) = DefaultUnleash(unleashConfig)
}

class DefaultUnleashContextProvider : UnleashContextProvider {
    override fun getContext(): UnleashContext {
        val userId = MDC.get("user")
        return UnleashContext
            .builder()
            .userId(userId)
            .appName(MDC.get("applicationKey"))
            .build()
    }
}
