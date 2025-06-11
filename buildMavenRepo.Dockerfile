# Stage 0: Build legacy versions with JDK8
FROM adoptopenjdk:8-jdk-hotspot as jdk8

# Set working directory
WORKDIR /app

# Install dependencies
RUN apt-get update && apt-get install -y wget git

# Download the latest BuildTools.jar from SpigotMC
RUN wget -O BuildTools.jar https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar

# Build legacy Spigot versions that require JDK8
RUN java -jar BuildTools.jar --rev 1.8.8 && \
    java -jar BuildTools.jar --rev 1.9.4 && \
    java -jar BuildTools.jar --rev 1.10.2 && \
    java -jar BuildTools.jar --rev 1.11.2 && \
    java -jar BuildTools.jar --rev 1.12.2 && \
    java -jar BuildTools.jar --rev 1.13 && \
    java -jar BuildTools.jar --rev 1.13.2 && \
    rm -r /app/*

# Stage 1: Build older versions with jdk13
FROM adoptopenjdk:13-jdk-hotspot as jdk13

# Copy Maven repository from JDK8 stage
COPY --from=jdk8 /root/.m2 /root/.m2

# Set working directory
WORKDIR /app

# Install dependencies
RUN apt-get update && apt-get install -y wget git

# Download the latest BuildTools.jar from SpigotMC
RUN wget -O BuildTools.jar https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar

RUN java -jar BuildTools.jar --rev 1.14.4 && \
    java -jar BuildTools.jar --rev 1.15.2 && \
    java -jar BuildTools.jar --rev 1.16.1 && \
    java -jar BuildTools.jar --rev 1.16.3 && \
    java -jar BuildTools.jar --rev 1.16.5 && \
    rm -r /app/*

# Stage 2: Build older versions with JDK17
FROM openjdk:17-slim as jdk17

COPY --from=jdk13 /root/.m2 /root/.m2

WORKDIR /app

# Install dependencies
RUN apt-get update && apt-get install -y wget git

# Download the latest BuildTools.jar from SpigotMC
RUN wget -O BuildTools.jar https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar

# Build our Spigot jar files
RUN java -jar BuildTools.jar --remapped --rev 1.17.1 && \
    java -jar BuildTools.jar --remapped --rev 1.18 && \
    java -jar BuildTools.jar --remapped --rev 1.18.2 && \
    java -jar BuildTools.jar --remapped --rev 1.19 && \
    java -jar BuildTools.jar --remapped --rev 1.19.3 && \
    java -jar BuildTools.jar --remapped --rev 1.19.4 && \
    java -jar BuildTools.jar --remapped --rev 1.20 && \
    java -jar BuildTools.jar --remapped --rev 1.20.2 && \
    java -jar BuildTools.jar --remapped --rev 1.20.4 && \
    rm -r /app/*

# Stage 3: Build current versions with JDK21
FROM openjdk:21-slim as jdk21

COPY --from=jdk17 /root/.m2 /root/.m2

WORKDIR /app

# Install dependencies
RUN apt-get update && apt-get install -y wget git

# Download the latest BuildTools.jar from SpigotMC
RUN wget -O BuildTools.jar https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar

# Build our Spigot jar files
RUN java -jar BuildTools.jar --remapped --rev 1.20.6 && \
    java -jar BuildTools.jar --remapped --rev 1.21 && \
    java -jar BuildTools.jar --remapped --rev 1.21.1 && \
    rm -r /app/*

# Stage 4: Busybox container to keep it running so we can copy the maven repo
FROM --platform=linux/amd64 busybox:latest AS busybox

COPY --from=jdk21 /root/.m2 /root/.m2

# Keep container running
CMD ["sleep", "infinity"]


