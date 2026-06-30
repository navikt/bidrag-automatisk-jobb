package no.nav.bidrag.automatiskjobb.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.core.api.Protected
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.configuration.JobRegistry
import org.springframework.batch.core.job.JobExecution
import org.springframework.batch.core.job.parameters.JobParameter
import org.springframework.batch.core.launch.NoSuchJobException
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.StepExecution
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.LocalDateTime

private val LOGGER = KotlinLogging.logger {}

@Protected
@RestController
@RequestMapping("/batch/info")
@Tag(name = "Batch info", description = "Henter statusinformasjon om Spring Batch-jobber fra metadatadatabasen")
class BatchInfoController(
    private val jobRepository: JobRepository,
    private val jobRegistry: JobRegistry,
) {
    @GetMapping("/jobs")
    @Operation(
        summary = "Henter oversikt over alle registrerte jobber",
        description = "Returnerer navn på alle registrerte Spring Batch-jobber samt antall kjøringer og siste status.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Liste over jobber hentet"),
        ],
    )
    fun hentAlleJobber(
        @RequestParam(defaultValue = "true") @Parameter(
            description = "Når true returneres kun jobber som kjører akkurat nå.",
            example = "true",
        ) kunAktive: Boolean = true,
    ): ResponseEntity<List<JobOversiktDto>> {
        val oversikt =
            jobRegistry.jobNames
                .map { jobName ->
                    JobOversiktDto(
                        jobNavn = jobName,
                        antallKjøringer = antallKjøringerFor(jobName),
                        sisteStatus = hentSisteStatus(jobName),
                        harAktiveKjøringer = jobRepository.findRunningJobExecutions(jobName).isNotEmpty(),
                    )
                }.filter { !kunAktive || it.harAktiveKjøringer }
        return ResponseEntity.ok(oversikt)
    }

    @GetMapping("/jobs/{jobNavn}")
    @Operation(
        summary = "Henter detaljer for en spesifikk jobb",
        description =
            "Returnerer de siste kjøringene for en jobb med status, tidspunkt, parametere og stegdetaljer " +
                "(lese-/skrivetellere, hoppet-over rader, commit/rollback og eventuelle feilmeldinger).",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Detaljer hentet"),
            ApiResponse(responseCode = "404", description = "Jobb ikke funnet"),
        ],
    )
    fun hentJobbDetaljer(
        @PathVariable @Parameter(description = "Navn på jobben", example = "opprettAldersjusteringerBidragJob") jobNavn: String,
        @RequestParam(defaultValue = "10") @Parameter(description = "Maks antall kjøringer som returneres", example = "10") antall: Int,
    ): ResponseEntity<JobDetaljerDto> {
        if (jobNavn !in jobRegistry.jobNames) {
            LOGGER.warn { "Ukjent jobb forespurt: $jobNavn" }
            return ResponseEntity.notFound().build()
        }

        val kjøringer =
            jobRepository
                .getJobInstances(jobNavn, 0, antall)
                .flatMap { instans -> jobRepository.getJobExecutions(instans).map { it.tilJobKjøringDto() } }
                .sortedByDescending { it.opprettetTidspunkt }

        return ResponseEntity.ok(
            JobDetaljerDto(
                jobNavn = jobNavn,
                antallKjøringer = antallKjøringerFor(jobNavn),
                kjøringer = kjøringer,
            ),
        )
    }

    @GetMapping("/executions/{kjøringId}")
    @Operation(
        summary = "Henter detaljer for en enkelt jobkjøring",
        description = "Returnerer full informasjon om en spesifikk JobExecution inkludert alle steg og parametere.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Kjøring funnet"),
            ApiResponse(responseCode = "404", description = "Kjøring ikke funnet"),
        ],
    )
    fun hentKjøring(
        @PathVariable @Parameter(description = "JobExecution ID", example = "42") kjøringId: Long,
    ): ResponseEntity<JobKjøringDto> {
        val kjøring = jobRepository.getJobExecution(kjøringId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(kjøring.tilJobKjøringDto())
    }

    @GetMapping("/jobs/{jobNavn}/siste")
    @Operation(
        summary = "Henter siste kjøring for en jobb",
        description = "Returnerer den seneste JobExecution — ideell for en rask dashboard-widget som poller status.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Siste kjøring hentet"),
            ApiResponse(responseCode = "404", description = "Jobb ikke funnet eller ingen kjøringer"),
        ],
    )
    fun hentSisteKjøring(
        @PathVariable @Parameter(description = "Navn på jobben", example = "opprettAldersjusteringerBidragJob") jobNavn: String,
    ): ResponseEntity<JobKjøringDto> {
        if (jobNavn !in jobRegistry.jobNames) return ResponseEntity.notFound().build()
        val kjøring =
            jobRepository
                .getJobInstances(jobNavn, 0, 1)
                .firstOrNull()
                ?.let { jobRepository.getLastJobExecution(it) }
                ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(kjøring.tilJobKjøringDto())
    }

    @GetMapping("/jobs/{jobNavn}/fremgang")
    @Operation(
        summary = "Henter live fremgang og estimert gjenstående tid for en aktiv kjøring",
        description =
            "Returnerer steg-tellere (lest/skrevet/hoppet over) for den aktive kjøringen, " +
                "medgått tid og ETA basert på gjennomsnittlig varighet fra de siste vellykkede kjøringene.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Fremgang hentet"),
            ApiResponse(responseCode = "404", description = "Jobb har ingen aktiv kjøring"),
        ],
    )
    fun hentFremgang(
        @PathVariable @Parameter(description = "Navn på jobben", example = "opprettAldersjusteringerBidragJob") jobNavn: String,
        @RequestParam(
            defaultValue = "10",
        ) @Parameter(description = "Antall vellykkede kjøringer som brukes for ETA-beregning", example = "10") historikkAntall: Int,
    ): ResponseEntity<FremgangDto> {
        val aktiv =
            jobRepository.findRunningJobExecutions(jobNavn).maxByOrNull { it.createTime }
                ?: return ResponseEntity.notFound().build()

        val medgåttSekunder = aktiv.startTime?.let { Duration.between(it, LocalDateTime.now()).toSeconds() }

        val gjennomsnittSekunder =
            jobRepository
                .getJobInstances(jobNavn, 0, historikkAntall * 2)
                .flatMap { jobRepository.getJobExecutions(it) }
                .filter { it.status == BatchStatus.COMPLETED && it.startTime != null && it.endTime != null }
                .take(historikkAntall)
                .map { Duration.between(it.startTime, it.endTime).toSeconds() }
                .takeIf { it.isNotEmpty() }
                ?.average()
                ?.toLong()

        val gjenståendeSekunder =
            if (medgåttSekunder != null && gjennomsnittSekunder != null) {
                (gjennomsnittSekunder - medgåttSekunder).coerceAtLeast(0)
            } else {
                null
            }

        return ResponseEntity.ok(
            FremgangDto(
                kjøringId = aktiv.id,
                jobNavn = jobNavn,
                status = aktiv.status,
                startTidspunkt = aktiv.startTime,
                medgåttSekunder = medgåttSekunder,
                estimertGjenståendeSekunder = gjenståendeSekunder,
                estimertFerdigTidspunkt = gjenståendeSekunder?.let { LocalDateTime.now().plusSeconds(it) },
                gjennomsnittligVarighetSekunder = gjennomsnittSekunder,
                steg =
                    aktiv.stepExecutions.sortedBy { it.startTime }.map { steg ->
                        StegFremgangDto(
                            stegNavn = steg.stepName,
                            status = steg.status,
                            lestAntall = steg.readCount,
                            skrevet = steg.writeCount,
                            hoppetOver = steg.skipCount,
                            commitAntall = steg.commitCount,
                            startTidspunkt = steg.startTime,
                            medgåttSekunder =
                                steg.startTime?.let {
                                    Duration.between(it, steg.endTime ?: LocalDateTime.now()).toSeconds()
                                },
                        )
                    },
            ),
        )
    }

    @GetMapping("/statistikk")
    @Operation(
        summary = "Henter historisk statistikk for alle jobber",
        description =
            "Returnerer aggregert statistikk per jobb: antall vellykkede/feilede kjøringer, " +
                "gjennomsnittlig/min/max varighet og tidspunkt for siste vellykkede og feilede kjøring.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun hentStatistikk(
        @RequestParam(
            defaultValue = "50",
        ) @Parameter(description = "Antall siste kjøringer som inngår i statistikken", example = "50") antall: Int,
    ): ResponseEntity<List<JobStatistikkDto>> {
        val statistikk =
            jobRegistry.jobNames.map { jobName ->
                val kjøringer =
                    jobRepository
                        .getJobInstances(jobName, 0, antall)
                        .flatMap { jobRepository.getJobExecutions(it) }

                val fullførte =
                    kjøringer
                        .filter { it.status == BatchStatus.COMPLETED && it.startTime != null && it.endTime != null }
                val varigheter = fullførte.map { Duration.between(it.startTime, it.endTime).toSeconds() }

                JobStatistikkDto(
                    jobNavn = jobName,
                    antallVellykket = fullførte.size,
                    antallFeilet = kjøringer.count { it.status == BatchStatus.FAILED },
                    antallStoppet = kjøringer.count { it.status == BatchStatus.STOPPED },
                    gjennomsnittligVarighetSekunder = varigheter.takeIf { it.isNotEmpty() }?.average()?.toLong(),
                    minVarighetSekunder = varigheter.takeIf { it.isNotEmpty() }?.min(),
                    maxVarighetSekunder = varigheter.takeIf { it.isNotEmpty() }?.max(),
                    sisteVellykketTidspunkt = fullførte.maxByOrNull { it.endTime!! }?.endTime,
                    sisteFeiletTidspunkt =
                        kjøringer
                            .filter { it.status == BatchStatus.FAILED && it.endTime != null }
                            .maxByOrNull { it.endTime!! }
                            ?.endTime,
                )
            }
        return ResponseEntity.ok(statistikk)
    }

    @GetMapping("/running")
    @Operation(
        summary = "Henter alle kjøringer som kjører nå",
        description = "Returnerer alle JobExecutions med status STARTED eller STARTING på tvers av alle jobber.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun hentKjørendeJobber(): ResponseEntity<List<AktivKjøringDto>> {
        val aktive =
            jobRegistry.jobNames.flatMap { jobName ->
                jobRepository.findRunningJobExecutions(jobName).map { kjøring ->
                    AktivKjøringDto(
                        jobNavn = jobName,
                        kjøringId = kjøring.id,
                        status = kjøring.status,
                        startTidspunkt = kjøring.startTime,
                        varighetSekunder = kjøring.startTime?.let { Duration.between(it, LocalDateTime.now()).toSeconds() },
                        parametere = kjøring.jobParameters.tilParameterMap(),
                    )
                }
            }
        return ResponseEntity.ok(aktive)
    }

    private fun antallKjøringerFor(jobName: String): Long =
        try {
            jobRepository.getJobInstanceCount(jobName)
        } catch (_: NoSuchJobException) {
            0L
        }

    private fun hentSisteStatus(jobName: String): BatchStatus? =
        jobRepository
            .getJobInstances(jobName, 0, 1)
            .firstOrNull()
            ?.let { instans ->
                jobRepository
                    .getJobExecutions(instans)
                    .maxByOrNull { it.createTime }
                    ?.status
            }
}

