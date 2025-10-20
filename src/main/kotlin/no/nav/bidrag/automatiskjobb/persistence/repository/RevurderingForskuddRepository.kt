package no.nav.bidrag.automatiskjobb.persistence.repository

import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import org.springframework.data.jpa.repository.JpaRepository

interface RevurderingForskuddRepository : JpaRepository<RevurderingForskudd, Int>
