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
  "auth-service|8081-authservice|Dockerfile.mvnbuild|auth-service"
  "flight-service|8082-flightservice|Dockerfile.mvnbuild|flight-service"
  "booking-service|8083-bookingservice|Dockerfile|booking-service"
  "customer-service|8084-customerservice|Dockerfile|customer-service"
  "payment-service|8085-paymentservice|Dockerfile|payment-service"
)

for row in "${SERVICES[@]}"; do
  IFS='|' read -r service_name context_dir dockerfile image_name <<<"${row}"

  image_version="${REGISTRY}/${image_name}:${VERSION}"
  image_latest="${REGISTRY}/${image_name}:latest"

  echo "------------------------------------------------------------"
  echo "Service: ${service_name}"
  echo "Context: ${context_dir}"
  echo "Dockerfile: ${dockerfile}"
  echo "Images: ${image_version}, ${image_latest}"

  # Force remove existing local image builds if present.
  podman image rm -f "${image_version}" "${image_latest}" 2>/dev/null || true

  podman build \
    -f "${ROOT_DIR}/${context_dir}/${dockerfile}" \
    -t "${image_version}" \
    -t "${image_latest}" \
    "${ROOT_DIR}/${context_dir}"

  podman push "${image_version}"
  podman push "${image_latest}"
done

echo "All service images built and pushed."
