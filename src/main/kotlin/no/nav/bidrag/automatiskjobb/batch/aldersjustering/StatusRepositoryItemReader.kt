package no.nav.bidrag.automatiskjobb.batch.aldersjustering

import org.springframework.batch.item.data.RepositoryItemReader
import org.springframework.beans.factory.InitializingBean
import org.springframework.lang.Nullable
import org.springframework.stereotype.Component
import org.springframework.util.ClassUtils
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.Volatile

/**
 * Modifisert versjon av [RepositoryItemReader] som kan brukes i batcher som oppdaterer status på argumenter under kjøring.
 * Dette er nødvendig da pagineringen ikke forstår at readeren ikke lenger returnerer samme rader etter at ny status er satt.
 */
@Component
class StatusRepositoryItemReader<T> :
    RepositoryItemReader<T?>(),
    InitializingBean {
    @Volatile
    private var page = 0

    @Volatile
    private var current = 0

    @Volatile
    private var results: MutableList<T?>? = null

    private val lock: Lock = ReentrantLock()

    init {
        name = ClassUtils.getShortName(StatusRepositoryItemReader::class.java)
    }

    @Nullable
    @Throws(Exception::class)
    override fun doRead(): T? {
        this.lock.lock()
        try {
            val nextPageNeeded = (results != null && current >= results!!.size)

            if (results == null || nextPageNeeded) {
                if (logger.isDebugEnabled) {
                    logger.debug("Reading page $page")
                }

                results = doPageRead()

                if (results!!.isEmpty()) {
                    return null
                }

                if (nextPageNeeded) {
                    current = 0
                }
            }

            if (current < results!!.size) {
                val curLine = results!![current]
                current++
                return curLine
            } else {
                return null
            }
        } finally {
            this.lock.unlock()
        }
    }
}
