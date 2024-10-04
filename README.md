# SSH Script Runner
> connect to remote server to run script via SSH triggered by REST API
## Technical Stack
- jdk 21
- maven
- vert.x
- apache sshd
- HOCON conf
## Install
1. Create New User in remote servers
```shell
sudo useradd -r -m -s /bin/bash sshsr
sudo passwd sshsr
```
2. add the public key to authorized_keys in remote servers
```shell
# ssh-keygen -t rsa -b 4096
# option 1
# ssh-copy-id sshsr@server_ip
# option 2
echo $(cat id_rsa.pub) >> .ssh/authorized_keys
```