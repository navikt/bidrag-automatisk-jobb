#!/bin/bash
kubectx dev-gcp

deployment="deployment/bidrag-automatisk-jobb-q2"
[ "$1" == "q1" ] && deployment="deployment/bidrag-automatisk-jobb-q1"
echo "Henter miljøparametere fra deployment: $deployment"
kubectl exec --tty $deployment -- printenv | grep -E 'AZURE_|_URL|SCOPE|UNLEASH_'> src/test/resources/application-lokal-nais-secrets.properties