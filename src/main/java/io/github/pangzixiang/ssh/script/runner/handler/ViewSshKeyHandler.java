package io.github.pangzixiang.ssh.script.runner.handler;

import io.github.pangzixiang.ssh.script.runner.pojo.RestResponse;
import io.github.pangzixiang.ssh.script.runner.service.SshKeyService;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class ViewSshKeyHandler implements Handler<RoutingContext> {

    private static ViewSshKeyHandler instance;
    private final SshKeyService sshKeyService;

    private ViewSshKeyHandler() {
        this.sshKeyService = SshKeyService.getInstance();
    }

    public static synchronized ViewSshKeyHandler create() {
        if (instance == null) {
            instance = new ViewSshKeyHandler();
        }
        return instance;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        String content = sshKeyService.viewKeyFile(routingContext.pathParam("name"));
        RestResponse.builder().message("succeeded to view key")
                .data(content)
                .build()
                .respond(routingContext, HttpResponseStatus.OK.code());
    }
}
