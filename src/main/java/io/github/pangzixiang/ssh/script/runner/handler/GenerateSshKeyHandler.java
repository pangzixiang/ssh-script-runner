package io.github.pangzixiang.ssh.script.runner.handler;

import io.github.pangzixiang.ssh.script.runner.pojo.RestResponse;
import io.github.pangzixiang.ssh.script.runner.service.SshKeyService;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class GenerateSshKeyHandler implements Handler<RoutingContext> {

    private static GenerateSshKeyHandler instance;
    private final SshKeyService sshKeyService;

    private GenerateSshKeyHandler() {
        this.sshKeyService = SshKeyService.getInstance();
    }

    public static synchronized GenerateSshKeyHandler create() {
        if (instance == null) {
            instance = new GenerateSshKeyHandler();
        }
        return instance;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        this.sshKeyService.generateKeyPair(routingContext.pathParam("name"));
        RestResponse.builder().message("succeeded to generate new key")
                .build()
                .respond(routingContext, HttpResponseStatus.OK.code());
    }
}
