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
Første fase er å kjøre batchen `OpprettAldersjusteringerBidragBatch`, som oppretter grunnlaget i `aldersjustering`-tabellen.

Batchen leser barn fra `barn`-tabellen for valgt år (`aar`) med følgende utvalg:
- `:år - EXTRACT(YEAR FROM fodselsdato) IN (6, 11, 15)`
- `bidrag_fra <= aldersjusteringsdato`
- `bidrag_til IS NULL OR bidrag_til > aldersjusteringsdato`

For hvert treff opprettes en aldersjustering med status `UBEHANDLET` (dersom den ikke allerede finnes for samme barn og aldersgruppe).

`batch_id` settes til `aldersjustering_bidrag_<årstall>`.
Hvis det er feil i uttrekket, kan rader med årets `batch_id` slettes og batchen kjøres på nytt.
**NB: Ikke slett rader fra eldre batchkjøringer.**

Batchen trigges med:
`POST /aldersjustering/batch/bidrag/opprett?aar=<årstall>`

Støttet parameter:
- `aar` (påkrevd): året det skal aldersjusteres for.
- `aldersjusteringsdato` (valgfri): cutoff-dato for uttrekket. Hvis ikke satt, brukes `01.07.<aar>`.

Status på kjøringen kan sjekkes i `batch_job_execution`-tabellen (jobbnavn `opprettAldersjusteringerBidragJob`).
Når status er `COMPLETED`, er kjøringen ferdig.

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
- `statuser` - Kommaseparert liste over statuser som skal inkluderes i kjøringen. Default er `UBEHANDLET,FEILET,SIMULERT`. Se statusbeskrivelser under.
- `barn` - liste over barnider som det ønskes å beregnes for. Hvis det ikke er satt så beregnes det for alle.

Andre fase er å kjøre beregning av aldersjustering.
I denne kjøringen beregnes aldersjustering for alle barn som ble opprettet i fase 1.

Aldersjustering beregning kan føre til følgende utfall:

Status etter beregning:
- `UBEHANDLET` - Ny aldersjustering som ikke er beregnet ennå (startverdi fra Del 1).
- `SIMULERT` - Aldersjusteringen ble behandlet med suksess men det ble ikke opprettet en vedtaksforslag (simuleringsmodus).
- `BEHANDLET` - Aldersjusteringen ble behandlet med suksess og det ble opprettet et vedtaksforslag. Beregning vil ikke kjøres på nytt med mindre `BEHANDLET` er eksplisitt inkludert i `statuser`. Eksisterende vedtaksforslag slettes og nytt opprettes — vedtak som allerede er fattet påvirkes ikke.
- `FEILET` - Det skjedde en feil under beregning av aldersjusteringen. Feil begrunnelsen er lagret i `begrunnelse` kolonnen i aldersjustering tabellen.
- `FATTE_VEDTAK_FEILET` - Et tidligere forsøk på å fatte vedtak (Del 3) feilet, typisk fordi saksbehandler fattet vedtak manuelt etter at beregningen ble utført. Disse kan rekjøres ved å inkludere `FATTE_VEDTAK_FEILET` i `statuser`.
- `SLETTET` - Aldersjusteringen er markert som slettet. Kan rekjøres ved å inkludere `SLETTET` i `statuser`.

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

Eller for mer detaljert utrekk for testing

```sql
SELECT
a.status,
saksnummer,
a.behandlingstype,
b.fodselsdato as fødselsdato,
resultat_siste_vedtak,
fattet_tidspunkt,
CASE
    WHEN array_length(begrunnelse, 1) IS NULL THEN ''
    ELSE string_agg(
            CONCAT(
                    UPPER(LEFT(REPLACE(begrunnelse_item, '_', ' '), 1)),
                    LOWER(SUBSTRING(REPLACE(begrunnelse_item, '_', ' '), 2))
            ), ', '
         )
    END AS begrunnelser,

CASE
    WHEN behandlingstype = 'MANUELL' THEN 'Ja'
    ELSE 'Nei'
    END AS skal_behandles_manuelt,
CASE
    WHEN behandlingstype = 'FATTET_FORSLAG' THEN 'Ja'
    ELSE 'Nei'
END AS fattes_vedtak,
a.vedtak as vedtak_id,
a.vedtaksid_beregning
FROM aldersjustering a
         INNER JOIN public.barn b ON b.id = a.barn_id
         LEFT JOIN LATERAL unnest(
        CASE
            WHEN array_length(begrunnelse, 1) > 0 THEN begrunnelse
            ELSE ARRAY[NULL]::text[]
            END
                           ) AS t(begrunnelse_item) ON array_length(begrunnelse, 1) > 0
WHERE a.status = 'BEHANDLET' and a.batch_id = 'aldersjustering_bidrag_2026'
GROUP BY b.id, a.id, a.oppgave, vedtak, vedtaksid_beregning, b.fodselsdato, b.kravhaver, saksnummer, begrunnelse, resultat_siste_vedtak, behandlingstype,b.skyldner
ORDER BY begrunnelser;
```