private fun JobExecution.tilJobKjøringDto(): JobKjøringDto =
    JobKjøringDto(
        kjøringId = id,
        instansId = jobInstance.instanceId,
        status = status,
        exitStatus = exitStatus.exitCode,
        exitBeskrivelse = exitStatus.exitDescription.takeIf { it.isNotBlank() },
        startTidspunkt = startTime,
        sluttTidspunkt = endTime,
        opprettetTidspunkt = createTime,
        varighetSekunder = if (startTime != null && endTime != null) Duration.between(startTime, endTime).toSeconds() else null,
        parametere = jobParameters.tilParameterMap(),
        steg = stepExecutions.sortedBy { it.startTime }.map { it.tilStegKjøringDto() },
        feilmeldinger = allFailureExceptions.map { it.message ?: it.javaClass.simpleName },
    )

private fun StepExecution.tilStegKjøringDto(): StegKjøringDto =
    StegKjøringDto(
        stegNavn = stepName,
        status = status,
        exitStatus = exitStatus.exitCode,
        exitBeskrivelse = exitStatus.exitDescription.takeIf { it.isNotBlank() },
        lestAntall = readCount,
        skrevet = writeCount,
        hoppetOverLes = readSkipCount,
        hoppetOverProsesser = processSkipCount,
        hoppetOverSkriv = writeSkipCount,
        filtrert = filterCount,
        commitAntall = commitCount,
        rollbackAntall = rollbackCount,
        startTidspunkt = startTime,
        sluttTidspunkt = endTime,
        varighetSekunder = if (startTime != null && endTime != null) Duration.between(startTime, endTime).toSeconds() else null,
        feilmeldinger = failureExceptions.map { e -> e.message ?: e.javaClass.simpleName },
    )

