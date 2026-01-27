package no.nav.bidrag.automatiskjobb.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.consumer.BidragBeløpshistorikkConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragPersonConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.mapper.GrunnlagMapper
import no.nav.bidrag.automatiskjobb.mapper.erBidrag
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Behandlingstype
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.RevurderForskuddRepository
import no.nav.bidrag.automatiskjobb.service.model.AdresseEndretResultat
import no.nav.bidrag.automatiskjobb.service.model.ForskuddRedusertResultat
import no.nav.bidrag.automatiskjobb.service.model.StønadEngangsbeløpId
import no.nav.bidrag.automatiskjobb.utils.enesteResultatkode
import no.nav.bidrag.automatiskjobb.utils.erDirekteAvslag
import no.nav.bidrag.automatiskjobb.utils.erHusstandsmedlem
import no.nav.bidrag.automatiskjobb.utils.tilResultatkode
import no.nav.bidrag.beregn.barnebidrag.service.external.SisteManuelleVedtak
import no.nav.bidrag.beregn.forskudd.BeregnForskuddApi
import no.nav.bidrag.beregn.vedtak.Vedtaksfiltrering
import no.nav.bidrag.domene.enums.beregning.Resultatkode.Companion.erDirekteAvslag
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.felles.personidentNav
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentStønadHistoriskRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadDto
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadPeriodeDto
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.beregning.forskudd.ResultatPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.personIdent
import no.nav.bidrag.transport.behandling.felles.grunnlag.søknadsbarn
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import no.nav.bidrag.transport.behandling.vedtak.request.HentVedtakForStønadRequest
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakDto
import no.nav.bidrag.transport.behandling.vedtak.response.erDelvedtak
import no.nav.bidrag.transport.behandling.vedtak.response.erOrkestrertVedtak
import no.nav.bidrag.transport.behandling.vedtak.response.referertVedtaksid
import no.nav.bidrag.transport.behandling.vedtak.saksnummer
import no.nav.bidrag.transport.felles.ifTrue
import org.springframework.context.annotation.Import
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

private fun VedtakDto.erIndeksreguleringEllerAldersjustering() =
    listOf(Vedtakstype.ALDERSJUSTERING, Vedtakstype.INDEKSREGULERING).contains(type)

private val LOGGER = KotlinLogging.logger { }

