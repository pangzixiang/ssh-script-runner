package io.github.pangzixiang.ssh.script.runner.handler;

import io.github.pangzixiang.ssh.script.runner.pojo.KeyFile;
import io.github.pangzixiang.ssh.script.runner.pojo.RestResponse;
import io.github.pangzixiang.ssh.script.runner.service.SshKeyService;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

public class GetSshKeyHandler implements Handler<RoutingContext> {

    private static GetSshKeyHandler instance;
    private final SshKeyService sshKeyService;

    private GetSshKeyHandler() {
        this.sshKeyService = SshKeyService.getInstance();
    }

    public static synchronized GetSshKeyHandler create() {
        if (instance == null) {
            instance = new GetSshKeyHandler();
        }
        return instance;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        List<KeyFile> keyFiles = sshKeyService.getAllKeyFiles();
        RestResponse.builder().message("succeeded to get all keys")
                .data(keyFiles)
                .build()
                .respond(routingContext, HttpResponseStatus.OK.code());
    }
}
