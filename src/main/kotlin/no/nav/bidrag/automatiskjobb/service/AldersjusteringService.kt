package no.nav.bidrag.automatiskjobb.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.nav.bidrag.automatiskjobb.batch.utils.BatchConfiguration
import no.nav.bidrag.automatiskjobb.consumer.BidragPersonConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.controller.AldersjusteringerHvorBeløpBleRedusertRespons
import no.nav.bidrag.automatiskjobb.mapper.VedtakMapper
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Behandlingstype
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Forsendelsestype
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.persistence.entity.metadata.AldersjusteringMetadata
import no.nav.bidrag.automatiskjobb.persistence.entity.metadata.BeregningAvvikMetadata
import no.nav.bidrag.automatiskjobb.persistence.entity.metadata.SamværsklasseEndringMetadata
import no.nav.bidrag.automatiskjobb.persistence.entity.metadata.TidligereVedtakMetadata
import no.nav.bidrag.automatiskjobb.persistence.entity.metadata.UnderholdskostnadEndringMetadata
import no.nav.bidrag.automatiskjobb.persistence.entity.metadata.mergeMissingFrom
import no.nav.bidrag.automatiskjobb.persistence.entity.metadata.oppdaterMetadata
import no.nav.bidrag.automatiskjobb.persistence.repository.AldersjusteringRepository
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import no.nav.bidrag.automatiskjobb.service.model.AldersjusteringResponse
import no.nav.bidrag.automatiskjobb.service.model.AldersjusteringResultatResponse
import no.nav.bidrag.automatiskjobb.service.model.GrunnlagAvvikResultat
import no.nav.bidrag.automatiskjobb.service.model.SamværsklasseEndring
import no.nav.bidrag.automatiskjobb.service.model.UnderholdskostnadEndring
import no.nav.bidrag.automatiskjobb.utils.ugyldigForespørsel
import no.nav.bidrag.beregn.barnebidrag.service.orkestrering.AldersjusteresManueltException
import no.nav.bidrag.beregn.barnebidrag.service.orkestrering.AldersjusteringOrchestrator
import no.nav.bidrag.beregn.barnebidrag.service.orkestrering.BeregnBasertPåVedtak
import no.nav.bidrag.beregn.barnebidrag.service.orkestrering.SkalIkkeAldersjusteresException
import no.nav.bidrag.commons.util.RequestContextAsyncContext
import no.nav.bidrag.commons.util.SecurityCoroutineContext
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.sak.Stønadsid
import no.nav.bidrag.transport.automatiskjobb.AldersjusteringAldersjustertResultat
import no.nav.bidrag.transport.automatiskjobb.AldersjusteringIkkeAldersjustertResultat
import no.nav.bidrag.transport.automatiskjobb.AldersjusteringResultat
import no.nav.bidrag.transport.automatiskjobb.AldersjusteringResultatlisteResponse
import no.nav.bidrag.transport.automatiskjobb.HentAldersjusteringStatusRequest
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.KopiDelberegningUnderholdskostnad
import no.nav.bidrag.transport.behandling.felles.grunnlag.KopiSamværsperiodeGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.innholdTilObjekt
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientResponseException
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDate

private val LOGGER = KotlinLogging.logger {}

