#!/usr/bin/env bash
set -e

status() { echo ">>> $*" >&2; }
error() { echo "ERROR: $*"; exit 1; }
warning() { echo "WARNING: $*"; }

if [ -z "${MAIN_SCRIPT}" ]; then
  error "main script not provided"
else
  status "main script is ${MAIN_SCRIPT}"
fi

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

if [ -f "${WORKING_DIR}/.sshsr/${MAIN_SCRIPT}" ]; then
  cd "${WORKING_DIR}/.sshsr"
  status "start to run main script ${MAIN_SCRIPT}..."
  bash "${MAIN_SCRIPT}"
else
  error "main script ${MAIN_SCRIPT} not found in folder .sshsr for your repository."
fi