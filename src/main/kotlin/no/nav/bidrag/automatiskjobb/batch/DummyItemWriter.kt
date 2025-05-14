package no.nav.bidrag.automatiskjobb.batch

import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component

@Component
class DummyItemWriter : ItemWriter<Any?> {
    override fun write(chunk: Chunk<out Any?>) {
    }
}
