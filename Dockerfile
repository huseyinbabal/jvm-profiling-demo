# Stage 1: Build the application
FROM maven:3.9-amazoncorretto-17 as builder

# Copy the project files to the container
COPY ./pom.xml ./pom.xml

# Download all required dependencies into one layer
RUN mvn dependency:go-offline -B

# Copy your other files
COPY ./src ./src

# Build the application
RUN mvn package -DskipTests

# Stage 2: Create the runtime image
FROM amazoncorretto:17-alpine

# Install async profiler
RUN apk add --no-cache curl tar libstdc++

ARG ASYNC_PROFILER_VERSION="2.9"

RUN cd /tmp && \
    curl -L https://github.com/jvm-profiling-tools/async-profiler/releases/download/v${ASYNC_PROFILER_VERSION}/async-profiler-${ASYNC_PROFILER_VERSION}-linux-musl-x64.tar.gz -o async-profiler.tar.gz && \
    tar -xzf async-profiler.tar.gz -C /opt && \
    mv /opt/async-profiler-${ASYNC_PROFILER_VERSION}-linux-musl-x64 /opt/async-profiler && \
    rm /tmp/async-profiler.tar.gz

ENV PATH="/opt/async-profiler:${PATH}"

# Copy the built artifact from the builder stage
COPY --from=builder /target/jvm-profiling-demo-0.0.1-SNAPSHOT.jar /app/spring-boot-application.jar

# Set JMX envs
ENV JAVA_TOOL_OPTIONS_TEMP "-Dcom.sun.management.jmxremote \
                     -Dcom.sun.management.jmxremote.authenticate=false \
                     -Dcom.sun.management.jmxremote.ssl=false \
                     -Dcom.sun.management.jmxremote.local.only=false \
                     -Dcom.sun.management.jmxremote.port=9010 \
                     -Dcom.sun.management.jmxremote.rmi.port=9010 \
                     -Djava.rmi.server.hostname=127.0.0.1"

# Run the application
ENTRYPOINT ["java", "-jar", "-Xmx1G", "-Xms512M", "/app/spring-boot-application.jar"]

