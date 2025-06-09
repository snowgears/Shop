#!/bin/bash

# Define variables
IMAGE_NAME="spigot-buildtools:dev"
CONTAINER_NAME="spigot-builder"
LOCAL_DIR="./SpigotBuildTools-two"
DOCKERHUB_IMAGE="ostlerdev/spigot-maven:latest"

# Add a condition to check if the SpigotBuildTools directory already exists and has content
if [ -d ${LOCAL_DIR} ] && [ "$(ls -A ${LOCAL_DIR})" ]; then
  echo "SpigotBuildTools directory already exists, skipping build..."
else
  # Create local directory if it doesn't exist
  mkdir -p ${LOCAL_DIR}

  # Remove a previous container if it still exists
  docker stop ${CONTAINER_NAME} 2>/dev/null || true
  docker rm ${CONTAINER_NAME} 2>/dev/null || true

  # Determine whether to use local build or Docker Hub
  if [ "$1" == "local" ]; then
      # Build the Docker image locally
      docker build --cache-from ${DOCKERHUB_IMAGE} -t ${IMAGE_NAME} -f buildMavenRepo.Dockerfile . # --progress=plain
  else
      # Pull the Docker image from Docker Hub
      docker pull ${DOCKERHUB_IMAGE}
      IMAGE_NAME=${DOCKERHUB_IMAGE}
  fi

  # Run the Docker container
  docker run -d --name ${CONTAINER_NAME} ${IMAGE_NAME}

  # Wait for the container to finish
  #docker wait ${CONTAINER_NAME}

  # Copy the Maven repo from the docker image to the local directory
  docker cp ${CONTAINER_NAME}:/root/.m2 ${LOCAL_DIR}
  mv ${LOCAL_DIR}/.m2 ${LOCAL_DIR}/maven

  # Clean up the container
  docker stop ${CONTAINER_NAME}
  docker rm ${CONTAINER_NAME}

  echo "Build completed and files copied to ${LOCAL_DIR}."
fi
