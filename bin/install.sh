#!/usr/bin/env bash
set -eu

REPOSITORY_URL=git@github.com:pangzixiang/ssh-script-runner.git
REPOSITORY_NAME=ssh-script-runner

status() { echo ">>> $*" >&2; }
error() { echo "ERROR $*"; exit 1; }
warning() { echo "WARNING: $*"; }

available() { command -v "$1" >/dev/null; }

[ "$(uname -s)" = "Linux" ] || error 'This script is intended to run on Linux only.'

ARCH=$(uname -m)
case "$ARCH" in
    x86_64) ARCH="x64" ;;
    aarch64|arm64) ARCH="aarch64" ;;
    *) error "Unsupported architecture: $ARCH" ;;
esac

IS_WSL2=false

KERN=$(uname -r)
case "$KERN" in
    *icrosoft*WSL2 | *icrosoft*wsl2) IS_WSL2=true;;
    *icrosoft) error "Microsoft WSL1 is not currently supported. Please upgrade to WSL2 with 'wsl --set-version <distro> 2'" ;;
    *) ;;
esac

SUDO=
if [ "$(id -u)" -ne 0 ]; then
    # Running as root, no need for sudo
    if ! available sudo; then
        error "This script requires superuser permissions. Please re-run as root."
    fi
    SUDO="sudo"
fi

### unset proxy to avoid maven download issue
unset HTTPS_PROXY
unset HTTP_PROXY
unset https_proxy
unset http_proxy
#############################################

if [ ! -f "${HOME}"/.jdk/jdk21/bin/java ]; then
  JDK_MIRROR_URL="https://mirrors.tuna.tsinghua.edu.cn/Adoptium/21/jdk/${ARCH}/linux/OpenJDK21U-jdk_${ARCH}_linux_hotspot_21.0.5_11.tar.gz"
  status "Installing JDK from ${JDK_MIRROR_URL}"
  mkdir -p "${HOME}"/.jdk
  curl --fail --show-error --location --progress-bar -o "${HOME}"/.jdk/jdk21.tar.gz ${JDK_MIRROR_URL}
else
  status "JDK exists hence no need to download"
fi

mkdir -p "${HOME}"/.jdk/jdk21
tar -zxf "${HOME}"/.jdk/jdk21.tar.gz --strip-components 1 -C "${HOME}"/.jdk/jdk21
export PATH="${HOME}"/.jdk/jdk21/bin:$PATH
java --version

if [ ! -f "${HOME}"/.node/node20/bin/npm ]; then
  NODE_MIRROR_URL="https://npmmirror.com/mirrors/node/v20.17.0/node-v20.17.0-linux-${ARCH}.tar.gz"
  status "Installing Node from ${NODE_MIRROR_URL}"
  mkdir -p "${HOME}"/.node
  curl --fail --show-error --location --progress-bar -o "${HOME}"/.node/node20.tar.gz ${NODE_MIRROR_URL}
else
  status "Node exists hence no need to download"
fi

mkdir -p "${HOME}"/.node/node20
tar -zxf "${HOME}"/.node/node20.tar.gz --strip-components 1 -C "${HOME}"/.node/node20
export PATH="${HOME}"/.node/node20/bin:$PATH
node --version
npm --version
npm config set registry https://registry.npmmirror.com

status "Building and Installing"
rm -rf /tmp/${REPOSITORY_NAME}
git clone --progress --depth=1 ${REPOSITORY_URL} /tmp/${REPOSITORY_NAME}
cd /tmp/${REPOSITORY_NAME}/ui
npm install
npm run build
cd /tmp/${REPOSITORY_NAME}
./mvnw clean package -s .mvn/settings.xml

for BINDIR in /usr/local/bin /usr/bin /bin; do
    echo "$PATH" | grep -q $BINDIR && break || continue
done
$SUDO install -o0 -g0 -m755 -d "$BINDIR"
$SUDO install -o0 -g0 -m755 target/${REPOSITORY_NAME}* "$BINDIR"/${REPOSITORY_NAME}
install_success() {
    status 'Install complete.'
}
trap install_success EXIT

configure_systemd() {
    status "Creating ${REPOSITORY_NAME} systemd service..."
    cat <<EOF | $SUDO tee /etc/systemd/system/${REPOSITORY_NAME}.service >/dev/null
[Unit]
Description=${REPOSITORY_NAME}
After=network-online.target

[Service]
ExecStart=$HOME/.jdk/jdk21/bin/java -Dssh-script-runner.env=prod -Dssh-script-runner.log.home=/var/log/ssh-script-runner -jar $BINDIR/${REPOSITORY_NAME}
Restart=always
RestartSec=3
Environment="PATH=$PATH"

[Install]
WantedBy=default.target
EOF
    SYSTEMCTL_RUNNING="$(systemctl is-system-running || true)"
    case $SYSTEMCTL_RUNNING in
        running|degraded)
            status "Enabling and starting ${REPOSITORY_NAME} service..."
            $SUDO systemctl daemon-reload
            $SUDO systemctl enable ${REPOSITORY_NAME}

            start_service() { $SUDO systemctl restart ${REPOSITORY_NAME}; }
            trap start_service EXIT
            ;;
    esac
}

if available systemctl; then
    configure_systemd
fi

install_success