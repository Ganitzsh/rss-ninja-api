FROM openjdk:8

RUN mkdir /usr/src/myapp
COPY ./rssagg-0.1.jar /usr/src/myapp
COPY ./entrypoint.sh /usr/src/myapp
COPY ./api.conf /usr/src/myapp
WORKDIR /usr/src/myapp

EXPOSE 8080

ENTRYPOINT [ "/usr/src/myapp/entrypoint.sh"]