private fun org.springframework.batch.core.job.parameters.JobParameters.tilParameterMap(): Map<String, JobParameterDto> =
    parameters().associate { param ->
        param.name() to
            JobParameterDto(
                type = param.type().simpleName ?: param.type().name,
                verdi = param.value()?.toString(),
                identifiserende = param.identifying(),
            )
    }

data class JobOversiktDto(
    val jobNavn: String,
    val antallKjøringer: Long,
    val sisteStatus: BatchStatus?,
    val harAktiveKjøringer: Boolean,
)

data class JobDetaljerDto(
    val jobNavn: String,
    val antallKjøringer: Long,
    val kjøringer: List<JobKjøringDto>,
)

data class JobKjøringDto(
    val kjøringId: Long?,
    val instansId: Long,
    val status: BatchStatus,
    val exitStatus: String,
    val exitBeskrivelse: String?,
    val startTidspunkt: LocalDateTime?,
    val sluttTidspunkt: LocalDateTime?,
    val opprettetTidspunkt: LocalDateTime,
    val varighetSekunder: Long?,
    val parametere: Map<String, JobParameterDto>,
    val steg: List<StegKjøringDto>,
    val feilmeldinger: List<String>,
)

data class StegKjøringDto(
    val stegNavn: String,
    val status: BatchStatus,
    val exitStatus: String,
    val exitBeskrivelse: String?,
    val lestAntall: Long,
    val skrevet: Long,
    val hoppetOverLes: Long,
    val hoppetOverProsesser: Long,
    val hoppetOverSkriv: Long,
    val filtrert: Long,
    val commitAntall: Long,
    val rollbackAntall: Long,
    val startTidspunkt: LocalDateTime?,
    val sluttTidspunkt: LocalDateTime?,
    val varighetSekunder: Long?,
    val feilmeldinger: List<String>,
)

