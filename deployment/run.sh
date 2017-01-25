#!/usr/bin/env bash
if [ ! -d "data" ]; then
  (mkdir data)
fi
(docker-compose stop ; docker-compose up -d)
if [ $? -eq 0 ]; then
    echo "API Running on port 4242"
    echo "Postgres database available on port 5431"
else
    echo "An error occured"
fi
