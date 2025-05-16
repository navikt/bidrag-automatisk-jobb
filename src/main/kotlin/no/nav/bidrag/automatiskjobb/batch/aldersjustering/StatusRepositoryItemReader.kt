package no.nav.bidrag.automatiskjobb.batch.aldersjustering

import org.springframework.batch.item.adapter.AbstractMethodInvokingDelegator.InvocationTargetThrowableWrapper
import org.springframework.batch.item.adapter.DynamicMethodInvocationException
import org.springframework.batch.item.data.RepositoryItemReader
import org.springframework.beans.factory.InitializingBean
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.domain.Sort
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.util.ClassUtils
import org.springframework.util.MethodInvoker
import java.lang.reflect.InvocationTargetException

/**
 * Modifisert versjon av [RepositoryItemReader] som kan brukes i batcher som oppdaterer status på argumenter under kjøring.
 * Dette er nødvendig da pagineringen ikke forstår at readeren ikke lenger returnerer samme rader etter at ny status er satt.
 */
open class StatusRepositoryItemReader<T> :
    RepositoryItemReader<T?>(),
    InitializingBean {
//    @Volatile
//    private var page = 0

    private var pageSize = 10

//    @Volatile
//    private var current = 0

//    @Volatile
//    private var results: List<T?>? = null

    private var arguments: List<Any>? = null

    private var repository: PagingAndSortingRepository<*, *>? = null

    private lateinit var sort: Sort

    private lateinit var methodName: String

//    private val lock: Lock = ReentrantLock()

    init {
        name = ClassUtils.getShortName(StatusRepositoryItemReader::class.java)
    }

    override fun setPageSize(pageSize: Int) {
        this.pageSize = pageSize
        super.setPageSize(pageSize)
    }

    override fun setSort(sorts: MutableMap<String?, Sort.Direction?>) {
        this.sort = convertToSort(sorts)
        super.setSort(sorts)
    }

    override fun setRepository(repository: PagingAndSortingRepository<*, *>) {
        this.repository = repository
        super.setRepository(repository)
    }

    override fun setMethodName(methodName: String) {
        this.methodName = methodName
        super.setMethodName(methodName)
    }

    @Throws(java.lang.Exception::class)
    override fun doPageRead(): MutableList<T?> {
        val pageRequest: Pageable = PageRequest.of(0, pageSize, sort)

        val invoker = createMethodInvoker(repository, methodName)

        val parameters: MutableList<Any?> = ArrayList()

        if (arguments != null && !arguments!!.isEmpty()) {
            parameters.addAll(arguments!!)
        }

        parameters.add(pageRequest)

        invoker.setArguments(*parameters.toTypedArray())

        val curPage = doInvoke(invoker) as Slice<T?>

        return curPage.getContent()
    }

//    @Nullable
//    @Throws(Exception::class)
//    override fun doRead(): T? {
//        this.lock.lock()
//        try {
//            val nextPageNeeded = (results != null && current >= results!!.size)
//
//            if (results == null || nextPageNeeded) {
//                if (logger.isDebugEnabled) {
//                    logger.debug("Reading page $page")
//                }
//
//                results = doPageRead()
//
//                if (results!!.isEmpty()) {
//                    return null
//                }
//
//                if (nextPageNeeded) {
//                    current = 0
//                }
//            }
//
//            if (current < results!!.size) {
//                val curLine = results!![current]
//                current++
//                return curLine
//            } else {
//                return null
//            }
//        } finally {
//            this.lock.unlock()
//        }
//    }

//    @Throws(java.lang.Exception::class)
//    override fun jumpToItem(itemLastIndex: Int) {
//        this.lock.lock()
//        try {
//            page = 0
//            current = itemLastIndex % pageSize
//        } finally {
//            this.lock.unlock()
//        }
//    }

    private fun convertToSort(sorts: MutableMap<String?, Sort.Direction?>): Sort {
        val sortValues: MutableList<Sort.Order?> = java.util.ArrayList<Sort.Order?>()

        for (curSort in sorts.entries) {
            sortValues.add(Sort.Order(curSort.value, curSort.key!!))
        }

        return Sort.by(sortValues)
    }

    private fun createMethodInvoker(
        targetObject: PagingAndSortingRepository<*, *>?,
        targetMethod: String,
    ): MethodInvoker {
        val invoker = MethodInvoker()
        invoker.targetObject = targetObject
        invoker.targetMethod = targetMethod
        return invoker
    }

    @Throws(java.lang.Exception::class)
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
            if (e.cause is java.lang.Exception) {
                throw e.cause as java.lang.Exception? as Throwable
            } else {
                throw InvocationTargetThrowableWrapper(e.cause!!)
            }
        } catch (e: IllegalAccessException) {
            throw DynamicMethodInvocationException(e)
        }
    }
}