data class JobParameterDto(
    val type: String,
    val verdi: String?,
    val identifiserende: Boolean,
)

data class AktivKjøringDto(
    val jobNavn: String,
    val kjøringId: Long?,
    val status: BatchStatus,
    val startTidspunkt: LocalDateTime?,
    val varighetSekunder: Long?,
    val parametere: Map<String, JobParameterDto>,
)

data class FremgangDto(
    val kjøringId: Long?,
    val jobNavn: String,
    val status: BatchStatus,
    val startTidspunkt: LocalDateTime?,
    val medgåttSekunder: Long?,
    val estimertGjenståendeSekunder: Long?,
    val estimertFerdigTidspunkt: LocalDateTime?,
    val gjennomsnittligVarighetSekunder: Long?,
    val steg: List<StegFremgangDto>,
)

data class StegFremgangDto(
    val stegNavn: String,
    val status: BatchStatus,
    val lestAntall: Long,
    val skrevet: Long,
    val hoppetOver: Long,
    val commitAntall: Long,
    val startTidspunkt: LocalDateTime?,
    val medgåttSekunder: Long?,
)

data class JobStatistikkDto(
    val jobNavn: String,
    val antallVellykket: Int,
    val antallFeilet: Int,
    val antallStoppet: Int,
    val gjennomsnittligVarighetSekunder: Long?,
    val minVarighetSekunder: Long?,
    val maxVarighetSekunder: Long?,
    val sisteVellykketTidspunkt: LocalDateTime?,
    val sisteFeiletTidspunkt: LocalDateTime?,
)
