package io.github.pangzixiang.ssh.script.runner.verticle;

import io.github.pangzixiang.ssh.script.runner.common.SSEOutputStream;
import io.github.pangzixiang.ssh.script.runner.handler.SSESubscriptionHandler;
import io.github.pangzixiang.ssh.script.runner.pojo.TriggerRunRequest;
import io.github.pangzixiang.ssh.script.runner.service.SshKeyService;
import io.vertx.core.AbstractVerticle;
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
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class RunProcessWorkerVerticle extends AbstractVerticle {
    private final SshKeyService sshKeyService;
    private static final String ENV_TEMPLATE = "GIT_SSH_URL=%s GIT_BRANCH=%s MAIN_SCRIPT=%s PROCESS_ID=%s GIT_SSH_COMMAND='ssh -o StrictHostKeyChecking=no -i %s'";
    private static final String PROCESS_LOCK = RunProcessWorkerVerticle.class.getName();
    private static final AtomicBoolean locked = new AtomicBoolean(false);
    private static final Queue<TriggerRunRequest> runRequestQueue = new ConcurrentLinkedQueue<>();
    private static final List<TriggerRunRequest> runRequestHistory = new ArrayList<>();

    public static synchronized boolean addRunRequestToQueue(TriggerRunRequest runRequest) {
        return runRequestQueue.offer(runRequest);
    }

    public static synchronized boolean isLocked() {
        return locked.get();
    }

    public static synchronized boolean setLocked(boolean isLock) {
        return locked.compareAndSet(!isLock, isLock);
    }

    public static synchronized Queue<TriggerRunRequest> getRunRequestQueue() {
        return runRequestQueue;
    }

    public static synchronized List<TriggerRunRequest> getRunRequestHistory() {
        return runRequestHistory;
    }

    public static synchronized void addRunRequestToHistory(TriggerRunRequest runRequest) {
        if (runRequestHistory.size() > 10) {
            runRequestHistory.removeLast();
        }
        runRequestHistory.addFirst(runRequest);
    }

    public RunProcessWorkerVerticle() {
        this.sshKeyService = SshKeyService.getInstance();
    }

    @Override
    public void start() throws Exception {
        log.info("Starting to deploy RunProcessWorkerVerticle");
        getVertx().setPeriodic(0, TimeUnit.SECONDS.toMillis(5), l -> {
            if (locked.getAcquire()) {
                return;
            }
            getVertx().sharedData().getLocalLockWithTimeout(PROCESS_LOCK, TimeUnit.SECONDS.toMillis(3)).onSuccess(lock -> {
                File gitSshKey = new File(sshKeyService.getKeyDir(), "sshsr_git.pri");
                if (!gitSshKey.exists()) {
                    publishNotification("git ssh key not exists, please generate or upload and named with sshsr_git.pri.");
                    lock.release();
                    return;
                }
                TriggerRunRequest runRequest = getRunRequestQueue().poll();
                if (runRequest != null) {
                    String id = UUID.randomUUID().toString().replace("-", "");
                    log.info("Start to handle script runner request {} (processId={})", runRequest, id);
                    publishNotification("process started for %s".formatted(runRequest));
                    try (SshClient sshClient = SshClient.setUpDefaultClient()) {
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
                        publishLog("sshsr $ succeeded to connect to target %s".formatted(targetServer));

                        SftpFileSystem sftpFileSystem = SftpClientFactory.instance().createSftpFileSystem(clientSession);
                        Path targetDir = sftpFileSystem.getDefaultDir().resolve(".ssh");
                        Path targetKeyFilePath = targetDir.resolve("sshsr_git.pri");
                        Files.createDirectories(targetDir);
                        Files.copy(new FileInputStream(gitSshKey), targetKeyFilePath, StandardCopyOption.REPLACE_EXISTING);
                        Files.setPosixFilePermissions(targetKeyFilePath, Set.of(PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
                        String env = ENV_TEMPLATE.formatted(runRequest.getGitSshUrl(), runRequest.getBranch(), runRequest.getMainScript(), id, targetKeyFilePath);
                        ChannelExec channelExec = clientSession.createExecChannel("%s bash".formatted(env), Charset.defaultCharset(), null, null);
                        channelExec.setIn(RunProcessWorkerVerticle.class.getClassLoader().getResourceAsStream("bin/main.sh"));
                        channelExec.setOut(new SSEOutputStream(this::publishLog));
                        channelExec.setRedirectErrorStream(true);
                        channelExec.open().verify(5, TimeUnit.SECONDS);
                        channelExec.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), TimeUnit.MINUTES.toMillis(30));
                        channelExec.close();
                        clientSession.close();
                        log.info("Succeeded to handle script runner request {} (processId={})", runRequest, id);
                        publishNotification("process ended for %s with status %s".formatted(runRequest, channelExec.getExitStatus()));
                        publishLog("========== END ==========\n\n\n\n\n");
                    } catch (Exception e) {
                        log.error("Failed to handle script runner request {} (processId={})", runRequest, id, e);
                        StringWriter sw = new StringWriter();
                        e.printStackTrace(new PrintWriter(sw));
                        publishNotification("process failed for %s with err: ".formatted(runRequest));
                        for (String s : sw.toString().split("\n")) {
                            publishLog(s);
                        }
                    }
                }
                lock.release();
            }).onFailure(throwable -> log.debug("Failed to get lock", throwable));
        });
        log.info("Succeeded to deploy RunProcessWorkerVerticle");
    }

    private Void publishLog(String message) {
        getVertx().eventBus().publish(SSESubscriptionHandler.LOG_SUBSCRIPTION_ADDRESS, message);
        return null;
    }

    private Void publishNotification(String message) {
        getVertx().eventBus().publish(SSESubscriptionHandler.NOTIFICATION_SUBSCRIPTION_ADDRESS, message);
        return null;
    }
}
