package no.nav.bidrag.automatiskjobb

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.context.annotation.EnableAspectJAutoProxy

const val PROFILE_NAIS = "nais"

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
