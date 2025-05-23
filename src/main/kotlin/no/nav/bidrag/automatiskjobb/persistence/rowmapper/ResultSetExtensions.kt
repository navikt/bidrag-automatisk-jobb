package no.nav.bidrag.automatiskjobb.persistence.rowmapper

import java.sql.ResultSet

fun ResultSet.getIntOrNull(string: String): Int? = this.getInt(string).takeIf { it != 0 }

fun ResultSet.getLongOrNull(string: String): Long? = this.getLong(string).takeIf { it != 0L }
