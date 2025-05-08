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
import no.nav.bidrag.automatiskjobb.service.model.AldersjusteringAldersjustertResultat
import no.nav.bidrag.automatiskjobb.service.model.AldersjusteringIkkeAldersjustertResultat
import no.nav.bidrag.automatiskjobb.service.model.AldersjusteringResponse
import no.nav.bidrag.automatiskjobb.service.model.AldersjusteringResultat
import no.nav.bidrag.automatiskjobb.service.model.AldersjusteringResultatResponse
import no.nav.bidrag.automatiskjobb.service.model.OpprettVedtakConflictResponse
import no.nav.bidrag.automatiskjobb.utils.ugyldigForespørsel
import no.nav.bidrag.beregn.barnebidrag.service.AldersjusteresManueltException
import no.nav.bidrag.beregn.barnebidrag.service.AldersjusteringOrchestrator
import no.nav.bidrag.beregn.barnebidrag.service.SkalIkkeAldersjusteresException
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.sak.Stønadsid
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettVedtakRequestDto
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
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
) {
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
            )
        }

        val aldersjusteringListe =
            alderjusteringRepository
                .finnForFlereStatuserOgBarnId(
                    listOf(Status.SLETTET, Status.UBEHANDLET, Status.FEILET, Status.SIMULERT),
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
    ) {
        val aldersgruppe = barn.fødselsdato?.year?.let { år - it }

        if (aldersgruppe == null) {
            log.error {
                "Aldersjustering for barn ${barn.id} kan ikke opprettes. Barnet har ingen fødselsdato og aldersgruppe kan derfor ikke settes."
            }
            return
        }

        if (!alderjusteringRepository.existsAldersjusteringsByBarnIdAndAldersgruppe(barn.id!!, aldersgruppe)) {
            val aldersjustering =
                Aldersjustering(
                    batchId = batchId,
                    barnId = barn.id!!,
                    aldersgruppe = aldersgruppe,
                    status = Status.UBEHANDLET,
                )
            val id = alderjusteringRepository.save(aldersjustering).id
            log.info { "Opprettet aldersjustering $id for barn ${barn.id}." }
        } else {
            log.info { "Aldersjustering for barn ${barn.id} er allerede opprettet." }
        }
    }

    fun utførAldersjustering(
        aldersjustering: Aldersjustering,
        stønadstype: Stønadstype,
        simuler: Boolean,
    ): AldersjusteringResultat {
        val barn =
            barnRepository
                .findById(aldersjustering.barnId)
                .orElseThrow { error("Fant ikke barn med id ${aldersjustering.barnId}") }
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
            val (vedtaksidBeregning, løpendeBeløp, resultatBeregning) =
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
            alderjusteringRepository.save(aldersjustering)

            return AldersjusteringAldersjustertResultat(vedtaksid ?: -1, stønadsid, vedtaksforslagRequest)
        } catch (e: SkalIkkeAldersjusteresException) {
            aldersjustering.vedtaksidBeregning = e.vedtaksid
            aldersjustering.status = if (simuler) Status.SIMULERT else Status.BEHANDLET
            aldersjustering.behandlingstype = Behandlingstype.INGEN
            aldersjustering.begrunnelse = e.begrunnelser.map { it.name }

            alderjusteringRepository.save(aldersjustering)

            combinedLogger.warn(e) {
                "Stønad $stønadsid skal ikke aldersjusteres med begrunnelse ${e.begrunnelser.joinToString(", ")}"
            }
            return AldersjusteringIkkeAldersjustertResultat(stønadsid, e.begrunnelser.joinToString(", "))
        } catch (e: AldersjusteresManueltException) {
            aldersjustering.vedtaksidBeregning = e.vedtaksid
            aldersjustering.status = if (simuler) Status.SIMULERT else Status.BEHANDLET
            aldersjustering.behandlingstype = Behandlingstype.MANUELL
            aldersjustering.begrunnelse = listOf(e.begrunnelse.name)

            alderjusteringRepository.save(aldersjustering)

            combinedLogger.warn(e) { "Stønad $stønadsid skal aldersjusteres manuelt med begrunnelse ${e.begrunnelse}" }
            return AldersjusteringIkkeAldersjustertResultat(stønadsid, e.begrunnelse.name)
        } catch (e: Exception) {
            aldersjustering.status = Status.FEILET
            aldersjustering.behandlingstype = Behandlingstype.FEILET
            aldersjustering.begrunnelse = listOf(e.message ?: "Ukjent feil")

            alderjusteringRepository.save(aldersjustering)

            combinedLogger.error(e) { "Det skjedde en feil ved aldersjustering for stønad $stønadsid" }
            return AldersjusteringIkkeAldersjustertResultat(stønadsid, "Teknisk feil: ${e.message}")
        }
    }

    fun fattVedtakOmAldersjustering(aldersjustering: Aldersjustering) {
        combinedLogger.info { "Fatter vedtak for aldersjustering ${aldersjustering.id} og vedtaksid ${aldersjustering.vedtak}" }
        vedtakConsumer.fatteVedtaksforslag(aldersjustering.vedtak ?: error("Aldersjustering ${aldersjustering.id} mangler vedtak!"))
        aldersjustering.status = Status.FATTET
        alderjusteringRepository.save(aldersjustering)
    }

    fun opprettOppgaveForAldersjustering(aldersjustering: Aldersjustering) {
//         oppgaveService.opprettRevurderForskuddOppgave()  TODO()
        val oppdageId = 1
        aldersjustering.oppgave = oppdageId
        alderjusteringRepository.save(aldersjustering)
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

    fun slettVedtaksforslag(
        stønadstype: Stønadstype,
        aldersjustering: Aldersjustering,
    ) {
        val barn =
            barnRepository
                .findById(aldersjustering.barnId)
                .orElseThrow { error("Fant ikke barn med id ${aldersjustering.barnId}") }

        val stønadsid =
            Stønadsid(
                stønadstype,
                Personident(barn.kravhaver),
                Personident(barn.skyldner!!),
                Saksnummer(barn.saksnummer),
            )

        log.info { "Aldersjustering for barn ${barn.id} med stønadsid: $aldersjustering. skal slettes. Sletter.." }
        aldersjustering.vedtak = null
        aldersjustering.status = Status.SLETTET
        val unikReferanse = "aldersjustering_${aldersjustering.batchId}_${stønadsid.toReferanse()}"
        slettEksisterendeVedtaksforslag(unikReferanse)
    }

    private fun opprettEllerOppdaterVedtaksforslag(request: OpprettVedtakRequestDto) =
        try {
            slettEksisterendeVedtaksforslag(request.unikReferanse!!)
            vedtakConsumer.opprettVedtaksforslag(request)
        } catch (e: HttpStatusCodeException) {
            if (e.statusCode == HttpStatus.CONFLICT) {
                log.info { "Vedtaksforslag med referanse ${request.unikReferanse} finnes allerede. Oppdaterer vedtaksforslaget" }
                val resultat = e.getResponseBodyAs(OpprettVedtakConflictResponse::class.java)!!
                vedtakConsumer.oppdaterVedtaksforslag(resultat.vedtaksid, request)
            } else {
                log.error(e) { "Feil ved oppretting av vedtaksforslag med referanse ${request.unikReferanse}" }
                throw e
            }
        }

    private fun slettEksisterendeVedtaksforslag(referanse: String) {
        vedtakConsumer.hentVedtaksforslagBasertPåReferanase(referanse)?.let {
            log.info {
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
        val bp = sak.roller.find { it.type == Rolletype.BIDRAGSPLIKTIG } ?: ugyldigForespørsel("Fant ikke BP for sak $saksnummer")
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
            combinedLogger.warn(e) { "Stønad $stønadsid skal ikke aldersjusteres med begrunnelse ${e.begrunnelser.joinToString(", ")}" }
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
