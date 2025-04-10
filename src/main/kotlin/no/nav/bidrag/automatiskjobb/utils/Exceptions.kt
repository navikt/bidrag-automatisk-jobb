package no.nav.bidrag.automatiskjobb.utils

import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

fun ugyldigForespørsel(melding: String): Nothing = throw HttpClientErrorException(HttpStatus.BAD_REQUEST, melding)
