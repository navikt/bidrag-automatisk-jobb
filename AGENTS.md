# AGENTS.md – bidrag-automatisk-jobb

Spring Boot-applikasjon (Kotlin, Java 21) som kjører automatiserte batchjobber i bidragsdomene. To primære funksjoner:

1. **Aldersjustering** – aldersjusterer barnebidrag når et barn fyller 6, 11 eller 15 år.
2. **Revurdering av forskudd** – revurderer forskuddsutbetalinger for saker uten manuelle endringer de siste N månedene.

## Arkitekturoversikt

```
Kafka-lyttere (kafka/)
    └─► Tjenestelag (service/)
            └─► Konsumenter (consumer/)   – REST-kall til andre bidrag-mikrotjenester
            └─► Persistens (persistence/) – JPA + Flyway (PostgreSQL)

HTTP-kontrollere (controller/)
    └─► Spring Batch-jobber (batch/)
            └─► Tjenestelag (service/batch/)
```

- **Batchjobber startes via HTTP, ikke automatisk ved oppstart.** `spring.batch.job.enabled: false` er satt globalt. Kontrollere under `controller/` kaller `batch/*Batch.kt`-startere som returnerer umiddelbart (asynkron `JobLauncher`). Se `BatchConfiguration.kt`.
- **ShedLock** hindrer samtidig kjøring av samme jobb på tvers av instanser.
- **Chunk-/grid-størrelser**: `CHUNK_SIZE = 100`, `PAGE_SIZE = 100`, `GRID_SIZE = 10` tråder (se `BatchConfiguration`).

## Eksterne tjenestekonsumenter

Alle konsumenter arver fra `AbstractRestClient` og injiserer `@Qualifier("azure") RestOperations`. Hver nedstrøms tjeneste har sin egen OAuth2-klientregistrering i `application.yaml`:

| Konsument | Miljøvariabelprefix |
|---|---|
| `BidragBehandlingConsumer` | `BIDRAG_BEHANDLING_URL/SCOPE` |
| `BidragVedtakConsumer` | `BIDRAG_VEDTAK_URL/SCOPE` |
| `BidragSakConsumer` | `BIDRAG_SAK_URL/SCOPE` |
| `BidragPersonConsumer` | `PERSON_URL/SCOPE` |
| `BidragGrunnlagConsumer` | `BIDRAG_GRUNNLAG_URL/SCOPE` |
| `BidragBeløpshistorikkConsumer` | `BIDRAG_BELOPSHISTORIKK_URL/SCOPE` |
| `OppgaveConsumer` | `OPPGAVE_URL/SCOPE` |

Bruk `@Retryable(maxAttempts = 3, backoff = Backoff(...))` på konsumentmetoder som må tåle forbigående feil.

## Kafka

- **`KAFKA_VEDTAK_TOPIC`**: konsumeres av to grupper – `bidrag-automatisk-jobb-start` (offset=earliest, oppdaterer `Barn`-tabellen) og `bidrag-automatisk-jobb-siste` (offset=latest, trigger oppgaveoppretting).
- **`KAFKA_PERSON_HENDELSE_TOPIC`**: behandler identendringer og adresseflyttinger.
- Feilhåndtering: eksponentiell tilbakebakking opp til 5 min, dead-letter-logging i `KafkaConfiguration.kt`.

## Testmønstre

- **Enhetstester**: `@ExtendWith(MockKExtension::class)` + `@InjectMockKs` / `@MockK` / `@RelaxedMockK` (MockK-biblioteket).
- **Assertions**: Kotest (`io.kotest.matchers.shouldBe`, `shouldNotBe`, osv.).
- **Testdatagenerering**: bruk hjelpere fra `bidrag-generer-testdata-felles`, f.eks. `genererFødselsnummer()`, `genererSaksnummer()`.
- **WireMock** for HTTP-stubber; **Testcontainers** (PostgreSQL) for repositorytester.
- Delt stubb-hjelper: `StubUtils.stubSaksbehandlernavnProvider()` for å mocke `EnhetProvider`.

## Bygg og linting

```bash
mvn verify          # kompilér + test + ktlint-sjekk
mvn validate        # ktlint automatisk formatering (kjøres før kompilering)
mvn test            # kun tester (hopper over ktlint)
```

ktlint kjøres automatisk via `maven-antrun-plugin`; autoformaterer ved `validate`, feiler bygget ved `verify`.
Kan kjøres direkte med `ktlint --format` og bør gjøres etter hver endring.

## Kjøre lokalt

Start `BidragAldersjusteringLocal.kt` (i `src/test/kotlin/`). Aktiverer profilene `local,nais,lokal-nais-secrets,lokal-nais` og leser konfig fra `src/test/resources/application-local.yaml`.

Påkrevde miljøvariabler (ikke committet):
```bash
AZURE_APP_CLIENT_ID=<id>
AZURE_APP_CLIENT_SECRET=<secret>
# Hent fra Q1:
kubectl exec --tty deployment/bidrag-automatisk-jobb-q1 -- printenv | grep -e AZURE_APP_CLIENT_ID -e AZURE_APP_CLIENT_SECRET
```

## Viktige konvensjoner

- Logging: `private val LOGGER = KotlinLogging.logger {}` (`io.github.oshai.kotlinlogging`).
- JSON: bruk `commonObjectmapper` fra `no.nav.bidrag.transport.felles` (konfigurert for `YearMonth`, `LocalDate` år > 9999, `NON_NULL`).
- Alle kontrollere annotert med `@Protected` (token-validation-spring).
- Funksjonsflagg via Unleash (`@EnableUnleashFeatures`); toggle-sjekker ligger i tjeneste-/batch-logikk.
- Databaseskjemaendringer legges i `src/main/resources/db/migration/` som `V{major}.{minor}.{patch}__beskrivelse.sql`.
- `Barn`-entiteten er den sentrale tverrgående posten som kobler et barn (kravhaver) til en sak (saksnummer) med aktive stønadperioder.
