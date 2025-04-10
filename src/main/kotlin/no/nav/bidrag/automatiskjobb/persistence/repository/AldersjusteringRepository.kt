package no.nav.bidrag.automatiskjobb.persistence.repository

import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import org.springframework.data.jpa.repository.JpaRepository

interface AldersjusteringRepository : JpaRepository<Aldersjustering, Int>
