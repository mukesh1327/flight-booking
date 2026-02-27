#!/usr/bin/env bash
set -euo pipefail

# Builds and pushes all service images to quay.
# Usage:
#   ./scripts/build-and-push-images.sh [version]
# Example:
#   ./scripts/build-and-push-images.sh v1

VERSION="${1:-v1}"
REGISTRY="quay.io/mukeshs1306"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

SERVICES=(
  "auth-service|springboot|8081-authservice|Containerfile|auth-service"
  "flight-service|quarkus|8082-flightservice|src/main/docker/Dockerfile.jvm|flight-service"
  "booking-service|dotnet|8083-bookingservice|Containerfile|booking-service"
  "customer-service|node|8084-customerservice|Containerfile|customer-service"
  "payment-service|python|8085-paymentservice|Containerfile|payment-service"
  "frontend-ui|frontend|skyfly-flight-booking|docker/Containerfile|frontend-ui"
)

for row in "${SERVICES[@]}"; do
  IFS='|' read -r service_name framework context_dir dockerfile image_name <<<"${row}"

  image_version="${REGISTRY}/${image_name}:${VERSION}"
  image_latest="${REGISTRY}/${image_name}:latest"
  service_path="${ROOT_DIR}/${context_dir}"
  dockerfile_path="${service_path}/${dockerfile}"

  # Fallback if a custom Quarkus Dockerfile path is used in other branches.
  if [[ "${framework}" == "quarkus" && ! -f "${dockerfile_path}" && -f "${service_path}/src/main/Dockerfile.jvm" ]]; then
    dockerfile_path="${service_path}/src/main/Dockerfile.jvm"
  fi

  echo "------------------------------------------------------------"
  echo "Service: ${service_name}"
  echo "Framework: ${framework}"
  echo "Context: ${context_dir}"
  echo "Dockerfile: ${dockerfile_path}"
  echo "Images: ${image_version}, ${image_latest}"

  case "${framework}" in
    springboot)
      (cd "${service_path}" && mvn clean package -DskipTests)
      ;;
    quarkus)
      (cd "${service_path}" && mvn clean package -DskipTests)
      ;;
    dotnet)
      (cd "${service_path}" && dotnet restore bookingservice.csproj && dotnet publish bookingservice.csproj -c Release -o publish)
      ;;
    frontend)
      (cd "${service_path}" && npm install && npm run build)
      ;;
    *)
      # No pre-build step required.
      ;;
  esac

  # Force remove existing local image builds if present.
  podman image rm -f "${image_version}" "${image_latest}" 2>/dev/null || true

  podman build \
    -f "${dockerfile_path}" \
    -t "${image_version}" \
    -t "${image_latest}" \
    "${service_path}"

  podman push "${image_version}"
  podman push "${image_latest}"
done

echo "All service images built and pushed."
