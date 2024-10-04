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
            routingContext.vertx().eventBus().publish(RunProcessWorkerVerticle.RUN_PROCESS_ADDRESS, runRequest);
            RestResponse.builder()
                    .message("succeeded to trigger")
                    .build()
                    .respond(routingContext, HttpResponseStatus.ACCEPTED.code());
        } else {
            throw new IllegalArgumentException("request body is null");
        }
    }
}
