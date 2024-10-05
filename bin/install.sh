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
    x86_64) ARCH="x86" ;;
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

JDK_MIRROR_URL="https://mirrors.tuna.tsinghua.edu.cn/Adoptium/21/jdk/${ARCH}/linux/OpenJDK21U-jdk_x64_linux_hotspot_21.0.4_7.tar.gz"
status "Install JDK from ${JDK_MIRROR_URL}"
curl --fail --show-error --location --progress-bar -o "/tmp/jdk21.tar.gz" -H "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0.1 Safari/605.1.15" ${JDK_MIRROR_URL}
mkdir -p /tmp/jdk21
tar -zxf /tmp/jdk21.tar.gz --strip-components 1 -C /tmp/jdk21
export PATH=/tmp/jdk21/bin:$PATH
java --version

status "Building and Installing"
git clone --depth=1 ${REPOSITORY_URL} /tmp/${REPOSITORY_NAME}
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
ExecStart=java -jar $BINDIR/${REPOSITORY_NAME}
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