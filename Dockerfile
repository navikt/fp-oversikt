FROM gcr.io/distroless/java21:nonroot
LABEL org.opencontainers.image.source=https://github.com/navikt/fp-oversikt

ENV TZ=Europe/Oslo
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 \
    -XX:+PrintCommandLineFlags \
    -Djava.security.egd=file:/dev/urandom \
    -Duser.timezone=Europe/Oslo \
    -Dlogback.configurationFile=conf/logback.xml"

# Config
COPY target/classes/logback*.xml ./conf/
# Application Container (Jetty)
COPY target/lib/*.jar ./lib/
COPY target/app.jar ./
# Healtcheck lokalt/test
COPY --from=busybox:stable-musl /bin/wget /usr/bin/wget

CMD ["app.jar"]
