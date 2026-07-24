FROM azul/zulu-openjdk-alpine:21-jre-headless@sha256:3dd53ed02013cb7caa41f5b4d516ebbb1c92561fd1deb5638357154bb9860071
COPY http4k-server-shutdown-integration-test.zip .
RUN ["unzip", "http4k-server-shutdown-integration-test.zip"]
CMD ["http4k-server-shutdown-integration-test/bin/http4k-server-shutdown-integration-test"]

