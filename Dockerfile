#
# Build stage - we need the full JDK and Maven
#
FROM maven:3.9.6-eclipse-temurin-17 AS build
ENV HOME=/opt/app
RUN mkdir -p $HOME
WORKDIR $HOME
ADD . $HOME
RUN --mount=type=cache,target=/root/.m2 mvn -f $HOME/pom.xml clean package

#
# Package stage - use a much smaller base image with only the JRE
#
FROM eclipse-temurin:17-jre-jammy
ARG JAR_FILE=/opt/app/target/*with-dependencies*.jar
COPY --from=build $JAR_FILE /app/runner.jar
ENTRYPOINT ["java", "-jar", "/app/runner.jar"]
