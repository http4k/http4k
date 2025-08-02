FROM azul/zulu-openjdk-alpine:21-jre-headless
COPY http4k-server-shutdown-integration-test.zip .
RUN ["unzip", "http4k-server-shutdown-integration-test.zip"]
CMD ["http4k-server-shutdown-integration-test/bin/http4k-server-shutdown-integration-test"]

