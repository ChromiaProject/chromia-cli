FROM adoptopenjdk/openjdk11:jre-11.0.16.1_1

ARG CLI_JAR=chromia-cli-dev
COPY target/$CLI_JAR-dist /usr/share/chr
RUN ln -s /usr/share/chr/bin/chr /usr/bin/chr

EXPOSE 7740 9870
WORKDIR /usr/app
ENTRYPOINT ["chr"]
