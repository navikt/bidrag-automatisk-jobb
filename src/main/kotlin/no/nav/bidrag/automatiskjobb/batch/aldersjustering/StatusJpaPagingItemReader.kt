package no.nav.bidrag.automatiskjobb.batch.aldersjustering

import org.springframework.batch.item.database.JpaPagingItemReader

class StatusJpaPagingItemReader<T> : JpaPagingItemReader<T>() {
    override fun getPage(): Int = 0
}
