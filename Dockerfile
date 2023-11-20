FROM gcr.io/distroless/java21-debian12:nonroot
LABEL org.opencontainers.image.source=https://github.com/navikt/fp-oversikt
# Healtcheck lokalt/test
COPY --from=busybox:stable-musl /bin/wget /usr/bin/wget

# Working dir for RUN, CMD, ENTRYPOINT, COPY and ADD (required because of nonroot user cannot run commands in root)
WORKDIR /app

COPY target/classes/logback*.xml conf/
COPY target/lib/*.jar lib/
COPY target/app.jar .

ENV TZ=Europe/Oslo
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 \
    -XX:+PrintCommandLineFlags \
    -Djava.security.egd=file:/dev/urandom \
    -Duser.timezone=Europe/Oslo \
    -Dlogback.configurationFile=conf/logback.xml"

CMD ["app.jar"]
