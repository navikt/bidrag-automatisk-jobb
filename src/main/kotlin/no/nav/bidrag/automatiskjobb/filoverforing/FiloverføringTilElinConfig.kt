package no.nav.bidrag.automatiskjobb.filoverforing

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding
import java.util.Base64

@ConfigurationProperties("sftp")
class FiloverføringTilElinConfig
    @ConstructorBinding
    constructor(
        val username: String,
        val host: String,
        val port: Int,
        val privateKey: String,
        val directory: String = "inbound",
        val skalOverforeFil: Boolean,
    ) {
        companion object {
            const val JSCH_CHANNEL_TYPE_SFTP = "sftp"
        }

        val privateKeyDecoded: ByteArray get() = Base64.getDecoder().decode(privateKey)
    }
