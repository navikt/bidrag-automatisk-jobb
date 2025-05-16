package no.nav.bidrag.automatiskjobb.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.combinedLogger
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.mapper.VedtakMapper
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.Behandlingstype
import no.nav.bidrag.automatiskjobb.persistence.entity.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.AldersjusteringRepository
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import no.nav.bidrag.automatiskjobb.service.model.AldersjusteringResponse
import no.nav.bidrag.automatiskjobb.service.model.AldersjusteringResultatResponse
import no.nav.bidrag.automatiskjobb.service.model.OpprettVedtakConflictResponse
import no.nav.bidrag.automatiskjobb.utils.ugyldigForespørsel
import no.nav.bidrag.beregn.barnebidrag.service.AldersjusteresManueltException
import no.nav.bidrag.beregn.barnebidrag.service.AldersjusteringOrchestrator
import no.nav.bidrag.beregn.barnebidrag.service.SkalIkkeAldersjusteresException
import no.nav.bidrag.commons.util.secureLogger
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
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettVedtakRequestDto
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpStatusCodeException

private val log = KotlinLogging.logger {}

@Service
class AldersjusteringService(
    private val barnRepository: BarnRepository,
    private val alderjusteringRepository: AldersjusteringRepository,
    private val aldersjusteringOrchestrator: AldersjusteringOrchestrator,
    private val vedtakConsumer: BidragVedtakConsumer,
    private val sakConsumer: BidragSakConsumer,
    private val vedtakMapper: VedtakMapper,
    private val oppgaveService: OppgaveService,
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
            log.error {
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
            log.info { "Opprettet aldersjustering ${aldersjustering.id} for barn ${barn.id}." }
            return aldersjustering
        } else {
            log.info { "Aldersjustering for barn ${barn.id} er allerede opprettet." }
            return null
        }
    }

    @Transactional
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
        val stønadsid =
            Stønadsid(
                stønadstype,
                Personident(barn.kravhaver),
                Personident(barn.skyldner!!),
                Saksnummer(barn.saksnummer),
            )

        try {
            val (vedtaksidBeregning, løpendeBeløp, resultatBeregning, resultatSisteVedtak) =
                aldersjusteringOrchestrator.utførAldersjustering(
                    stønadsid,
                    barn.fødselsdato!!.year + aldersjustering.aldersgruppe,
                )

            val vedtaksforslagRequest =
                vedtakMapper.tilOpprettVedtakRequest(resultatBeregning, stønadsid, aldersjustering.batchId)

            val vedtaksid = if (simuler) null else opprettEllerOppdaterVedtaksforslag(vedtaksforslagRequest)

            aldersjustering.vedtaksidBeregning = vedtaksidBeregning
            aldersjustering.lopendeBelop = løpendeBeløp
            aldersjustering.vedtak = vedtaksid
            aldersjustering.status = if (simuler) Status.SIMULERT else Status.BEHANDLET
            aldersjustering.behandlingstype = Behandlingstype.FATTET_FORSLAG
            aldersjustering.begrunnelse = emptyList()
            aldersjustering.resultatSisteVedtak = resultatSisteVedtak

            return AldersjusteringAldersjustertResultat(vedtaksid ?: -1, stønadsid, vedtaksforslagRequest)
        } catch (e: SkalIkkeAldersjusteresException) {
            aldersjustering.vedtaksidBeregning = e.vedtaksid
            aldersjustering.status = if (simuler) Status.SIMULERT else Status.BEHANDLET
            aldersjustering.behandlingstype = Behandlingstype.INGEN
            aldersjustering.begrunnelse = e.begrunnelser.map { it.name }
            aldersjustering.resultatSisteVedtak = e.resultat

            combinedLogger.warn(e) {
                "Stønad $stønadsid skal ikke aldersjusteres med begrunnelse ${e.begrunnelser.joinToString(", ")}"
            }
            return AldersjusteringIkkeAldersjustertResultat(stønadsid, e.begrunnelser.joinToString(", "))
        } catch (e: AldersjusteresManueltException) {
            aldersjustering.vedtaksidBeregning = e.vedtaksid
            aldersjustering.status = if (simuler) Status.SIMULERT else Status.BEHANDLET
            aldersjustering.behandlingstype = Behandlingstype.MANUELL
            aldersjustering.begrunnelse = listOf(e.begrunnelse.name)
            aldersjustering.resultatSisteVedtak = e.resultat

            combinedLogger.warn(e) { "Stønad $stønadsid skal aldersjusteres manuelt med begrunnelse ${e.begrunnelse}" }
            return AldersjusteringIkkeAldersjustertResultat(stønadsid, e.begrunnelse.name)
        } catch (e: Exception) {
            aldersjustering.status = Status.FEILET
            aldersjustering.behandlingstype = Behandlingstype.FEILET
            aldersjustering.begrunnelse = listOf(e.message ?: "Ukjent feil")

            combinedLogger.error(e) { "Det skjedde en feil ved aldersjustering for stønad $stønadsid" }
            return AldersjusteringIkkeAldersjustertResultat(stønadsid, "Teknisk feil: ${e.message}")
        }
    }

    fun fattVedtakOmAldersjustering(aldersjustering: Aldersjustering) {
        val sak = sakConsumer.hentSak(aldersjustering.barn.saksnummer)

        combinedLogger.info { "Fatter vedtak for aldersjustering ${aldersjustering.id} og vedtaksid ${aldersjustering.vedtak}" }
//        vedtakConsumer.fatteVedtaksforslag(
//            aldersjustering.vedtak ?: error("Aldersjustering ${aldersjustering.id} mangler vedtak!"),
//        )
//        aldersjustering.status = Status.FATTET
//        aldersjustering.fattetTidspunkt = Timestamp(System.currentTimeMillis())
//        alderjusteringRepository.save(aldersjustering)

        sak.roller.filter { it.type == Rolletype.BIDRAGSPLIKTIG || it.type == Rolletype.BIDRAGSMOTTAKER }.forEach {
            forsendelseBestillingService.opprettForsendelseBestilling(
                aldersjustering,
                it.fødselsnummer!!.verdi,
                it.type,
            )
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

    @Transactional
    fun slettVedtaksforslag(
        stønadstype: Stønadstype,
        aldersjustering: Aldersjustering,
    ): Aldersjustering? {
        val barn = aldersjustering.barn

        val stønadsid =
            Stønadsid(
                stønadstype,
                Personident(barn.kravhaver),
                Personident(barn.skyldner!!),
                Saksnummer(barn.saksnummer),
            )

        secureLogger.info { "Aldersjustering for barn ${barn.id} med stønadsid: $aldersjustering. skal slettes. Sletter.." }
        val unikReferanse = "aldersjustering_${aldersjustering.batchId}_${stønadsid.toReferanse()}"

        vedtakConsumer.hentVedtaksforslagBasertPåReferanase(unikReferanse)?.let {
            secureLogger.info {
                "Fant eksisterende vedtaksforslag med referanse $unikReferanse og id ${it.vedtaksid}. Sletter eksisterende vedtaksforslag "
            }
            vedtakConsumer.slettVedtaksforslag(it.vedtaksid.toInt())
        } ?: run {
            log.error { "Fant ikke eksisterende vedtaksforslag med referanse $unikReferanse" }
            aldersjustering.vedtak = null
            aldersjustering.status = Status.SLETTET
            return null
        }
        aldersjustering.vedtak = null
        aldersjustering.status = Status.SLETTET
        return aldersjustering
    }

    private fun opprettEllerOppdaterVedtaksforslag(request: OpprettVedtakRequestDto) =
        try {
            slettEksisterendeVedtaksforslag(request.unikReferanse!!)
            vedtakConsumer.opprettVedtaksforslag(request)
        } catch (e: HttpStatusCodeException) {
            if (e.statusCode == HttpStatus.CONFLICT) {
                secureLogger.info { "Vedtaksforslag med referanse ${request.unikReferanse} finnes allerede. Oppdaterer vedtaksforslaget" }
                val resultat = e.getResponseBodyAs(OpprettVedtakConflictResponse::class.java)!!
                vedtakConsumer.oppdaterVedtaksforslag(resultat.vedtaksid, request)
            } else {
                secureLogger.error(e) { "Feil ved oppretting av vedtaksforslag med referanse ${request.unikReferanse}" }
                throw e
            }
        }

    private fun slettEksisterendeVedtaksforslag(referanse: String) {
        vedtakConsumer.hentVedtaksforslagBasertPåReferanase(referanse)?.let {
            secureLogger.info {
                "Fant eksisterende vedtaksforslag med referanse $referanse og id ${it.vedtaksid}. Sletter eksisterende vedtaksforslag "
            }
            vedtakConsumer.slettVedtaksforslag(it.vedtaksid.toInt())
        }
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

        log.info { "Antall barn ${result.getLengths()}" }
        return result
    }

    fun hentAntallBarnSomSkalAldersjusteresForÅr(år: Int): Int =
        barnRepository
            .finnBarnSomSkalAldersjusteresForÅr(år)
            .filter { it.fødselsdato != null }
            .groupBy { år - it.fødselsdato!!.year }
            .size

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
            val vedtaksforslagRequest = vedtakMapper.tilOpprettVedtakRequest(resultatBeregning, stønadsid, batchId)
            if (simuler) {
                log.info { "Kjører aldersjustering i simuleringsmodus. Oppretter ikke vedtaksforslag" }
                return AldersjusteringAldersjustertResultat(-1, stønadsid, vedtaksforslagRequest)
            }

            val vedtaksid = opprettEllerOppdaterVedtaksforslag(vedtaksforslagRequest)
            return AldersjusteringAldersjustertResultat(vedtaksid, stønadsid, vedtaksforslagRequest)
        } catch (e: SkalIkkeAldersjusteresException) {
            combinedLogger.warn(e) {
                "Stønad $stønadsid skal ikke aldersjusteres med begrunnelse ${
                    e.begrunnelser.joinToString(
                        ", ",
                    )
                }"
            }
            return AldersjusteringIkkeAldersjustertResultat(stønadsid, e.begrunnelser.joinToString(", "))
        } catch (e: AldersjusteresManueltException) {
            combinedLogger.warn(e) { "Stønad $stønadsid skal aldersjusteres manuelt med begrunnelse ${e.begrunnelse}" }
            return AldersjusteringIkkeAldersjustertResultat(stønadsid, e.begrunnelse.name)
        } catch (e: Exception) {
            combinedLogger.error(e) { "Det skjedde en feil ved aldersjustering for stønad $stønadsid" }
            return AldersjusteringIkkeAldersjustertResultat(stønadsid, "Teknisk feil: ${e.message}")
        }
    }
}

fun Map<Int, List<Barn>>.getLengths(): Map<Int, Int> = this.mapValues { it.value.size }
