#!/usr/bin/env bash
mvn clean compile package
mv target/rssagg-0.2.jar deployment/rssagg.jar
git add deployment/rssagg.jar
git commit -m "updated jar $(date +'%Y-%m-%d-%h-%H-%S-%M')"
git push