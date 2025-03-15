package no.nav.bidrag.automatiskjobb.persistence.repository

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import no.nav.bidrag.automatiskjobb.BidragAutomatiskJobb
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import java.time.LocalDate

@Transactional
@DirtiesContext
@ActiveProfiles("test")
@EnableMockOAuth2Server
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = [BidragAutomatiskJobb::class])
@Disabled
class BarnRepositoryTest {
    companion object {
        private var postgreSqlDb =
            PostgreSQLContainer("postgres:latest").apply {
                withDatabaseName("bidrag-regnskap")
                withUsername("cloudsqliamuser")
                withPassword("admin")
                start()
            }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgreSqlDb::getJdbcUrl)
            registry.add("spring.datasource.username", postgreSqlDb::getUsername)
            registry.add("spring.datasource.password", postgreSqlDb::getPassword)
        }
    }

    @Autowired
    private lateinit var barnRepository: BarnRepository

    @Test
    fun skalHenteUtBarnSomSkalAldersjusteresFor2024() {
        val barn2024 = Barn(fødselsdato = LocalDate.of(2024, 1, 1))
        val barn2018 = Barn(fødselsdato = LocalDate.of(2018, 1, 1))
        val barn2016 = Barn(fødselsdato = LocalDate.of(2016, 3, 3))
        val barn2013 = Barn(fødselsdato = LocalDate.of(2013, 12, 31))
        val barn2011 = Barn(fødselsdato = LocalDate.of(2011, 9, 27))
        val barn2009 = Barn(fødselsdato = LocalDate.of(2009, 6, 15))
        val barn2006 = Barn(fødselsdato = LocalDate.of(2006, 4, 8))
        val barn2001 = Barn(fødselsdato = LocalDate.of(2001, 2, 24))
        barnRepository.saveAll(
            listOf(
                barn2024,
                barn2018,
                barn2016,
                barn2013,
                barn2011,
                barn2009,
                barn2006,
                barn2001,
            ),
        )

        val barnSomSkalAldersjusteres = barnRepository.finnBarnSomSkalAldersjusteresForÅr(2024)

        barnSomSkalAldersjusteres shouldHaveSize 3
        barnSomSkalAldersjusteres shouldContainAll listOf(barn2018, barn2013, barn2009)
    }
}
