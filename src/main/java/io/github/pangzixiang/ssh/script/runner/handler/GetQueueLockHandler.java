package io.github.pangzixiang.ssh.script.runner.handler;

import io.github.pangzixiang.ssh.script.runner.pojo.RestResponse;
import io.github.pangzixiang.ssh.script.runner.verticle.RunProcessWorkerVerticle;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class GetQueueLockHandler implements Handler<RoutingContext> {
    private static GetQueueLockHandler instance;

    private GetQueueLockHandler() {
    }

    public static synchronized GetQueueLockHandler create() {
        if (instance == null) {
            instance = new GetQueueLockHandler();
        }
        return instance;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        RestResponse.builder()
                .message("succeeded to get status of queue lock")
                .data(RunProcessWorkerVerticle.isLocked())
                .build()
                .respond(routingContext, HttpResponseStatus.OK.code());
    }
}