@Service
@Import(BeregnForskuddApi::class, Vedtaksfiltrering::class)
class RevurderForskuddService(
    private val `bidragBeløpshistorikkConsumer`: BidragBeløpshistorikkConsumer,
    private val bidragVedtakConsumer: BidragVedtakConsumer,
    private val bidragSakConsumer: BidragSakConsumer,
    private val bidragPersonConsumer: BidragPersonConsumer,
    private val beregning: BeregnForskuddApi,
    private val vedtaksFilter: Vedtaksfiltrering,
    private val revurderForskuddRepository: RevurderForskuddRepository,
    private val reskontroService: ReskontroService,
) {
    fun skalBMFortsattMottaForskuddForSøknadsbarnEtterAdresseendring(aktørId: String): List<AdresseEndretResultat> {
        val person = bidragPersonConsumer.hentPerson(Personident(aktørId))
        val barnIdent = person.ident
        val saker = bidragSakConsumer.hentSakerForPerson(barnIdent)
        return saker.mapNotNull { sak ->
            val personRolle = sak.roller.find { it.fødselsnummer == barnIdent } ?: return@mapNotNull null
            LOGGER.info {
                "Sjekker om person ${barnIdent.verdi} er barn i saken ${sak.saksnummer}, mottar forskudd og fortsatt bor hos BM etter adresseendring"
            }
            if (personRolle.type != Rolletype.BARN) {
                LOGGER.info {
                    "Person ${barnIdent.verdi} har rolle ${personRolle.type} i sak ${sak.saksnummer}. Behandler bare når barnets adresse endres. Avslutter behandling"
                }
                return@mapNotNull null
            }
            val bidragsmottaker =
                sak.roller.find { it.type == Rolletype.BIDRAGSMOTTAKER } ?: run {
                    LOGGER.info {
                        "Sak ${sak.saksnummer} har ingen bidragsmottaker. Avslutter behandling"
                    }
                    return@mapNotNull null
                }

            LOGGER.info {
                "Sjekker om barnet ${barnIdent.verdi} i sak ${sak.saksnummer} mottar forskudd og fortsatt bor hos BM etter adresseendring"
            }
            val løpendeForskudd =
                hentLøpendeForskudd(sak.saksnummer.verdi, barnIdent.verdi) ?: run {
                    LOGGER.info {
                        "Fant ingen løpende forskudd i sak ${sak.saksnummer} for barn ${barnIdent.verdi}. Avslutter behandling"
                    }
                    return@mapNotNull null
                }

            val husstandsmedlemmerBM =
                bidragPersonConsumer.hentPersonHusstandsmedlemmer(bidragsmottaker.fødselsnummer!!)
            if (husstandsmedlemmerBM.erHusstandsmedlem(barnIdent)) {
                LOGGER.info {
                    "Barn ${barnIdent.verdi} er husstandsmedlem til bidragsmottaker ${bidragsmottaker.fødselsnummer!!.verdi}. Ingen endringer kreves."
                }
                return@mapNotNull null
            }

            LOGGER.info {
                "Bidragsmottaker ${bidragsmottaker.fødselsnummer?.verdi} mottar forskudd for barn ${barnIdent.verdi} " +
                    "i sak ${sak.saksnummer} med beløp ${løpendeForskudd.beløp} ${løpendeForskudd.valutakode}. " +
                    "Barnet bor ikke lenger hos bidragsmottaker og skal derfor ikke motta forskudd lenger"
            }
            AdresseEndretResultat(
                saksnummer = sak.saksnummer.verdi,
                bidragsmottaker = bidragsmottaker.fødselsnummer!!.verdi,
                gjelderBarn = barnIdent.verdi,
                enhet = sak.eierfogd.verdi,
            )
        }
    }

    fun erForskuddRedusert(vedtakHendelse: VedtakHendelse): List<ForskuddRedusertResultat> {
        LOGGER.info {
            "Sjekker om forskuddet er redusert etter fattet vedtak ${vedtakHendelse.id} i sak ${vedtakHendelse.saksnummer}"
        }
        val vedtak = hentVedtak(vedtakHendelse.id) ?: return listOf()
        return erForskuddRedusertEtterFattetBidrag(
            SisteManuelleVedtak(
                vedtakHendelse.id,
                vedtak,
            ),
        ) + erForskuddRedusertEtterSærbidrag(SisteManuelleVedtak(vedtakHendelse.id, vedtak))
    }

    @Transactional
    fun resetEvalueringEtterSimuering() {
        val simulerteRevurderForskudd =
            revurderForskuddRepository.findAllByStatusIs(Status.SIMULERT, Pageable.unpaged())
        simulerteRevurderForskudd.forEach {
            it.status = Status.UBEHANDLET
            it.behandlingstype = null
            it.begrunnelse = emptyList()
        }
    }

    @Transactional
    fun slettRevurderingForskuddForMåned(forMåned: YearMonth) {
        val revurderingerForMåned =
            revurderForskuddRepository.findAllByForMåned(forMåned.toString(), Pageable.unpaged())

        revurderingerForMåned.forEach {
            revurderForskuddRepository.delete(it)
        }
        LOGGER.info {
            "Slettet ${revurderingerForMåned.totalElements} revurderinger av forskudd for måned $forMåned"
        }
    }

    private fun erForskuddRedusertEtterSærbidrag(vedtakInfo: SisteManuelleVedtak): List<ForskuddRedusertResultat> {
        val (vedtaksid, vedtak) = vedtakInfo
        return vedtak.engangsbeløpListe
            .filter { it.type == Engangsbeløptype.SÆRBIDRAG }
            .filter {
                if (it.resultatkode.tilResultatkode()?.erDirekteAvslag() == true) {
                    LOGGER.info {
                        "Særbidrag vedtaket $vedtaksid er direkte avslag med resultat ${it.resultatkode} og har derfor ingen inntekter."
                    }
                    false
                } else {
                    true
                }
            }.groupBy { it.sak }
            .flatMap { (sak, stønader) ->
                val stønad = stønader.first()
                val sak = bidragSakConsumer.hentSak(sak.verdi)
                sak.roller.filter { it.type == Rolletype.BARN }.mapNotNull { rolle ->
                    val stønadsid =
                        StønadEngangsbeløpId(
                            rolle.fødselsnummer!!,
                            stønad.skyldner,
                            stønad.sak,
                            engangsbeløptype = stønad.type,
                        )
                    erForskuddetRedusert(vedtakInfo, stønadsid, stønad.mottaker)
                }
            }
    }

    private fun erForskuddRedusertEtterFattetBidrag(vedtakInfo: SisteManuelleVedtak): List<ForskuddRedusertResultat> =
        vedtakInfo.vedtak.stønadsendringListe
            .filter { it.erBidrag }
            .filter {
                if (it.erDirekteAvslag()) {
                    LOGGER.info {
                        "Bidrag vedtaket ${vedtakInfo.vedtaksId} med type ${it.type} for kravhaver ${it.kravhaver} er direkte avslag med resultat ${it.enesteResultatkode()} og har derfor ingen inntekter."
                    }
                    false
                } else {
                    true
                }
            }.groupBy { it.sak }
            .flatMap { (sak, stønader) ->
                val stønad = stønader.first()
                val sak = bidragSakConsumer.hentSak(sak.verdi)
                val bidragsmottaker = sak.roller.find { it.type == Rolletype.BIDRAGSMOTTAKER }!!
                sak.roller.filter { it.type == Rolletype.BARN }.mapNotNull { rolle ->
                    val stønadsid =
                        StønadEngangsbeløpId(
                            rolle.fødselsnummer!!,
                            stønad.skyldner,
                            stønad.sak,
                            stønadstype = stønad.type,
                        )
                    erForskuddetRedusert(vedtakInfo, stønadsid, bidragsmottaker.fødselsnummer!!)
                }
            }

    private fun erForskuddetRedusert(
        vedtakFattet: SisteManuelleVedtak,
        stønadEngangsbeløpId: StønadEngangsbeløpId,
        mottaker: Personident,
    ): ForskuddRedusertResultat? {
        val gjelderBarn = stønadEngangsbeløpId.kravhaver
        val sistePeriode = hentLøpendeForskudd(stønadEngangsbeløpId.sak.verdi, gjelderBarn.verdi) ?: return null

        val vedtakForskudd =
            hentSisteManuelleForskuddVedtak(sistePeriode.vedtaksid, stønadEngangsbeløpId.sak, gjelderBarn)
                ?: return null
        val (beregnetForskudd, grunnlagsliste) = beregnForskudd(vedtakFattet.vedtak, vedtakForskudd.vedtak, gjelderBarn)

        val beregnetResultat = beregnetForskudd.resultat
        val beløpLøpende = sistePeriode.beløp!!
        val erForskuddRedusert = beløpLøpende > beregnetResultat.belop
        val sisteInntektForskudd =
            GrunnlagMapper.hentSisteDelberegningInntektFraForskudd(
                vedtakForskudd.vedtak,
                stønadEngangsbeløpId.kravhaver,
            )
        val sisteInntektFattetVedtak =
            GrunnlagMapper.hentSisteInntektFraBeregning(
                beregnetForskudd.grunnlagsreferanseListe,
                grunnlagsliste,
            )
        return erForskuddRedusert.ifTrue { _ ->
            LOGGER.info {
                """Forskudd er redusert i sak ${stønadEngangsbeløpId.sak.verdi} for bidragsmottaker ${mottaker.verdi} og barn ${gjelderBarn.verdi}. 
                   Løpende forskudd er $beløpLøpende og forskudd ble beregnet til ${beregnetResultat.belop} basert på siste vurdert inntekt fra bidrag vedtak ${vedtakFattet.vedtaksId} og grunnlag fra forskudd vedtak ${vedtakForskudd.vedtaksId}.
                   Siste løpende inntekt for BM i fattet vedtak er ${sisteInntektFattetVedtak?.totalinntekt} og siste inntekt for BM i forskudd vedtaket er ${sisteInntektForskudd?.totalinntekt}.
                   
                   Innteksdetaljer fra fattet vedtak $sisteInntektFattetVedtak 
                   og fra forskudd vedtaket $sisteInntektForskudd
                """.trimMargin()
                    .trimIndent()
                    .replace("\t", "")
                    .replace("  ", "")
            }
            ForskuddRedusertResultat(
                saksnummer = stønadEngangsbeløpId.sak.verdi,
                bidragsmottaker = mottaker.verdi,
                gjelderBarn = gjelderBarn.verdi,
                stønadstype = stønadEngangsbeløpId.stønadstype,
                engangsbeløptype = stønadEngangsbeløpId.engangsbeløptype,
            )
        } ?: run {
            LOGGER.info {
                """Forskudd er IKKE redusert i sak ${stønadEngangsbeløpId.sak.verdi} for bidragsmottaker ${mottaker.verdi} og barn ${gjelderBarn.verdi}. 
                   Løpende forskudd er $beløpLøpende og forskudd ble beregnet til ${beregnetResultat.belop} basert på siste vurdert inntekt fra bidrag vedtak ${vedtakFattet.vedtaksId} og grunnlag fra forskudd vedtak ${vedtakForskudd.vedtaksId}.
                   Siste løpende inntekt for BM i fattet vedtak er $sisteInntektFattetVedtak og siste inntekt for BM i forskudd vedtaket er $sisteInntektForskudd.
                   
                   Innteksdetaljer fra fattet vedtak $sisteInntektFattetVedtak 
                   og fra forskudd vedtaket $sisteInntektForskudd
                """.trimMargin()
                    .trimIndent()
                    .trimIndent()
                    .replace("\t", "")
                    .replace("  ", "")
            }
            null
        }
    }

    private fun hentVedtak(vedtakId: Int): VedtakDto? {
        val vedtak = bidragVedtakConsumer.hentVedtak(vedtakId) ?: return null
        if (vedtak.erDelvedtak || (vedtak.erOrkestrertVedtak && vedtak.type == Vedtakstype.INNKREVING)) return null
        val faktiskVedtak =
            if (vedtak.erOrkestrertVedtak) {
                bidragVedtakConsumer.hentVedtak(vedtak.referertVedtaksid!!)
            } else {
                vedtak
            } ?: return null
        if (faktiskVedtak.grunnlagListe.isEmpty()) {
            LOGGER.info {
                "Vedtak $vedtakId fattet av system ${vedtak.kildeapplikasjon} mangler grunnlag. Gjør ingen vurdering"
            }
            return null
        }
        return faktiskVedtak
    }

    private fun hentSisteManuelleForskuddVedtak(
        vedtakId: Int,
        saksnummer: Saksnummer,
        gjelderBarn: Personident,
    ): SisteManuelleVedtak? {
        val vedtak =
            bidragVedtakConsumer.hentVedtak(vedtakId)?.let {
                if (it.erIndeksreguleringEllerAldersjustering()) {
                    val forskuddVedtakISak =
                        bidragVedtakConsumer.hentVedtakForStønad(
                            HentVedtakForStønadRequest(
                                saksnummer,
                                Stønadstype.FORSKUDD,
                                personidentNav,
                                gjelderBarn,
                            ),
                        )
                    val sisteManuelleVedtak =
                        vedtaksFilter.finneSisteManuelleVedtak(
                            forskuddVedtakISak.vedtakListe,
                        ) ?: return null
                    bidragVedtakConsumer.hentVedtak(sisteManuelleVedtak.vedtaksid)?.let {
                        SisteManuelleVedtak(sisteManuelleVedtak.vedtaksid, it)
                    }
                } else {
                    SisteManuelleVedtak(vedtakId, it)
                }
            } ?: return null

        if (vedtak.vedtak.grunnlagListe.isEmpty()) {
            LOGGER.info {
                "Forskudd vedtak $vedtakId fattet av system ${vedtak.vedtak.kildeapplikasjon} mangler grunnlag. Gjør ingen vurdering"
            }
            return null
        }
        LOGGER.info { "Fant siste manuelle forskudd vedtak ${vedtak.vedtaksId}" }
        return vedtak
    }

    private fun beregnForskudd(
        vedtakBidrag: VedtakDto,
        vedtakLøpendeForskudd: VedtakDto,
        gjelderBarn: Personident,
    ): Pair<ResultatPeriode, List<GrunnlagDto>> {
        val grunnlag = GrunnlagMapper.byggGrunnlagForBeregning(vedtakBidrag, vedtakLøpendeForskudd, gjelderBarn)
        val resultat =
            beregning.beregn(
                BeregnGrunnlag(
                    periode =
                        ÅrMånedsperiode(
                            YearMonth.now(),
                            YearMonth.now().plusMonths(1),
                        ),
                    stønadstype = Stønadstype.FORSKUDD,
                    søknadsbarnReferanse = grunnlag.søknadsbarn.find { it.personIdent == gjelderBarn.verdi }!!.referanse,
                    grunnlagListe = grunnlag,
                ),
            )

        val sistePeriode = resultat.beregnetForskuddPeriodeListe.last()
        return sistePeriode to resultat.grunnlagListe
    }

    private fun hentLøpendeForskudd(
        saksnummer: String,
        gjelderBarn: String,
    ): StønadPeriodeDto? {
        val forskuddStønad =
            hentForskuddForSak(saksnummer, gjelderBarn) ?: run {
                LOGGER.info { "Fant ingen løpende forskudd i sak $saksnummer for barn $gjelderBarn" }
                return null
            }
        return forskuddStønad.periodeListe.hentSisteLøpendePeriode() ?: run {
            LOGGER.info {
                "Forskudd i sak $saksnummer og barn $gjelderBarn har opphørt før dagens dato. Det finnes ingen løpende forskudd"
            }
            null
        }
    }

    private fun hentForskuddForSak(
        saksnummer: String,
        søknadsbarnIdent: String,
    ): StønadDto? =
        bidragBeløpshistorikkConsumer.hentHistoriskeStønader(
            HentStønadHistoriskRequest(
                type = Stønadstype.FORSKUDD,
                sak = Saksnummer(saksnummer),
                skyldner = personidentNav,
                kravhaver = Personident(søknadsbarnIdent),
                gyldigTidspunkt = LocalDateTime.now(),
            ),
        )

    private fun List<StønadPeriodeDto>.hentSisteLøpendePeriode() =
        maxByOrNull { it.periode.fom }?.takeIf { it.periode.til == null || it.periode.til!!.isAfter(YearMonth.now()) }

    /**
     * Oppdaterer vurdereTilbakekrevingsfeltet basert på om det finnes forskudd i reskontro for en eller flere av tre siste månedene.
     */
    @Transactional
    fun vurderTilbakekrevingBasertPåReskontro() {
        val fattedeForslag = revurderForskuddRepository.findAllByBehandlingstypeIs(Behandlingstype.FATTET_FORSLAG)
        fattedeForslag.forEach {
            val finnesForskuddForSakPeriode = reskontroService.finnesForskuddForSakPeriode(
                Saksnummer(it.barn.saksnummer),
                listOf(
                    LocalDate.now().minusMonths(3),
                    LocalDate.now().minusMonths(2),
                    LocalDate.now().minusMonths(1),
                ),
            )
            LOGGER.info { "Sak ${it.barn.saksnummer} skal vurdere tilbakekreving: $finnesForskuddForSakPeriode" }
            it.vurdereTilbakekreving = finnesForskuddForSakPeriode
        }
        fattedeForslag.forEach { revurderForskuddRepository.save(it) }
    }
}
