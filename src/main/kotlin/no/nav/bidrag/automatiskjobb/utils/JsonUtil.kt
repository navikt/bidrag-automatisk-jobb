package no.nav.bidrag.automatiskjobb.utils

import no.nav.bidrag.transport.felles.commonObjectmapper

open class JsonUtil {
    companion object {
        fun tilJson(json: Any): String = commonObjectmapper.writerWithDefaultPrettyPrinter().writeValueAsString(json)
    }
}
