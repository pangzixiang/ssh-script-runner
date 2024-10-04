package io.github.pangzixiang.ssh.script.runner.handler;

import io.github.pangzixiang.ssh.script.runner.pojo.RestResponse;
import io.github.pangzixiang.ssh.script.runner.verticle.RunProcessWorkerVerticle;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class QueueLockHandler implements Handler<RoutingContext> {
    private static QueueLockHandler instance;

    private QueueLockHandler() {
    }

    public static synchronized QueueLockHandler create() {
        if (instance == null) {
            instance = new QueueLockHandler();
        }
        return instance;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        boolean isLock = Boolean.parseBoolean(routingContext.queryParam("isLock").getFirst());
        if (RunProcessWorkerVerticle.setLocked(isLock)) {
            RestResponse.builder().message("succeeded to %s queue".formatted(isLock ? "lock" : "unlock")).build().respond(routingContext, HttpResponseStatus.OK.code());
        } else {
            RestResponse.builder().message("failed to %s queue".formatted(isLock ? "lock" : "unlock")).build().respond(routingContext, HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
        }
    }
}
