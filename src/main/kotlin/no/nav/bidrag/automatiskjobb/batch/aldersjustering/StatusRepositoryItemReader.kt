package no.nav.bidrag.automatiskjobb.batch.aldersjustering

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.batch.item.adapter.AbstractMethodInvokingDelegator.InvocationTargetThrowableWrapper
import org.springframework.batch.item.adapter.DynamicMethodInvocationException
import org.springframework.batch.item.data.RepositoryItemReader
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader
import org.springframework.beans.factory.InitializingBean
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.domain.Sort
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.lang.Nullable
import org.springframework.util.Assert
import org.springframework.util.ClassUtils
import org.springframework.util.MethodInvoker
import org.springframework.util.StringUtils
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.Volatile

/**
 * Modifisert versjon av [RepositoryItemReader] som kan brukes i batcher som oppdaterer status på argumenter under kjøring.
 * Dette er nødvendig da pagineringen ikke forstår at readeren ikke lenger returnerer samme rader etter at ny status er satt.
 */
open class StatusRepositoryItemReader<T> :
    AbstractItemCountingItemStreamItemReader<T?>(),
    InitializingBean {
    protected var logger: Log = LogFactory.getLog(javaClass)

    private var repository: PagingAndSortingRepository<*, *>? = null

    private var sort: Sort? = null

    @Volatile
    private var page = 0

    private var pageSize = 10

    @Volatile
    private var current = 0

    private var arguments: MutableList<*>? = null

    @Volatile
    private var results: MutableList<T?>? = null

    private val lock: Lock = ReentrantLock()

    private var methodName: String? = null

    init {
        name = ClassUtils.getShortName(StatusRepositoryItemReader::class.java)
    }

    /**
     * Arguments to be passed to the data providing method.
     * @param arguments list of method arguments to be passed to the repository
     */
    fun setArguments(arguments: MutableList<*>) {
        this.arguments = arguments
    }

    /**
     * Provides ordering of the results so that order is maintained between paged queries.
     * Use a [java.util.LinkedHashMap] in case of multiple sort entries to keep the
     * order.
     * @param sorts the fields to sort by and the directions
     */
    fun setSort(sorts: MutableMap<String?, Sort.Direction?>) {
        this.sort = convertToSort(sorts)
    }

    /**
     * @param pageSize The number of items to retrieve per page. Must be greater than 0.
     */
    fun setPageSize(pageSize: Int) {
        this.pageSize = pageSize
    }

    /**
     * The [PagingAndSortingRepository]
     * implementation used to read input from.
     * @param repository underlying repository for input to be read from.
     */
    fun setRepository(repository: PagingAndSortingRepository<*, *>) {
        this.repository = repository
    }

    /**
     * Specifies what method on the repository to call. This method must take
     * [Pageable] as the *last* argument.
     * @param methodName name of the method to invoke
     */
    fun setMethodName(methodName: String) {
        this.methodName = methodName
    }

    @Throws(Exception::class)
    override fun afterPropertiesSet() {
        Assert.state(repository != null, "A PagingAndSortingRepository is required")
        Assert.state(pageSize > 0, "Page size must be greater than 0")
        Assert.state(sort != null, "A sort is required")
        Assert.state(this.methodName != null && !this.methodName!!.isEmpty(), "methodName is required.")
        if (isSaveState) {
            Assert.state(StringUtils.hasText(name), "A name is required when saveState is set to true.")
        }
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
                page++

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

    @Throws(Exception::class)
    override fun jumpToItem(itemLastIndex: Int) {
        this.lock.lock()
        try {
            page = itemLastIndex / pageSize
            current = itemLastIndex % pageSize
        } finally {
            this.lock.unlock()
        }
    }

    /**
     * Performs the actual reading of a page via the repository. Available for overriding
     * as needed.
     * @return the list of items that make up the page
     * @throws Exception Based on what the underlying method throws or related to the
     * calling of the method
     */
    @Throws(Exception::class)
    protected fun doPageRead(): MutableList<T?> {
        val pageRequest: Pageable = PageRequest.of(0, pageSize, sort!!)

        val invoker = createMethodInvoker(repository!!, methodName!!)

        val parameters: MutableList<Any?> = ArrayList()

        if (arguments != null && !arguments!!.isEmpty()) {
            parameters.addAll(arguments!!)
        }

        parameters.add(pageRequest)

        invoker.setArguments(*parameters.toTypedArray())

        @Suppress("UNCHECKED_CAST")
        val curPage = doInvoke(invoker) as Slice<T?>

        return curPage.getContent()
    }

    @Throws(Exception::class)
    override fun doOpen() {
    }

    @Throws(Exception::class)
    override fun doClose() {
        this.lock.lock()
        try {
            current = 0
            page = 0
            results = null
        } finally {
            this.lock.unlock()
        }
    }

    private fun convertToSort(sorts: MutableMap<String?, Sort.Direction?>): Sort {
        val sortValues: MutableList<Sort.Order?> = ArrayList()

        for (curSort in sorts.entries) {
            sortValues.add(Sort.Order(curSort.value, curSort.key!!))
        }

        return Sort.by(sortValues)
    }

    @Throws(Exception::class)
    private fun doInvoke(invoker: MethodInvoker): Any {
        try {
            invoker.prepare()
        } catch (e: ClassNotFoundException) {
            throw DynamicMethodInvocationException(e)
        } catch (e: NoSuchMethodException) {
            throw DynamicMethodInvocationException(e)
        }

        try {
            return invoker.invoke()!!
        } catch (e: InvocationTargetException) {
            if (e.cause is Exception) {
                throw e.cause as Exception? as Throwable
            } else {
                throw InvocationTargetThrowableWrapper(e.cause!!)
            }
        } catch (e: IllegalAccessException) {
            throw DynamicMethodInvocationException(e)
        }
    }

    private fun createMethodInvoker(
        targetObject: Any,
        targetMethod: String,
    ): MethodInvoker {
        val invoker = MethodInvoker()
        invoker.targetObject = targetObject
        invoker.targetMethod = targetMethod
        return invoker
    }
}