For statistikk
```sql
SELECT
    behandlingstype,
    CASE behandlingstype
        WHEN 'FATTET_FORSLAG' THEN 'Vedtak fattes automatisk'
        WHEN 'MANUELL'        THEN 'Skal behandles manuelt'
        WHEN 'INGEN'          THEN 'Ingen aldersjustering'
        WHEN 'FEILET'         THEN 'Feilet'
        ELSE                       'Ukjent / ikke beregnet'
    END                          AS beskrivelse,
    COUNT(*)                     AS totalt
FROM aldersjustering a
         INNER JOIN barn b ON b.id = a.barn_id
WHERE a.batch_id = 'aldersjustering_bidrag_2026'
GROUP BY behandlingstype
ORDER BY totalt DESC;
```

#### Del 3 - Fatte vedtak
I denne fasen fattes det vedtak for alle vedtaksforslagene som ble opprettet i Del 2.
Da endres vedtaksforslag til et vedtak.

Denne kan trigges ved å kalle `POST /aldersjustering/batch/bidrag/fattVedtak`

Batchen behandler kun aldersjusteringer som oppfyller:
- `status = BEHANDLET`
- `behandlingstype IN (MANUELL, FATTET_FORSLAG, INGEN)` (kan overstyres med parameter)
- `vedtak ikke fattet`

Med parametere:
- `barn` - liste over barnider som det ønskes å fattes vedtak for. Hvis det ikke er satt så fattes det vedtak for alle
- `behandlingstyper` - Liste over behandlingstyper det skal fattes vedtak for. Default: `MANUELL,FATTET_FORSLAG,INGEN`
- `simuler` - Default er `true`. Når `true` fattes ingen vedtak, men batchen kan fortsatt oppretter forsendelse bestilling (se under)
- `kunRedusertBidrag` - Fatte kun vedtak for casene hvor aldersjusteringen fører til at bidraget reduseres. Dette kan brukes for å fatte vedtak for å unngå B4 i regnskapet.

Resultat av kjøring:
- `simuler=false`: vedtak fattes, og status settes til `FATTET` ved suksess eller `FATTE_VEDTAK_FEILET` ved feil.
- `simuler=true`: vedtak fattes ikke men det opprettes en bestilling av forsendelse for vedtak.

> **Merk:** Status `FATTE_VEDTAK_FEILET` kan oppstå dersom saksbehandler har fattet vedtak manuelt i perioden etter at beregningen i Del 2 ble utført.
> I slike tilfeller vil batchen forsøke å fatte vedtak på et grunnlag som ikke lenger er gyldig.
> Disse sakene kan rekjøres fra Del 2 ved å inkludere `FATTE_VEDTAK_FEILET` i `statuser`-parameteren.

Forsendelsebestilling i Del 3:
- Forsendelsebestilling opprettes kun for aldersjusteringer med `behandlingstype = FATTET_FORSLAG`
- Det opprettes bestilling i `forsendelse_bestilling` for både BP og BM (`ALDERSJUSTERING_BIDRAG`)
- Dette gjelder også når `simuler=true`, og kan brukes for å teste brev/forsendelse før det fattes vedtak

NB!: Viktig at for de manuelle så må vedtak fattes etter 1. Juli. Det er fordi det opprettes en søknad med mottatt dato 1. Juli. og Bisys har begrensning at det ikke kan opprettes mottatt dato frem i tid.
#### Testing av forsendelse
Kall `POST /aldersjustering/batch/bidrag/fattVedtak`

