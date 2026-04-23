#!/usr/bin/env bash

set -euo pipefail

# Fetch all submodule content.
git submodule sync --recursive
git submodule update --force --recursive --init
