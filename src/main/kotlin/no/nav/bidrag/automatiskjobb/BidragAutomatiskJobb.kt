package no.nav.bidrag.automatiskjobb

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.SpringApplication
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.context.annotation.EnableAspectJAutoProxy

const val PROFILE_NAIS = "nais"
val SECURE_LOGGER = KotlinLogging.logger("secureLogger")
val combinedLogger = KotlinLogging.logger("combinedLogger")

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class, ManagementWebSecurityAutoConfiguration::class])
@EnableJwtTokenValidation(ignore = ["org.springframework", "org.springdoc"])
@EnableAspectJAutoProxy
class BidragAutomatiskJobb

fun main(args: Array<String>) {
    SpringApplication(BidragAutomatiskJobb::class.java)
        .apply {
            setAdditionalProfiles(if (args.isEmpty()) PROFILE_NAIS else args[0])
        }.run(*args)
}
