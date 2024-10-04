package io.github.pangzixiang.ssh.script.runner.handler;

import io.github.pangzixiang.ssh.script.runner.pojo.RestResponse;
import io.github.pangzixiang.ssh.script.runner.service.SshKeyService;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class DeleteSshKeyHandler implements Handler<RoutingContext> {

    private static DeleteSshKeyHandler instance;
    private final SshKeyService sshKeyService;

    private DeleteSshKeyHandler() {
        this.sshKeyService = SshKeyService.getInstance();
    }

    public static synchronized DeleteSshKeyHandler create() {
        if (instance == null) {
            instance = new DeleteSshKeyHandler();
        }
        return instance;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        sshKeyService.deleteKeyFile(routingContext.pathParam("name"));
        RestResponse.builder().message("succeeded to delete key")
                .build()
                .respond(routingContext, HttpResponseStatus.OK.code());
    }
}
