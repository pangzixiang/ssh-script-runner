package io.github.pangzixiang.ssh.script.runner.handler;

import io.github.pangzixiang.ssh.script.runner.pojo.RestResponse;
import io.github.pangzixiang.ssh.script.runner.pojo.TriggerRunRequest;
import io.github.pangzixiang.ssh.script.runner.verticle.RunProcessWorkerVerticle;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class TriggerScriptRunHandler implements Handler<RoutingContext> {
    private static TriggerScriptRunHandler instance;
    public static TriggerScriptRunHandler create() {
        if (instance == null) {
            instance = new TriggerScriptRunHandler();
        }
        return instance;
    }
    @Override
    public void handle(RoutingContext routingContext) {
        TriggerRunRequest runRequest = routingContext.body().asPojo(TriggerRunRequest.class);
        if (runRequest != null) {
            RunProcessWorkerVerticle.addRunRequestToHistory(runRequest);
            if (RunProcessWorkerVerticle.addRunRequestToQueue(runRequest)) {
                RestResponse.builder()
                        .message("succeeded to add run request to queue")
                        .build()
                        .respond(routingContext, HttpResponseStatus.ACCEPTED.code());
            } else {
                RestResponse.builder()
                        .message("failed to add run request to queue")
                        .build()
                        .respond(routingContext, HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
            }
        } else {
            throw new IllegalArgumentException("request body is null");
        }
    }
}