@Service
class AldersjusteringService(
    private val barnRepository: BarnRepository,
    private val alderjusteringRepository: AldersjusteringRepository,
    private val aldersjusteringOrchestrator: AldersjusteringOrchestrator,
    private val vedtakConsumer: BidragVedtakConsumer,
    private val sakConsumer: BidragSakConsumer,
    private val vedtakMapper: VedtakMapper,
    private val oppgaveService: OppgaveService,
    private val personConsumer: BidragPersonConsumer,
    private val forsendelseBestillingService: ForsendelseBestillingService,
) {
    fun hentAldersjusteringstatusForBarnOgSak(request: HentAldersjusteringStatusRequest): AldersjusteringResultatlisteResponse =
        AldersjusteringResultatlisteResponse(
            request.barnListe.map {
                val barn =
                    barnRepository.findByKravhaverAndSaksnummer(it.verdi, request.saksnummer.verdi)
                        ?: ugyldigForespørsel("Fant ikke barn med fødselsnummer $it og sak ${request.saksnummer}")
                val alderBarn = request.år - (barn.fødselsdato?.year ?: 0)
                val aldersjustering =
                    alderjusteringRepository.finnBarnAldersjustert(barn.id!!)
                        ?: ugyldigForespørsel("Fant ingen aldersjustering for barn med fødselsnummer $it og sak ${request.saksnummer}")

                if (aldersjustering.aldersgruppe != alderBarn) {
                    ugyldigForespørsel(
                        "Fant ingen aldersjustering for barn med fødselsnummer $it og sak ${request.saksnummer} for år ${request.år}",
                    )
                }

                val stønadsid =
                    Stønadsid(
                        Stønadstype.BIDRAG,
                        Personident(barn.kravhaver),
                        Personident(barn.skyldner!!),
                        Saksnummer(barn.saksnummer),
                    )
                if (aldersjustering.behandlingstype == Behandlingstype.FATTET_FORSLAG) {
                    AldersjusteringAldersjustertResultat(
                        aldersjustering.vedtak!!,
                        stønadsid,
                    )
                } else {
                    AldersjusteringIkkeAldersjustertResultat(
                        stønadsid,
                        aldersjustering.begrunnelseVisningsnavn.joinToString(", "),
                        aldersjusteresManuelt = aldersjustering.behandlingstype == Behandlingstype.MANUELL,
                    )
                }
            },
        )

    fun kjørAldersjusteringForSak(
        saksnummer: Saksnummer,
        år: Int,
        simuler: Boolean,
        stønadstype: Stønadstype,
    ): AldersjusteringResponse {
        val sak = sakConsumer.hentSak(saksnummer.verdi)
        val barnISaken = sak.roller.filter { it.type == Rolletype.BARN }
        val barnListe =
            barnISaken
                .flatMap { barnRepository.findAllByKravhaver(it.fødselsnummer!!.verdi) }
                .filter { riktigAldersgruppeForAldersjustering(it, år) }

        return opprettOgUtførAldersjusteringForBarn(
            barnListe,
            år,
            "aldersjustering-sak-$saksnummer",
            stønadstype,
            simuler,
        )
    }

    private fun riktigAldersgruppeForAldersjustering(
        barn: Barn,
        år: Int,
    ): Boolean =
        when {
            barn.fødselsdato?.year?.let { år - it } == 6 -> true
            barn.fødselsdato?.year?.let { år - it } == 11 -> true
            barn.fødselsdato?.year?.let { år - it } == 15 -> true
            else -> false
        }

    private fun opprettOgUtførAldersjusteringForBarn(
        barnListe: List<Barn>,
        år: Int,
        batchId: String,
        stønadstype: Stønadstype,
        simuler: Boolean,
    ): AldersjusteringResponse {
        barnListe.forEach {
            opprettAldersjusteringForBarn(
                it,
                år,
                batchId,
                stønadstype,
            )
        }

        val aldersjusteringListe =
            alderjusteringRepository
                .finnForFlereStatuserOgBarnId(
                    listOf(
                        Status.SLETTET,
                        Status.UBEHANDLET,
                        Status.FEILET,
                        Status.SIMULERT,
                    ),
                    barnListe.mapNotNull { it.id },
                ).toMutableList()

        val resultat =
            aldersjusteringListe.map {
                utførAldersjustering(it, stønadstype, simuler)
            }

        return opprettAldersjusteringResponse(resultat)
    }

    fun opprettAldersjusteringForBarn(
        barn: Barn,
        år: Int,
        batchId: String,
        stønadstype: Stønadstype,
    ): Aldersjustering? {
        val aldersgruppe = barn.fødselsdato?.year?.let { år - it }

        if (aldersgruppe == null) {
            LOGGER.error {
                "Aldersjustering for barn ${barn.id} kan ikke opprettes. Barnet har ingen fødselsdato og aldersgruppe kan derfor ikke settes."
            }
            return null
        }

        if (!alderjusteringRepository.existsAldersjusteringsByBarnAndAldersgruppe(barn.id!!, aldersgruppe)) {
            val aldersjustering =
                alderjusteringRepository.save(
                    Aldersjustering(
                        batchId = batchId,
                        barn = barn,
                        aldersgruppe = aldersgruppe,
                        status = Status.UBEHANDLET,
                        stønadstype = stønadstype,
                    ),
                )
            LOGGER.info { "Opprettet aldersjustering ${aldersjustering.id} for barn ${barn.id}." }
            return aldersjustering
        } else {
            LOGGER.info { "Aldersjustering for barn ${barn.id} er allerede opprettet." }
            return null
        }
    }

    fun utførAldersjustering(
        aldersjustering: Aldersjustering,
        stønadstype: Stønadstype,
        simuler: Boolean,
    ): AldersjusteringResultat {
        val barn = aldersjustering.barn
        if (barn.skyldner == null) {
            val errorMessage = "Mangler skyldner"
            val stønadsid =
                Stønadsid(
                    stønadstype,
                    Personident(barn.kravhaver),
                    Personident(barn.kravhaver),
                    Saksnummer(barn.saksnummer),
                )
            aldersjustering.status = Status.FEILET
            aldersjustering.behandlingstype = Behandlingstype.FEILET
            aldersjustering.begrunnelse = listOf(errorMessage)

            alderjusteringRepository.save(aldersjustering)

            return AldersjusteringIkkeAldersjustertResultat(stønadsid, errorMessage)
        }
        val stønadsid = barn.tilStønadsid(stønadstype)

        try {
            val (vedtaksidBeregning, løpendeBeløp, resultatBeregning, resultatSisteVedtak) =
                aldersjusteringOrchestrator.utførAldersjustering(
                    stønadsid,
                    aldersjustering.aldersjusteresForÅr,
                )

            aldersjustering.vedtaksidBeregning = vedtaksidBeregning
            aldersjustering.lopendeBelop = løpendeBeløp
            aldersjustering.status = if (simuler) Status.SIMULERT else Status.BEHANDLET
            aldersjustering.behandlingstype = Behandlingstype.FATTET_FORSLAG
            aldersjustering.begrunnelse = emptyList()
            aldersjustering.resultatSisteVedtak = resultatSisteVedtak

            val vedtaksforslagRequest =
                vedtakMapper.tilOpprettVedtakRequest(resultatBeregning, stønadsid, aldersjustering)

            val vedtaksid =
                if (simuler) null else vedtakConsumer.opprettEllerOppdaterVedtaksforslag(vedtaksforslagRequest)
            aldersjustering.vedtak = vedtaksid

            alderjusteringRepository.save(aldersjustering)

            return AldersjusteringAldersjustertResultat(vedtaksid ?: -1, stønadsid, vedtaksforslagRequest)
        } catch (e: SkalIkkeAldersjusteresException) {
            aldersjustering.vedtaksidBeregning = e.vedtaksid
            aldersjustering.status = if (simuler) Status.SIMULERT else Status.BEHANDLET
            aldersjustering.behandlingstype = Behandlingstype.INGEN
            aldersjustering.begrunnelse = e.begrunnelser.map { it.name }
            aldersjustering.resultatSisteVedtak = e.resultat

            val vedtaksforslagRequest =
                vedtakMapper.tilOpprettVedtakRequestIngenAldersjustering(aldersjustering)
            val vedtaksid =
                if (simuler) null else vedtakConsumer.opprettEllerOppdaterVedtaksforslag(vedtaksforslagRequest)
            aldersjustering.vedtak = vedtaksid

            alderjusteringRepository.save(aldersjustering)

            LOGGER.warn(e) {
                "Stønad $stønadsid skal ikke aldersjusteres med begrunnelse ${e.begrunnelser.joinToString(", ")}"
            }
            return AldersjusteringIkkeAldersjustertResultat(stønadsid, e.begrunnelser.joinToString(", "))
        } catch (e: AldersjusteresManueltException) {
            aldersjustering.vedtaksidBeregning = e.vedtaksid
            aldersjustering.status = if (simuler) Status.SIMULERT else Status.BEHANDLET
            aldersjustering.behandlingstype = Behandlingstype.MANUELL
            aldersjustering.begrunnelse = listOf(e.begrunnelse.name)
            aldersjustering.resultatSisteVedtak = e.resultat

            val vedtaksforslagRequest =
                vedtakMapper.tilOpprettVedtakRequestIngenAldersjustering(aldersjustering)
            val vedtaksid =
                if (simuler) null else vedtakConsumer.opprettEllerOppdaterVedtaksforslag(vedtaksforslagRequest)
            aldersjustering.vedtak = vedtaksid

            alderjusteringRepository.save(aldersjustering)

            LOGGER.warn(e) { "Stønad $stønadsid skal aldersjusteres manuelt med begrunnelse ${e.begrunnelse}" }
            return AldersjusteringIkkeAldersjustertResultat(stønadsid, e.begrunnelse.name)
        } catch (e: Exception) {
            aldersjustering.status = Status.FEILET
            aldersjustering.behandlingstype = Behandlingstype.FEILET
            aldersjustering.begrunnelse = listOf(e.message ?: "Ukjent feil")
            alderjusteringRepository.save(aldersjustering)

            LOGGER.error(e) { "Det skjedde en feil ved aldersjustering for stønad $stønadsid" }
            return AldersjusteringIkkeAldersjustertResultat(stønadsid, "Teknisk feil: ${e.message}")
        }
    }

    fun fattVedtakOmAldersjustering(
        aldersjustering: Aldersjustering,
        simuler: Boolean,
    ) {
        if (simuler) {
            LOGGER.info {
                "Simulering er satt til true. Fatter ikke vedtaksforslag men " +
                    "oppretter forsendelse bestillinger for aldersjustering ${aldersjustering.id} med behandlingstype ${aldersjustering.behandlingstype}"
            }
        } else {
            LOGGER.info {
                "Fatter vedtak for aldersjustering ${aldersjustering.id} og vedtaksid ${aldersjustering.vedtak} med behandlingstype ${aldersjustering.behandlingstype}"
            }
            try {
                vedtakConsumer.fatteVedtaksforslag(
                    aldersjustering.vedtak ?: error("Aldersjustering ${aldersjustering.id} mangler vedtak!"),
                )
                aldersjustering.status = Status.FATTET
                aldersjustering.fattetTidspunkt = Timestamp(System.currentTimeMillis())
            } catch (e: Exception) {
                val feilmelding =
                    if (e is RestClientResponseException) {
                        "Feil ved fatting av vedtak for aldersjustering ${aldersjustering.id}: ${e.message}. " +
                            hentFeilmeldingFraWarningHeader(e)
                    } else {
                        "Feil ved fatting av vedtak for aldersjustering ${aldersjustering.id}: ${e.message}"
                    }
                LOGGER.error(e) { feilmelding }

                aldersjustering.status = Status.FATTE_VEDTAK_FEILET
                alderjusteringRepository.save(aldersjustering)
                throw e
            }
        }

        if (aldersjustering.behandlingstype == Behandlingstype.FATTET_FORSLAG) {
            val forsendelseBestillinger =
                forsendelseBestillingService.opprettBestilling(
                    aldersjustering,
                    Forsendelsestype.ALDERSJUSTERING_BIDRAG,
                )
            aldersjustering.forsendelseBestilling.addAll(forsendelseBestillinger)
        }
        alderjusteringRepository.save(aldersjustering)
    }

    private fun hentFeilmeldingFraWarningHeader(exception: RestClientResponseException): String =
        exception.responseHeaders?.get("Warning")?.let {
            "Detaljer: ${it.joinToString(", ")}"
        } ?: ""

    fun slettOppgaveForAldersjustering(aldersjustering: Aldersjustering): Int? {
        if (aldersjustering.oppgave == null) {
            LOGGER.info { "Ingen oppgave å slette for aldersjustering ${aldersjustering.id}" }
            return null
        }
        try {
            val oppgaveId = oppgaveService.slettOppgave(aldersjustering.oppgave!!)
            aldersjustering.oppgave = null
            alderjusteringRepository.save(aldersjustering)
            return oppgaveId.toInt()
        } catch (e: Exception) {
            LOGGER.error(e) { "Feil ved sletting av oppgave for aldersjustering ${aldersjustering.id}" }
            throw e
        }
    }

    fun opprettOppgaveForAldersjustering(aldersjustering: Aldersjustering): Int {
        val oppgaveId = oppgaveService.opprettOppgaveForManuellAldersjustering(aldersjustering) //

        aldersjustering.oppgave = oppgaveId
        alderjusteringRepository.save(aldersjustering)
        return oppgaveId
    }

    private fun opprettAldersjusteringResponse(resultat: List<AldersjusteringResultat>): AldersjusteringResponse =
        AldersjusteringResponse(
            aldersjustert =
                resultat.filterIsInstance<AldersjusteringAldersjustertResultat>().let {
                    AldersjusteringResultatResponse(
                        antall = it.size,
                        stønadsider = it.map { barn -> barn.stønadsid },
                        detaljer = it,
                    )
                },
            ikkeAldersjustert =
                resultat.filterIsInstance<AldersjusteringIkkeAldersjustertResultat>().let {
                    AldersjusteringResultatResponse(
                        antall = it.size,
                        stønadsider = it.map { barn -> barn.stønadsid },
                        detaljer = it,
                    )
                },
        )

    fun slettVedtaksforslag(aldersjustering: Aldersjustering): Aldersjustering? {
        val barn = aldersjustering.barn

        LOGGER.info { "Aldersjustering for barn ${barn.id} med stønadsid: $aldersjustering. skal slettes. Sletter.." }

        vedtakConsumer.hentVedtaksforslagBasertPåReferanase(aldersjustering.unikReferanse)?.let {
            LOGGER.info {
                "Fant eksisterende vedtaksforslag med referanse ${aldersjustering.unikReferanse} og id ${it.vedtaksid}. Sletter eksisterende vedtaksforslag "
            }
            vedtakConsumer.slettVedtaksforslag(it.vedtaksid)
        } ?: run {
            LOGGER.error { "Fant ikke eksisterende vedtaksforslag med referanse ${aldersjustering.unikReferanse}" }
            aldersjustering.vedtak = null
            aldersjustering.status = Status.SLETTET
            alderjusteringRepository.save(aldersjustering)
            return null
        }
        aldersjustering.vedtak = null
        aldersjustering.status = Status.SLETTET
        return alderjusteringRepository.save(aldersjustering)
    }

    fun hentAlleBarnSomSkalAldersjusteresForÅr(
        år: Int,
        paging: Pageable = Pageable.unpaged(),
    ): Map<Int, List<Barn>> {
        val result =
            barnRepository
                .finnBarnSomSkalAldersjusteresForÅr(år, pageable = paging)
                .filter { it.fødselsdato != null }
                .groupBy { år - it.fødselsdato!!.year }
                .mapValues { it.value.sortedBy { barn -> barn.fødselsdato } }

        LOGGER.info { "Antall barn ${result.getLengths()}" }
        return result
    }

    fun erBidragRedusert(aldersjustering: Aldersjustering): Boolean = hentRedusertBeløpSak(aldersjustering) != null

    suspend fun hentAlleAldersjusteringerHvorBeløpErRedusert(): AldersjusteringerHvorBeløpBleRedusertRespons {
        val aldersjusteringer =
            withContext(Dispatchers.IO) {
                alderjusteringRepository
                    .finnAlleForBehandlingstypeOgStatus(
                        listOf(Behandlingstype.FATTET_FORSLAG, Behandlingstype.MANUELL),
                        listOf(Status.BEHANDLET),
                        Pageable.unpaged(),
                    )
            }.toList()

        return coroutineScope {
            val deferredResults =
                aldersjusteringer
                    .map { a ->
                        async(Dispatchers.IO + SecurityCoroutineContext() + RequestContextAsyncContext()) {
                            hentRedusertBeløpSak(a)
                        }
                    }
            val resultat = deferredResults.awaitAll().filterNotNull()
            AldersjusteringerHvorBeløpBleRedusertRespons(resultat.size, resultat)
        }
    }

    private fun hentRedusertBeløpSak(aldersjustering: Aldersjustering): Map<String, Any?>? =
        try {
            val vedtak = vedtakConsumer.hentVedtak(aldersjustering.vedtak ?: return null) ?: return null
            val stønadsendring = vedtak.stønadsendringListe.find { it.kravhaver.verdi == aldersjustering.barn.kravhaver } ?: return null
            val sistePeriode = stønadsendring.periodeListe.maxByOrNull { it.periode.fom }
            val sisteBeløp = sistePeriode?.beløp ?: return null
            val løpendeBeløp = aldersjustering.lopendeBelop ?: return null

            if (løpendeBeløp > sisteBeløp) {
                mapOf(
                    "saksnummer" to aldersjustering.barn.saksnummer,
                    "løpendeBeløp" to løpendeBeløp,
                    "aldersjustertBeløp" to sisteBeløp,
                    "skalBehandlesManuelt" to if (aldersjustering.behandlingstype == Behandlingstype.MANUELL) "Ja" else "Nei",
                )
            } else {
                null
            }
        } catch (e: Exception) {
            LOGGER.error(e) { "Feil ved henting av aldersjusterte beløp for aldersjustering ${aldersjustering.id}" }
            null
        }

    fun hentAntallBarnSomSkalAldersjusteresForÅr(år: Int): Map<Int, Int> =
        barnRepository
            .finnBarnSomSkalAldersjusteresForÅr(år, pageable = Pageable.unpaged())
            .filter { it.fødselsdato != null }
            .groupBy { år - it.fødselsdato!!.year }
            .mapValues { it.value.size }

    fun hentAldersjustering(id: Int): Aldersjustering? = alderjusteringRepository.findById(id).orElseGet { null }

    fun lagreAldersjustering(aldersjustering: Aldersjustering): Int? = alderjusteringRepository.save(aldersjustering).id

    fun kjørAldersjusteringForSakDebug(
        saksnummer: Saksnummer,
        år: Int,
        simuler: Boolean,
        stønadstype: Stønadstype,
    ): AldersjusteringResponse {
        val sak = sakConsumer.hentSak(saksnummer.verdi)
        val barnISaken = sak.roller.filter { it.type == Rolletype.BARN }
        val bp =
            sak.roller.find { it.type == Rolletype.BIDRAGSPLIKTIG }
                ?: ugyldigForespørsel("Fant ikke BP for sak $saksnummer")
        val resultat =
            barnISaken.map {
                utførAldersjusteringForBarnDebug(
                    stønadstype,
                    Barn(
                        kravhaver = it.fødselsnummer!!.verdi,
                        skyldner = bp.fødselsnummer!!.verdi,
                        saksnummer = saksnummer.verdi,
                        fødselsdato = personConsumer.hentFødselsdatoForPerson(it.fødselsnummer!!) ?: LocalDate.now(),
                    ),
                    år,
                    "aldersjustering-sak-$saksnummer",
                    simuler,
                )
            }

        return AldersjusteringResponse(
            aldersjustert =
                resultat.filterIsInstance<AldersjusteringAldersjustertResultat>().let {
                    AldersjusteringResultatResponse(
                        antall = it.size,
                        stønadsider = it.map { barn -> barn.stønadsid },
                        detaljer = it,
                    )
                },
            ikkeAldersjustert =
                resultat.filterIsInstance<AldersjusteringIkkeAldersjustertResultat>().let {
                    AldersjusteringResultatResponse(
                        antall = it.size,
                        stønadsider = it.map { barn -> barn.stønadsid },
                        detaljer = it,
                    )
                },
        )
    }

    fun utførAldersjusteringForBarnDebug(
        stønadstype: Stønadstype,
        barn: Barn,
        år: Int,
        batchId: String,
        simuler: Boolean = true,
    ): AldersjusteringResultat {
        val stønadsid =
            Stønadsid(
                stønadstype,
                Personident(barn.kravhaver),
                Personident(barn.skyldner!!),
                Saksnummer(barn.saksnummer),
            )
        try {
            val (_, _, resultatBeregning) = aldersjusteringOrchestrator.utførAldersjustering(stønadsid, år)
            val vedtaksforslagRequest =
                vedtakMapper.tilOpprettVedtakRequest(
                    resultatBeregning,
                    stønadsid,
                    Aldersjustering(
                        batchId = batchId,
                        barn = barn,
                        status = Status.BEHANDLET,
                        aldersgruppe = barn.fødselsdato!!.year + år,
                        stønadstype = stønadstype,
                    ),
                )
            if (simuler) {
                LOGGER.info { "Kjører aldersjustering i simuleringsmodus. Oppretter ikke vedtaksforslag" }
                return AldersjusteringAldersjustertResultat(-1, stønadsid, vedtaksforslagRequest)
            }

            val vedtaksid = vedtakConsumer.opprettEllerOppdaterVedtaksforslag(vedtaksforslagRequest)
            return AldersjusteringAldersjustertResultat(vedtaksid, stønadsid, vedtaksforslagRequest)
        } catch (e: SkalIkkeAldersjusteresException) {
            LOGGER.warn(e) {
                "Stønad $stønadsid skal ikke aldersjusteres med begrunnelse ${
                    e.begrunnelser.joinToString(
                        ", ",
                    )
                }"
            }
            return AldersjusteringIkkeAldersjustertResultat(stønadsid, e.begrunnelser.joinToString(", "))
        } catch (e: AldersjusteresManueltException) {
            LOGGER.warn(e) { "Stønad $stønadsid skal aldersjusteres manuelt med begrunnelse ${e.begrunnelse}" }
            return AldersjusteringIkkeAldersjustertResultat(stønadsid, e.begrunnelse.name, aldersjusteresManuelt = true)
        } catch (e: Exception) {
            LOGGER.error(e) { "Det skjedde en feil ved aldersjustering for stønad $stønadsid" }
            return AldersjusteringIkkeAldersjustertResultat(stønadsid, "Teknisk feil: ${e.message}")
        }
    }

    fun startResetBeregningEtterAvvik(aldersjusteringIder: List<Int>) {
        LOGGER.info { "Starter resett av aldersjustering etter avvik for ${aldersjusteringIder.size} aldersjusteringer" }
        CoroutineScope(Dispatchers.IO + SecurityCoroutineContext()).launch {
            for (id in aldersjusteringIder) {
                try {
                    val aldersjustering =
                        alderjusteringRepository.findById(id).orElse(null)
                            ?: run {
                                LOGGER.warn { "Fant ikke aldersjustering $id — hopper over" }
                                continue
                            }

                    if (aldersjustering.status != Status.FATTET) {
                        LOGGER.warn {
                            "Aldersjustering $id har status ${aldersjustering.status}, ikke FATTET — hopper over"
                        }
                        continue
                    }

                    val avvik = aldersjustering.metadata?.beregningAvvik
                    if (avvik?.samværsklasseEndring == null && avvik?.underholdskostnadEndring == null) {
                        LOGGER.warn {
                            "Aldersjustering $id sak ${aldersjustering.barn.saksnummer} har ingen registrerte avvik " +
                                "i metadata (samværsklasseEndring og underholdskostnadEndring er begge null) — hopper over"
                        }
                        continue
                    }

                    // Lagre snapshot av det opprinnelige fattet-vedtaket i metadata
                    val opprinneligBatchId =
                        aldersjustering.metadata?.tidligereVedtak?.batchId ?: aldersjustering.batchId
                    if (aldersjustering.metadata?.tidligereVedtak == null) {
                        aldersjustering.oppdaterMetadata(
                            AldersjusteringMetadata(
                                tidligereVedtak =
                                    TidligereVedtakMetadata(
                                        vedtaksid = aldersjustering.vedtak!!,
                                        vedtaksidBeregning = aldersjustering.vedtaksidBeregning!!,
                                        fattetTidspunkt = aldersjustering.fattetTidspunkt.toString(),
                                        batchId = aldersjustering.batchId,
                                    ),
                            ),
                        )
                    }

                    // Endre batch_id slik at unikReferanse ikke kolliderer med det opprinnelige vedtaket
                    aldersjustering.batchId = "${opprinneligBatchId}_ny_beregning_etter_avvik"

                    // Tilbakestill til ubehandlet slik at beregning kan kjøres på nytt
                    aldersjustering.status = Status.UBEHANDLET
                    aldersjustering.fattetTidspunkt = null
                    aldersjustering.vedtak = null
                    aldersjustering.vedtaksidBeregning = null
                    aldersjustering.behandlingstype = null
                    alderjusteringRepository.save(aldersjustering)

                    LOGGER.info {
                        "Reset etter avvik fullført for aldersjustering $id sak ${aldersjustering.barn.saksnummer} " +
                            "(nyBatchId=${aldersjustering.batchId})"
                    }
                } catch (e: Exception) {
                    LOGGER.error(e) { "Feil ved ny beregning etter avvik for aldersjustering $id" }
                }
            }
            LOGGER.info { "Ny beregning etter avvik fullført" }
        }
    }

    fun startVerifiserAldersjusteringerForÅr(år: Int) {
        LOGGER.info { "Starter verifisering av aldersjusteringer for år $år i bakgrunnen" }
        CoroutineScope(Dispatchers.IO + SecurityCoroutineContext()).launch {
            try {
                val alleAldersjusteringer = alderjusteringRepository.finnAlleFattetForÅr(år).content
                LOGGER.info { "Verifiserer ${alleAldersjusteringer.size} aldersjusteringer med status FATTET for år $år" }

                data class Resultat(
                    val avvik: GrunnlagAvvikResultat? = null,
                    val feilet: Boolean = false,
                )

                val resultater = mutableListOf<Resultat>()
                for (chunk in alleAldersjusteringer.chunked(BatchConfiguration.GRID_SIZE)) {
                    val chunkResultater =
                        coroutineScope {
                            chunk
                                .map { aldersjustering ->
                                    async {
                                        val barn = aldersjustering.barn
                                        if (barn.skyldner == null || aldersjustering.vedtaksidBeregning == null ||
                                            aldersjustering.vedtak == null
                                        ) {
                                            LOGGER.warn {
                                                "Hopper over aldersjustering ${aldersjustering.id} for sak ${barn.saksnummer} " +
                                                    "— mangler skyldner, vedtaksidBeregning eller vedtak"
                                            }
                                            return@async Resultat(feilet = true)
                                        }
                                        try {
                                            val stønadsid = barn.tilStønadsid(aldersjustering.stønadstype)

                                            val nyBeregning =
                                                aldersjusteringOrchestrator
                                                    .utførAldersjustering(
                                                        stønadsid,
                                                        aldersjustering.aldersjusteresForÅr,
                                                        BeregnBasertPåVedtak(vedtaksid = aldersjustering.vedtaksidBeregning),
                                                    ).beregning

                                            val origVedtak = vedtakConsumer.hentVedtak(aldersjustering.vedtak!!)
                                            if (origVedtak == null) {
                                                LOGGER.warn {
                                                    "Fant ikke originalt vedtak ${aldersjustering.vedtak} for aldersjustering ${aldersjustering.id}"
                                                }
                                                return@async Resultat(feilet = true)
                                            }

                                            val origGrunnlag = origVedtak.grunnlagListe
                                            val nyGrunnlag = nyBeregning?.grunnlagListe ?: emptyList()

                                            val samværsEndringer =
                                                sammenlignSamværsklasse(
                                                    origGrunnlag.filter { it.type == Grunnlagstype.KOPI_SAMVÆRSPERIODE },
                                                    nyGrunnlag.filter { it.type == Grunnlagstype.KOPI_SAMVÆRSPERIODE },
                                                )
                                            val underholdEndringer =
                                                sammenlignUnderholdskostnad(
                                                    origGrunnlag.filter { it.type == Grunnlagstype.KOPI_DELBEREGNING_UNDERHOLDSKOSTNAD },
                                                    nyGrunnlag.filter { it.type == Grunnlagstype.KOPI_DELBEREGNING_UNDERHOLDSKOSTNAD },
                                                )

                                            aldersjustering.oppdaterMetadata(
                                                lagMetadataForBeregningAvvik(
                                                    år = år,
                                                    nyttBeløp =
                                                        nyBeregning.beregnetBarnebidragPeriodeListe
                                                            .first()
                                                            .resultat.beløp,
                                                    gammelBeløp =
                                                        origVedtak.stønadsendringListe
                                                            .first()
                                                            .periodeListe
                                                            .first()
                                                            .beløp,
                                                    saksnummer = aldersjustering.barn.saksnummer,
                                                    samværsEndringer = samværsEndringer,
                                                    underholdEndringer = underholdEndringer,
                                                ),
                                            )
                                            alderjusteringRepository.save(aldersjustering)

                                            if (samværsEndringer.isNotEmpty() || underholdEndringer.isNotEmpty()) {
                                                LOGGER.warn {
                                                    "Avvik funnet for aldersjustering ${aldersjustering.id} sak ${barn.saksnummer}: " +
                                                        "samværsklasse=$samværsEndringer underholdskostnad=$underholdEndringer"
                                                }
                                                Resultat(
                                                    avvik =
                                                        GrunnlagAvvikResultat(
                                                            aldersjusteringId = aldersjustering.id!!,
                                                            saksnummer = barn.saksnummer,
                                                            kravhaver = barn.kravhaver,
                                                            samværsklasseEndringer = samværsEndringer,
                                                            underholdskostnadEndringer = underholdEndringer,
                                                        ),
                                                )
                                            } else {
                                                Resultat()
                                            }
                                        } catch (e: Exception) {
                                            LOGGER.error(e) {
                                                "Feil ved verifisering av aldersjustering ${aldersjustering.id} for sak ${barn.saksnummer}"
                                            }
                                            Resultat(feilet = true)
                                        }
                                    }
                                }.awaitAll()
                        }
                    resultater.addAll(chunkResultater)
                }

                val avvik = resultater.mapNotNull { it.avvik }
                val antallFeilet = resultater.count { it.feilet }

                LOGGER.info {
                    "Verifisering fullført for år $år — totalt=${resultater.size} avvik=${avvik.size} feilet=$antallFeilet. "
                }
            } catch (e: Exception) {
                LOGGER.warn(e) { "Verifisering av aldersjusteringer for år $år feilet" }
            }
        }
    }

    private fun lagMetadataForBeregningAvvik(
        år: Int,
        saksnummer: String,
        gammelBeløp: BigDecimal?,
        nyttBeløp: BigDecimal?,
        samværsEndringer: List<SamværsklasseEndring>,
        underholdEndringer: List<UnderholdskostnadEndring>,
    ): AldersjusteringMetadata =
        AldersjusteringMetadata(
            beregningAvvik =
                BeregningAvvikMetadata(
                    år = år,
                    sjekket = true,
                    nyttBeløp = nyttBeløp,
                    gammelBeløp = gammelBeløp,
                    saksnummer = saksnummer,
                    samværsklasseEndring =
                        samværsEndringer.firstOrNull()?.let {
                            SamværsklasseEndringMetadata(
                                gammelKlasse = it.gammelKlasse,
                                nyKlasse = it.nyKlasse,
                            )
                        },
                    underholdskostnadEndring =
                        underholdEndringer.firstOrNull()?.let {
                            UnderholdskostnadEndringMetadata(
                                gammelNettoTilsynsutgift = it.gammelNettoTilsynsutgift,
                                nyNettoTilsynsutgift = it.nyNettoTilsynsutgift,
                                gammelBarnetilsynMedStønad = it.gammelBarnetilsynMedStønad,
                                nyBarnetilsynMedStønad = it.nyBarnetilsynMedStønad,
                            )
                        },
                ),
        )

    private fun sammenlignSamværsklasse(
        orig: List<GrunnlagDto>,
        ny: List<GrunnlagDto>,
    ): List<SamværsklasseEndring> {
        val origMap = orig.mapNotNull { g -> g.innholdTilObjekt<KopiSamværsperiodeGrunnlag>() }.associateBy { it.periode }
        val nyMap = ny.mapNotNull { g -> g.innholdTilObjekt<KopiSamværsperiodeGrunnlag>() }.associateBy { it.periode }
        val allePerioder = (origMap.keys + nyMap.keys).distinct()
        return allePerioder.mapNotNull { periode ->
            val gammelKlasse = origMap[periode]?.samværsklasse
            val nyKlasse = nyMap[periode]?.samværsklasse
            if (gammelKlasse != nyKlasse) {
                SamværsklasseEndring(periode = periode, gammelKlasse = gammelKlasse, nyKlasse = nyKlasse)
            } else {
                null
            }
        }
    }

    private fun sammenlignUnderholdskostnad(
        orig: List<GrunnlagDto>,
        ny: List<GrunnlagDto>,
    ): List<UnderholdskostnadEndring> {
        val origMap = orig.mapNotNull { g -> g.innholdTilObjekt<KopiDelberegningUnderholdskostnad>() }.associateBy { it.periode }
        val nyMap = ny.mapNotNull { g -> g.innholdTilObjekt<KopiDelberegningUnderholdskostnad>() }.associateBy { it.periode }
        val allePerioder = (origMap.keys + nyMap.keys).distinct()
        return allePerioder.mapNotNull { periode ->
            val gammel = origMap[periode]
            val nyUnderhold = nyMap[periode]
            if (gammel?.nettoTilsynsutgift != nyUnderhold?.nettoTilsynsutgift ||
                gammel?.barnetilsynMedStønad != nyUnderhold?.barnetilsynMedStønad
            ) {
                UnderholdskostnadEndring(
                    periode = periode,
                    gammelNettoTilsynsutgift = gammel?.nettoTilsynsutgift,
                    nyNettoTilsynsutgift = nyUnderhold?.nettoTilsynsutgift,
                    gammelBarnetilsynMedStønad = gammel?.barnetilsynMedStønad,
                    nyBarnetilsynMedStønad = nyUnderhold?.barnetilsynMedStønad,
                )
            } else {
                null
            }
        }
    }
}

fun Map<Int, List<Barn>>.getLengths(): Map<Int, Int> = this.mapValues { it.value.size }
