#!/bin/bash -e

NEW_VERSION=$1

./mvnw versions:set -DnewVersion="$NEW_VERSION"

echo "New version: $NEW_VERSION" >&2