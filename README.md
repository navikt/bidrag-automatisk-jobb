# Bidrag-automatisk-jobb
Applikasjon for å kjøre jobber, primært batcher, for å utføre endringer i bidragssaker.

[![continuous integration](https://github.com/navikt/bidrag-automatisk-jobb/actions/workflows/deploy_q1.yaml/badge.svg)](https://github.com/navikt/bidrag-automatisk-jobb/actions/workflows/deploy_q1.yaml)
[![release bidrag-automatisk-jobb](https://github.com/navikt/bidrag-automatisk-jobb/actions/workflows/deploy_prod.yaml/badge.svg)](https://github.com/navikt/bidrag-automatisk-jobb/actions/workflows/deploy_prod.yaml)

## Beskrivelse

### Alderjustering
Bidrag-automatisk-jobb lagrer alle barn som er part i en bidragssak, og aldersjusterer dem.
Aldersjustering skjer for alle barn det året de fyller 6, 11 og 15 år, basert på sjablongverdier.

### Revurdering forskudd
Bidrag-automatisk-jobb henter ut alle løpende forskudd det ikke har blitt gjort manuelle endringer på i løpet av x 
siste måneder og utfører en beregning for å sjekke om forskudded skal settes ned. 

## Kjøre applikasjonen lokalt

Start opp applikasjonen ved å kjøre [BidragAldersjusteringLocal.kt](src/test/kotlin/no/nav/bidrag/automatiskjobb/BidragAldersjusteringLocal.kt).
Dette starter applikasjonen med profil `local` og henter miljøvariabler for Q1 miljøet fra filen [application-local.yaml](src/test/resources/application-local.yaml).

Her mangler det noen miljøvariabler som ikke bør committes til Git (Miljøvariabler for passord/secret osv).<br/>
Når du starter applikasjon må derfor følgende miljøvariabl(er) settes:
```bash
AZURE_APP_CLIENT_SECRET=<secret>
AZURE_APP_CLIENT_ID=<id>
```
Disse kan hentes ved å kjøre kan hentes ved å kjøre 
```bash
kubectl exec --tty deployment/bidrag-automatisk-jobb-q1 -- printenv | grep -e AZURE_APP_CLIENT_ID -e AZURE_APP_CLIENT_SECRET
```


## Kjøringsplan batcher

### Alderjustering

#### Del 1 - Klargjøring av aldersjustering-batchkjøring
Første fase er å hente ut et uttrekk over barn som potensielt skal aldersjusteres.
Uttrekket lagres i `aldersjustering`-tabellen i databasen. Dette gjøres ved å kjøre batchen `OpprettAldersjusteringerBidragBatch`.

`batch_id` i `aldersjustering`-tabellen lagres med formatet `aldersjustering_bidrag_<årstall aldersjustering kjøres for>`.
Hvis det er feil i uttrekket, kan alle rader med årets `batch_id` slettes. Deretter kan opprett-batchen kjøres på nytt.
**NB: Ikke slett rader fra eldre batchkjøringer.**

Denne kan trigges ved å kalle POST: `https://bidrag-automatisk-jobb-q1.intern.dev.nav.no/aldersjustering/batch/bidrag/opprett?aar=<årstall aldersjustering>`

Status på kjøringen kan sjekkes i `batch_job_execution`-tabellen i databasen.
Hvis status = 'COMPLETED', er kjøringen ferdig.

#### Grunnlagsoverføring
Før alle grunnlag fra vedtak fattet i BBM er overført til bidrag-vedtak, må grunnlagsoverføring kjøres for alle saker som skal aldersjusteres.
Dette er nødvendig for at beregningen skal ha tilstrekkelige grunnlagsdata til å utføre aldersjusteringsberegning.
Deretter må det hentes ut et uttrekk over saker som skal aldersjusteres. Sakene brukes i GB513-batchen for grunnlagsoverføring.

Utrekk over saker hvor det finnes barn som skal aldersjusteres
```sql
SELECT distinct b.saksnummer
FROM barn b
WHERE :år - EXTRACT(YEAR from b.fodselsdato) IN (6, 11, 15)
  AND b.bidrag_fra <= make_date(:år, 7, 1)
  AND (b.bidrag_til IS NULL OR b.bidrag_til >= make_date(:år, 7, 1));
```

Koble deg til bisys-batch-serveren
```bash
ssh <RA Nav ident>@a01wasl00178.adeo.no
```

Kopier over sakene fra uttrekket til filen `/tmp/grunnlagsoverforing/saker_aldersjustering_<årstall>.txt`

Start grunnlagsoverføringsbatchen med følgende kommando:

```bash
sudo start-batch -f GB513 saksnr="¤FILE:/tmp/grunnlagsoverforing/saker_aldersjustering_<årstall>.txt" modus=OVERFOR grid=10 overskrivOverfortGrunnlag=true
```

#### Del 2 - Beregning av aldersjustering
Denne kan trigges ved å kalle `POST /aldersjustering/batch/bidrag/beregn`

Med parametere:
- `simuler` - Simuler aldersjustering beregning uten å opprette vedtaksforslag. Dette kan brukes til å hente utrekk over antall barn som skal aldersjusteres og som må behandles manuelt.
- `inkluderBehandlet` - Rekjør beregning på aldersjusteringer som allerede er behandlet og har blitt opprettet vedtaksforslag. (status = `UBEHANDLET`)
- `barn` - liste over barnider som det ønskes å beregnes for. Hvis det ikke er satt så beregnes det for alle med status `UBEHANDLET`

Andre fase er å kjøre beregning av aldersjustering.
I denne kjøringen beregnes aldersjustering for alle barn som ble opprettet i fase 1.

Aldersjustering beregning kan føre til følgende utfall:

Status:
- `UBEHANDLET` - Aldersjustering er opprettet men ingen beregning er utført
- `SIMULERT` - Aldersjusteringen ble behandlet med suksess men det ble ikke opprettet en vedtaksforslag
- `BEHANDLET` - Aldersjusteringen ble behandlet med suksess og det ble opprettet en vedtaksforslag
- `FEILET` - Det skjedde en feil under beregning av aldersjusteringen. Feil begrunnelsen er lagret i `begrunnelse` kolonnen i aldersjustering tabellen

Behandlingstype:
- `FATTET_FORSLAG` - Det ble opprettet vedtaksforslag og kan åpnes via Sakshistorikken for saken
- `MANUELL` - Det ble opprettet vedtaksforslag som krever manuell behandling av aldersjusteringen
- `INGEN` - Det ble opprettet vedtaksforslag med beslutningstype AVVIST

Hent utrekk over saker med følgende kommando:

```sql
SELECT a.id, b.id, b.saksnummer, a.status, a.begrunnelse 
FROM aldersjustering a 
INNER JOIN barn b ON b.id = a.barn_id 
WHERE a.behandlingstype = 'FATTET_FORSLAG';
```

#### Del 3 - Fatte vedtak
I denne fasen fattes det vedtak for alle vedtaksforslagene som ble opprettet i Del 2.
Da endres vedtaksforslag til et vedtak.

Denne kan trigges ved å kalle `POST /aldersjustering/batch/bidrag/fattVedtak`

Med parametere:
- `barn` - liste over barnider som det ønskes å fattes vedtak for. Hvis det ikke er satt så fattes det vedtak for alle
- `behandlingstyper` - Liste over behandlingstyper det skal fattes vedtak for. Det kan brukes for å begynne med å fatte vedtak for de som aldersjusteres og ta de manuelle/ingen senere
- `simuler` - Fattes ingen vedtak men det opprettes en forsendelse for vedtak. Dette kan brukes til å teste forsendelse/brev før det fattes vedtak
- `kunRedusertBidrag` - Fatte kun vedtak for casene hvor aldersjusteringen fører til at bidraget reduseres. Dette kan brukes for å fatte vedtak for å unngå B4

NB!: Viktig at for de manuelle så må vedtak fattes etter 1. Juli. Det er fordi det opprettes en søknad med mottatt dato 1. Juli. og Bisys har begrensning at det ikke kan opprettes mottatt dato frem i tid.

#### Del 4 - Opprett forsendelse
I denne fasen opprettes forsendelse for alle vedtak som ble fattet i Del 3.
For å kunne teste forsendelse kan Del 3 kjøres med parameteren `simuler=true` slik at forsendelse kan opprettes uten at det fattes vedtak.

Denne kan trigges ved å kalle `POST /batch/forsendelse/opprett`

Med parametere:
- `prosesserFeilet` - Rekjør oppretting for forsendelser som tidligere har feilet
- `bestillingIds` - Komma-separert liste over `forsendelse_bestilling` id-er som skal slettes og gjennopprettes. Hvis det ikke er satt så opprettes det for alle `forsendelse_bestilling` uten `forsendelse_id`

NB!: Det er viktig at det gjøres stikkprøver ved å sjekke innhold på noen av brevene før distribusjon bestilles i neste fase.

#### Del 5 - Distribuer forsendelse
I denne fasen distribueres forsendelsene som ble opprettet i Del 4 til mottakerne (bidragspliktig og/eller bidragsmottaker).
Distribuering arkiverer forsendelsen i Joark og bestiller distribusjon av forsendelsen

Denne kan trigges ved å kalle `POST /batch/forsendelse/distribuer`

Med parametere:
- `bestillingIds` - Komma-separert liste over `forsendelse_bestilling` id-er som skal distribueres. Hvis det ikke er satt så distribueres det for alle `forsendelse_bestilling` som er opprettet og ikke distribuert
