FROM eclipse-temurin:26-jdk

ARG MAVEN_VERSION=3.9.16
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && curl -fsSL "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
        | tar -xz -C /opt \
    && ln -s "/opt/apache-maven-${MAVEN_VERSION}" /opt/maven
ENV PATH="/opt/maven/bin:${PATH}"

WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    printf '%s\n' \
      '<settings><mirrors><mirror><id>aliyun</id><url>https://maven.aliyun.com/repository/public</url><mirrorOf>*</mirrorOf></mirror></mirrors></settings>' \
      > /root/.m2/settings.xml \
    && mvn -B -DskipTests package

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "target/xt_interview-1.0.0.jar"]
