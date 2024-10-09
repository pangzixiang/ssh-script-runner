package io.github.pangzixiang.ssh.script.runner.verticle;

import io.github.pangzixiang.ssh.script.runner.common.GitEmptyCredentialsProvider;
import io.github.pangzixiang.ssh.script.runner.common.SSEOutputStream;
import io.github.pangzixiang.ssh.script.runner.common.SSEOutputWriter;
import io.github.pangzixiang.ssh.script.runner.exception.AppInitializeException;
import io.github.pangzixiang.ssh.script.runner.exception.RemoteFSException;
import io.github.pangzixiang.ssh.script.runner.handler.SSESubscriptionHandler;
import io.github.pangzixiang.ssh.script.runner.pojo.TriggerRunRequest;
import io.github.pangzixiang.ssh.script.runner.service.SshKeyService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.config.hosts.HostConfigEntry;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.KeyIdentityProvider;
import org.apache.sshd.git.transport.GitSshdSessionFactory;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.client.fs.SftpFileSystem;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.SshSessionFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RunProcessWorkerVerticle extends AbstractVerticle {
    public static final String RUN_PROCESS_ADDRESS = UUID.randomUUID().toString();
    private final SshKeyService sshKeyService;
    private static final CredentialsProvider CREDENTIALS_PROVIDER = new GitEmptyCredentialsProvider();
    private final File tempWorkingDir;
    private final SSEOutputWriter sseOutputWriter;
    private final SSEOutputStream sseOutputStream;
    public RunProcessWorkerVerticle() {
        this.sshKeyService = SshKeyService.getInstance();
        this.sseOutputWriter = new SSEOutputWriter(this::publishLog);
        this.sseOutputStream = new SSEOutputStream(this::publishLog);
        this.tempWorkingDir = new File(FileUtils.getTempDirectoryPath() + "/sshsrtmp");
        if (!this.tempWorkingDir.exists()) {
            try {
                FileUtils.forceMkdir(tempWorkingDir);
                FileUtils.forceDeleteOnExit(tempWorkingDir);
            } catch (Exception e){
                throw new AppInitializeException("Fail to create temp directory: " + tempWorkingDir, e);
            }
        }
    }
    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        log.info("Starting to deploy RunProcessWorkerVerticle");
        getVertx().eventBus().consumer(RUN_PROCESS_ADDRESS).handler(message -> {
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
                GitSshdSessionFactory sshdFactory = new GitSshdSessionFactory(sshClient);
                SshSessionFactory.setInstance(sshdFactory);

                File dir = new File(this.tempWorkingDir, id);

                publishLog("clone repository into %s".formatted(dir));
                Git.cloneRepository()
                        .setProgressMonitor(new TextProgressMonitor(this.sseOutputWriter))
                        .setBranch(runRequest.getBranch())
                        .setCloneAllBranches(false)
                        .setCredentialsProvider(CREDENTIALS_PROVIDER)
                        .setDirectory(dir)
                        .setNoTags()
                        .setURI(runRequest.getGitSshUrl())
                        .call().close();

                ClientSession clientSession = sshClient.connect("target")
                                .verify(5, TimeUnit.SECONDS).getClientSession();
                clientSession.auth().verify(5, TimeUnit.SECONDS);
                publishLog("succeeded to connect to target %s".formatted(targetServer));

                SftpFileSystem sftpFileSystem = SftpClientFactory.instance().createSftpFileSystem(clientSession);
                Path targetDir = sftpFileSystem.getPath("/tmp").resolve("sshsrtmp").resolve(id);
//                deleteDirFS(targetDir);
                Files.createDirectories(targetDir);
                copyDirFS(new File(dir, ".sshsr"), targetDir);
                ChannelExec channelExec = clientSession.createExecChannel("cd %s && bash main.sh".formatted(targetDir.toString()));
                channelExec.setOut(this.sseOutputStream);
                channelExec.setRedirectErrorStream(true);
                channelExec.open().verify(5, TimeUnit.SECONDS);
                channelExec.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), TimeUnit.MINUTES.toMillis(30));
                clientSession.close();
//                deleteDirFS(targetDir);
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

//    private void deleteDirFS(Path dir) {
//        if (Files.notExists(dir)) {
//            return;
//        }
//        try (Stream<Path> subs = Files.list(dir)) {
//            subs.forEach(sub -> {
//                if (Files.isDirectory(sub)) {
//                    deleteDirFS(sub);
//                } else {
//                    try {
//                        Files.deleteIfExists(sub);
//                    } catch (IOException e) {
//                        throw new RemoteFSException("Failed to delete file " + sub, e);
//                    }
//                }
//            });
//        } catch (Exception e) {
//            throw new RemoteFSException("Failed to list directory " + dir, e);
//        }
//    }

    private void copyDirFS(File source, Path target) {
        File[] files = source.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    try {
                        Files.createDirectory(target.resolve(file.getName()));
                    } catch (Exception e) {
                        throw new RemoteFSException("Failed to create directory " + target.resolve(file.getName()), e);
                    }
                    copyDirFS(file, target.resolve(file.getName()));
                } else {
                    try {
                        Files.copy(new FileInputStream(file), target.resolve(file.getName()), StandardCopyOption.REPLACE_EXISTING);
                    } catch (Exception e) {
                        throw new RemoteFSException("Failed to copy file " + file, e);
                    }
                }
            }
        }
    }
}
