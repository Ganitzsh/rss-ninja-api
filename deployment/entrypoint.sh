#!/bin/sh
sleep 10
java -Dninja.port=8080 -Dninja.mode=test -Dninja.external.configuration=api.conf -jar rssagg-0.1.jar