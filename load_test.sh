#!/bin/bash

URL="http://localhost:9980/rest-chain"
REQS_PER_SEC=1000

while true; do
  seq $REQS_PER_SEC | xargs -n1 -P1000 -I{} curl -s -o /dev/null "$URL" &
  sleep 1
done