Med parameter:
- `simuler=true`

Det fattes ingen vedtak men det **opprettes en bestilling av forsendelse for vedtak** hvor det opprettes rad i `forsendelse_bestilling`-tabellen med bestilling for BM og BP (for `behandlingstype = FATTET_FORSLAG`).
Dette kan brukes til å teste forsendelse/brev før det fattes vedtak.

#### Del 3b - Lagre B4-informasjon
Denne fasen henter og lagrer B4-avregningsbeløp fra reskontro for alle aldersjusteringer som ble fattet i Del 3.
Den **bør kjøres etter Del 3** og kan kjøres én eller flere ganger — rader der `b4_belop` allerede er satt hoppes automatisk over.

Denne kan trigges ved å kalle `POST /aldersjustering/batch/bidrag/lagreB4Informasjon`

B4-beløpet (transaksjonskode `B4`/`D4`) oppstår når BM skylder BP penger — typisk fordi aldersjustert bidrag er lavere enn allerede utbetalt bidrag for gjeldende periode.
Kun beløp større enn null lagres i `b4_belop`-kolonnen i `aldersjustering`-tabellen.

Med parametere:
- `aar` (påkrevd) - årstall som filtrerer på `fattet_tidspunkt`. Skal samsvare med året Del 3 ble kjørt.
- `barn` (valgfri) - komma-separert liste over barn-id-er. Hvis ikke satt kjøres alle fatlede aldersjusteringer for angitt år.

Hent oversikt over lagrede B4-beløp:

```sql
SELECT b.saksnummer, b.kravhaver, b.skyldner, a.b4_belop
FROM aldersjustering a
INNER JOIN barn b ON b.id = a.barn_id
WHERE a.b4_belop IS NOT NULL
  AND a.batch_id = 'aldersjustering_bidrag_2026'
ORDER BY a.b4_belop DESC;
```

#### Del 4 - Opprett forsendelse
I denne fasen opprettes forsendelse i bidrag-dokument-forsendelse for forsendelsebestillinger fra Del 3.
For å kunne teste forsendelse kan Del 3 kjøres med parameteren `simuler=true`, slik at forsendelse kan opprettes uten at vedtak fattes.

Denne kan trigges ved å kalle `POST /batch/forsendelse/opprett`

Uten parametere:
- Batchen finner alle aktive bestillinger som mangler opprettet forsendelse, og oppretter forsendelse for disse.

Med parametere:
- `prosesserFeilet` (default `false`) - bestemmer om bestillinger som tidligere har feilet skal forsøkes på nytt
- `bestillingIder` - komma-separert liste over bestillinger som skal kjøres på nytt. For disse slettes eventuell eksisterende forsendelse før ny opprettes

Når `bestillingIder` er satt:
- Batchen begandler kun disse bestillingene.
- Brukes typisk ved feilretting eller når enkeltbrev må opprettes på nytt.

> **NB!:** Det er viktig at det gjøres stikkprøver ved å sjekke innhold på noen av brevene før distribusjon bestilles i neste fase.

#### Del 5 - Distribuer forsendelse
I denne fasen distribueres forsendelsene som ble opprettet i Del 4.
Distribuering arkiverer forsendelsen i Joark og bestiller distribusjon.

Denne kan trigges ved å kalle `POST /batch/forsendelse/distribuer`

Uten parametere:
- Batchen distribuerer alle opprettede og aktive forsendelser som ikke allerede er distribuert.

Med parametere:
- `bestillingIder` - komma-separert liste over `forsendelse_bestilling`-id-er som skal distribueres

Når `bestillingIder` er satt:
- Batchen distribuerer kun de angitte bestillingene.
- Dette er nyttig ved kontrollert distribusjon av enkelte forsendelser.

> **NB!:** Husk å varsle i slack kanalen [#team-dokumenthåndtering](https://nav-it.slack.com/archives/C6W9E5GPJ) at det skal kjøres batch med opprettelse og distribusjon av vedtaksbrev for aldersjustering av barnebidrag
> Hvis distribusjon feiler fordi alle dokumenter ikke er ferdigstilt, må man enten vente til dokumentene er klare, eller opprette forsendelsen på nytt i Del 4 ved å legge inn `forsendelse_bestilling`-id, og deretter kjøre distribusjon på nytt.