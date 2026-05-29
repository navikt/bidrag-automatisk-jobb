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
select * from aldersjustering;
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