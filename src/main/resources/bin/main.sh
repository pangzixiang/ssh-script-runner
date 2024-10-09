#!/usr/bin/env bash
set -e

status() { echo ">>> $*" >&2; }
error() { echo "ERROR: $*"; exit 1; }
warning() { echo "WARNING: $*"; }

if [ -z "${GIT_SSH_URL}" ]; then
  error "git ssh url not provided"
else
  status "git ssh url is ${GIT_SSH_URL}"
fi

if [ -z "${GIT_BRANCH}" ]; then
  error "git branch not provided"
else
  status "git branch is ${GIT_BRANCH}"
fi

if [ -z "${PROCESS_ID}" ]; then
  error "process id not provided"
fi

mkdir -p /tmp/sshsrtmp
WORKING_DIR=/tmp/sshsrtmp/${PROCESS_ID}
status "working dir is ${WORKING_DIR}"

rm -rf "${WORKING_DIR}"
git clone --progress --depth=1 "${GIT_SSH_URL}" "${WORKING_DIR}"

if [ -f "${WORKING_DIR}/.sshsr/main.sh" ]; then
  cd "${WORKING_DIR}/.sshsr"
  status "start to run main script..."
  bash main.sh
else
  error "main script not found in .sshsr for your repository."
fi