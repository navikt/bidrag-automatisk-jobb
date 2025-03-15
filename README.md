# Bidrag-automatisk-jobb
Applikasjon for å aldersjustere barn i bidragssaker.

[![continuous integration](https://github.com/navikt/bidrag-automatisk-jobb/actions/workflows/deploy_q1.yaml/badge.svg)](https://github.com/navikt/bidrag-automatisk-jobb/actions/workflows/deploy_q1.yaml)
[![release bidrag-automatisk-jobb](https://github.com/navikt/bidrag-automatisk-jobb/actions/workflows/deploy_prod.yaml/badge.svg)](https://github.com/navikt/bidrag-automatisk-jobb/actions/workflows/deploy_prod.yaml)

## Beskrivelse
Bidrag-automatisk-jobb lagrer alle barn som er part i en bidragssak, og aldersjusterer disse. 
Aldersjustering skjer for alle barn året de fyller 6, 11 og 15 år basert på sjablong-verdier.

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