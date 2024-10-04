package io.github.pangzixiang.ssh.script.runner.handler;

import io.github.pangzixiang.ssh.script.runner.pojo.RestResponse;
import io.github.pangzixiang.ssh.script.runner.service.SshKeyService;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PostSshKeyHandler implements Handler<RoutingContext> {

    private static PostSshKeyHandler instance;
    private final SshKeyService sshKeyService;

    private PostSshKeyHandler() {
        this.sshKeyService = SshKeyService.getInstance();
    }

    public static synchronized PostSshKeyHandler create() {
        if (instance == null) {
            instance = new PostSshKeyHandler();
        }
        return instance;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        List<FileUpload> fileUploads = routingContext.fileUploads();
        List<String> errors = new ArrayList<>();
        for (FileUpload fileUpload : fileUploads) {
            try {
                FileUtils.copyFile(new File(fileUpload.uploadedFileName()), new File(sshKeyService.getKeyDir(), fileUpload.fileName()));
            } catch (Exception e) {
                errors.add("skip process file %s as error occurred: %s".formatted(fileUpload.fileName(), e.getMessage()));
            }
        }
        sshKeyService.loadKeyPairsFromDisk();
        RestResponse.builder().message(errors.isEmpty() ? "Succeeded to add keys" : StringUtils.join(errors, ".\n"))
                .build()
                .respond(routingContext, errors.isEmpty() ? HttpResponseStatus.ACCEPTED.code() : HttpResponseStatus.NOT_ACCEPTABLE.code());
    }
}
