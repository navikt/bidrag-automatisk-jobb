package no.nav.bidrag.automatiskjobb.persistence.repository

import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface BarnRepository : JpaRepository<Barn, Int> {
    @Query(
        "SELECT b FROM barn b WHERE :år - YEAR(b.fødselsdato) IN (6, 11, 15) " +
            "AND b.bidragFra <= :kjøredato " +
            "AND (b.bidragTil IS NULL OR b.bidragTil >= :kjøredato)",
    )
    fun finnBarnSomSkalAldersjusteresForÅr(
        @Param("år") år: Int,
        @Param("kjøredato") kjøredato: LocalDate = LocalDate.now(),
        pageable: Pageable = Pageable.ofSize(100),
    ): Page<Barn>

    fun findByKravhaverAndSaksnummer(
        kravhaver: String,
        saksnummer: String,
    ): Barn?

    fun findAllByKravhaver(kravhaver: String): List<Barn>

    fun findAllBySkyldner(skyldner: String): List<Barn>
}
