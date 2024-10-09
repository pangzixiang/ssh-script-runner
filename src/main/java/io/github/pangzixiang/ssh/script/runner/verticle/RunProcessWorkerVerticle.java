package io.github.pangzixiang.ssh.script.runner.verticle;

import io.github.pangzixiang.ssh.script.runner.common.SSEOutputStream;
import io.github.pangzixiang.ssh.script.runner.handler.SSESubscriptionHandler;
import io.github.pangzixiang.ssh.script.runner.pojo.TriggerRunRequest;
import io.github.pangzixiang.ssh.script.runner.service.SshKeyService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.config.hosts.HostConfigEntry;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.KeyIdentityProvider;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.client.fs.SftpFileSystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RunProcessWorkerVerticle extends AbstractVerticle {
    public static final String RUN_PROCESS_ADDRESS = UUID.randomUUID().toString();
    private final SshKeyService sshKeyService;
    private static final String ENV_TEMPLATE = "GIT_SSH_URL=%s GIT_BRANCH=%s PROCESS_ID=%s GIT_SSH_COMMAND='ssh -o StrictHostKeyChecking=no -i %s'";
    public RunProcessWorkerVerticle() {
        this.sshKeyService = SshKeyService.getInstance();
    }
    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        log.info("Starting to deploy RunProcessWorkerVerticle");
        getVertx().eventBus().consumer(RUN_PROCESS_ADDRESS).handler(message -> {
            File gitSshKey = new File(sshKeyService.getKeyDir(), "sshsr_git.pri");
            if (!gitSshKey.exists()) {
                publishLog("git ssh key not exists, please generate or upload and named with sshsr_git.pri.");
                return;
            }
            TriggerRunRequest runRequest = (TriggerRunRequest) message.body();
            String id = UUID.randomUUID().toString().replace("-", "");
            log.info("Start to handle script runner request {} (processId={})", runRequest, id);
            publishLog("process started for %s".formatted(runRequest));

            try (SshClient sshClient = SshClient.setUpDefaultClient()){
                sshClient.setKeyIdentityProvider(KeyIdentityProvider.wrapKeyPairs(sshKeyService.getKeyPairs()));
                TriggerRunRequest.Server jumpServer = runRequest.getJumpServer();
                TriggerRunRequest.Server targetServer = runRequest.getTargetServer();
                List<HostConfigEntry> hostConfigEntries = new ArrayList<>();
                hostConfigEntries.add(new HostConfigEntry("target", targetServer.getHost(), targetServer.getPort(), targetServer.getUsername(), jumpServer == null ? null : "jump"));
                if (jumpServer != null) {
                    hostConfigEntries.add(new HostConfigEntry("jump", jumpServer.getHost(), jumpServer.getPort(), jumpServer.getUsername()));
                }
                sshClient.setHostConfigEntryResolver(HostConfigEntry.toHostConfigEntryResolver(hostConfigEntries));
                sshClient.start();

                ClientSession clientSession = sshClient.connect("target")
                                .verify(5, TimeUnit.SECONDS).getClientSession();
                clientSession.auth().verify(5, TimeUnit.SECONDS);
                publishLog("succeeded to connect to target %s".formatted(targetServer));

                SftpFileSystem sftpFileSystem = SftpClientFactory.instance().createSftpFileSystem(clientSession);
                Path targetDir = sftpFileSystem.getDefaultDir().resolve(".ssh");
                Path targetKeyFilePath = targetDir.resolve("sshsr_git.pri");
                Files.createDirectories(targetDir);
                Files.copy(new FileInputStream(gitSshKey), targetKeyFilePath, StandardCopyOption.REPLACE_EXISTING);
                Files.setPosixFilePermissions(targetKeyFilePath, Set.of(PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
                String env = ENV_TEMPLATE.formatted(runRequest.getGitSshUrl(), runRequest.getBranch(), id, targetKeyFilePath);
                ChannelExec channelExec = clientSession.createExecChannel("%s bash".formatted(env), Charset.defaultCharset(), null, null);
                channelExec.setIn(RunProcessWorkerVerticle.class.getClassLoader().getResourceAsStream("bin/main.sh"));
                channelExec.setOut(new SSEOutputStream(this::publishLog));
                channelExec.setRedirectErrorStream(true);
                channelExec.open().verify(5, TimeUnit.SECONDS);
                channelExec.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), TimeUnit.MINUTES.toMillis(30));
                channelExec.close();
                clientSession.close();
                log.info("Succeeded to handle script runner request {} (processId={})", runRequest, id);
                publishLog("process ended for %s with status %s".formatted(runRequest, channelExec.getExitStatus()));
            } catch (Exception e) {
                log.error("Failed to handle script runner request {} (processId={})", runRequest, id, e);
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                publishLog("process failed for %s with err: ".formatted(runRequest));
                for (String s : sw.toString().split("\n")) {
                    publishLog(s);
                }
            }
        }).completionHandler(ar -> {
            if (ar.succeeded()) {
                log.info("Succeeded to deploy RunProcessWorkerVerticle");
                startPromise.complete();
            } else {
                log.error("Failed to deploy RunProcessWorkerVerticle", ar.cause());
                startPromise.fail(ar.cause());
            }
        });
    }

    private Void publishLog(String message) {
        getVertx().eventBus().publish(SSESubscriptionHandler.LOG_SUBSCRIPTION_ADDRESS, message);
        return null;
    }
}
