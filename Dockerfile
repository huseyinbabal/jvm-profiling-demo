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

# Copy the built artifact from the builder stage
COPY --from=builder /target/jvm-profiling-demo-0.0.1-SNAPSHOT.jar /app/spring-boot-application.jar

# Run the application
ENTRYPOINT ["java", "-Dcom.sun.management.jmxremote.rmi.port=9090", "-Dcom.sun.management.jmxremote=true", "-Dcom.sun.management.jmxremote.port=9090", "-Dcom.sun.management.jmxremote.ssl=false", "-Dcom.sun.management.jmxremote.authenticate=false", "-Dcom.sun.management.jmxremote.local.only=false", "-jar", "/app/spring-boot-application.jar"]

