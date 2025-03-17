#!/bin/bash
kubectx nais-dev

deployment="deployment/bidrag-automatisk-jobb-q2"
[ "$1" == "q1" ] && deployment="deployment/bidrag-automatisk-jobb-q2"
echo "Henter miljøparametere fra deployment: $deployment"
kubectl exec --tty $deployment -- printenv | grep -E 'AZURE_|_URL|SCOPE|UNLEASH_' | grep -v -e 'BIDRAG_VEDTAK_URL' -e 'KODEVERK_URL' > src/test/resources/application-lokal-nais-secrets.properties