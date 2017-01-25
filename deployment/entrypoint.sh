#!/bin/sh
echo "Waiting 20 seconds for database to be ready"
sleep 20
java -Dninja.port=8080 -Dninja.mode=test -Dninja.external.configuration=api.conf -jar rssagg.jar