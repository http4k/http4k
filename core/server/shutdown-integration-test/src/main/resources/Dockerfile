FROM azul/zulu-openjdk-alpine:21-jre-headless
COPY http4k-server-shutdown-integration-test-LOCAL.zip .
RUN ["unzip", "http4k-server-shutdown-integration-test-LOCAL.zip"]
CMD ["http4k-server-shutdown-integration-test-LOCAL/bin/http4k-server-shutdown-integration-test"]

