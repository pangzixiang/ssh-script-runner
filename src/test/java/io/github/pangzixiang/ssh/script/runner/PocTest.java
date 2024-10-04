package io.github.pangzixiang.ssh.script.runner;

import io.github.pangzixiang.ssh.script.runner.common.GitEmptyCredentialsProvider;
import io.github.pangzixiang.ssh.script.runner.config.AppConfiguration;
import io.github.pangzixiang.ssh.script.runner.common.StringLineOutputStream;
import io.github.pangzixiang.ssh.script.runner.service.SshKeyService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.config.hosts.HostConfigEntry;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.loader.KeyPairResourceLoader;
import org.apache.sshd.common.keyprovider.KeyIdentityProvider;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.git.transport.GitSshdSessionFactory;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.client.fs.SftpFileSystemClientSessionInitializer;
import org.apache.sshd.sftp.client.fs.SftpFileSystemInitializationContext;
import org.apache.sshd.sftp.client.fs.SftpFileSystemProvider;
import org.apache.sshd.sftp.client.impl.DefaultSftpClientFactory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Disabled
class PocTest {

    @Test
    @SneakyThrows
    void testSFTP2Git() throws IOException {
        SshKeyService sshKeyService = SshKeyService.getInstance();
        SshClient sshClient = SshClient.setUpDefaultClient();
        sshClient.setKeyIdentityProvider(KeyIdentityProvider.wrapKeyPairs(sshKeyService.getKeyPairs()));
        sshClient.start();

        GitSshdSessionFactory sshdFactory = new GitSshdSessionFactory(sshClient);// re-use the same client for all SSH sessions
        SshSessionFactory.setInstance(sshdFactory);


//        Git.cloneRepository()
//                .setBranch("main")
//                .setCloneAllBranches(false)
//                .setCredentialsProvider(new GitEmptyCredentialsProvider())
//                .setDirectory(new File("./.sshsr/test"))
//                .setNoTags()
//                .setURI("ssh://git@github.com:22/pangzixiang/ssh-script-runner-test-repo")
//                .call().close();

//        var ref = Git.lsRemoteRepository()
//                .setCredentialsProvider(new CredentialsProvider() {
//                    @Override
//                    public boolean isInteractive() {
//                        return false;
//                    }
//
//                    @Override
//                    public boolean supports(CredentialItem... items) {
//                        return false;
//                    }
//
//                    @Override
//                    public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
//                        return false;
//                    }
//                })
//                .setRemote("ssh://git@github.com:22/pangzixiang/ssh-script-runner-test-repo")
//                .call();

//        ref.forEach(ref1 -> {
//            log.info("{}", ref1.getName());
//        });

//        ClientSession session = sshClient.connect("git", "www.github.com", 22)
//                .verify(5, TimeUnit.SECONDS).getClientSession();
//        session.auth().verify(5, TimeUnit.SECONDS);
//        SftpClientFactory sftpClientFactory = new DefaultSftpClientFactory();
//
//        SftpClient sftpClient = sftpClientFactory.createSftpClient(session);

//        FileSystem fileSystem = sftpFileSystemProvider.getFileSystem(new URI("sftp://git@github.com/pangzixiang/ssh-script-runner-test-repo/.sshsr?branch=master"));

//        SftpClientFactory sftpClientFactory = SftpClientFactory.instance();
//        ClientSession session = sshClient.connect("git", "github.com", 22)
//                .verify(5, TimeUnit.SECONDS)
//                .getClientSession();
//
//        session.auth().verify(5, TimeUnit.SECONDS);
//        SftpClient sftpClient = sftpClientFactory.createSftpClient(session);
//        var dir = sftpClient.readDir("pangzixiang/ssh-script-runner-test-repo");
//        dir.forEach(dirEntry -> {
//            log.info("{}", dirEntry.getFilename());
//        });
    }

    @Test
    @SneakyThrows
    @Timeout(value = 15)
    void testSshConnect() {
        AppConfiguration appConfiguration = AppConfiguration.getInstance();
        KeyPairResourceLoader keyPairResourceLoader = SecurityUtils.getKeyPairResourceParser();
        Collection<KeyPair> keyPairs = keyPairResourceLoader.loadKeyPairs(null, PocTest.class.getClassLoader().getResource("ssh/id_rsa"), null);
        keyPairs.addAll(keyPairResourceLoader.loadKeyPairs(null, PocTest.class.getClassLoader().getResource("ssh/id_rsa2"), null));

        SshClient sshClient = SshClient.setUpDefaultClient();
        sshClient.setHostConfigEntryResolver(
                HostConfigEntry.toHostConfigEntryResolver(
                        List.of(
                                new HostConfigEntry("prd", "prd.whatsit.top", 22, "sshsr"),
                                new HostConfigEntry("baby-marker", "172.18.0.205", 22, "baby-marker", "prd")
                        )
                )
        );
        sshClient.setKeyIdentityProvider(KeyIdentityProvider.wrapKeyPairs(keyPairs));
        sshClient.start();

        ClientSession clientSession = sshClient.connect("baby-marker").verify(5, TimeUnit.SECONDS).getClientSession();
        clientSession.auth().verify(5, TimeUnit.SECONDS);
        ChannelExec channelExec = clientSession.createExecChannel("bash");
        channelExec.setIn(new FileInputStream("./test.sh"));
        channelExec.setRedirectErrorStream(true);
        channelExec.setOut(new StringLineOutputStream() {
            @Override
            protected void processLine(String line) {
                log.info(line);
            }
        });
        channelExec.open().verify(5, TimeUnit.SECONDS);
        channelExec.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 10000L);
        log.info("end with {} - {}", channelExec.getExitSignal(), channelExec.getExitStatus());
    }
}
