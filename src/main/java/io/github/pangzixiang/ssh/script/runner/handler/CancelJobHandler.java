package io.github.pangzixiang.ssh.script.runner.handler;

import io.github.pangzixiang.ssh.script.runner.pojo.RestResponse;
import io.github.pangzixiang.ssh.script.runner.verticle.RunProcessWorkerVerticle;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class CancelJobHandler implements Handler<RoutingContext> {
    private static CancelJobHandler instance;

    private CancelJobHandler() {
    }

    public static synchronized CancelJobHandler create() {
        if (instance == null) {
            instance = new CancelJobHandler();
        }
        return instance;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        RunProcessWorkerVerticle.cancelJob();
        RestResponse.builder().message("cancel request accepted").build().respond(routingContext, HttpResponseStatus.ACCEPTED.code());
    }
}
