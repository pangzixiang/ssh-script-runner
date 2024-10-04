package io.github.pangzixiang.ssh.script.runner.handler;

import io.github.pangzixiang.ssh.script.runner.pojo.RestResponse;
import io.github.pangzixiang.ssh.script.runner.verticle.RunProcessWorkerVerticle;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class GetRunProcessHistoryHandler implements Handler<RoutingContext> {
    private GetRunProcessHistoryHandler() {
    }

    private static GetRunProcessHistoryHandler instance;

    public static GetRunProcessHistoryHandler create() {
        if (instance == null) {
            instance = new GetRunProcessHistoryHandler();
        }
        return instance;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        RestResponse.builder()
                .message("succeeded to get run process history")
                .data(RunProcessWorkerVerticle.getRunRequestHistory())
                .build().respond(routingContext, HttpResponseStatus.OK.code());
    }
}
