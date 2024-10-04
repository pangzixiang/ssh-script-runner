package io.github.pangzixiang.ssh.script.runner.handler;

import io.github.pangzixiang.ssh.script.runner.exception.SshKeyException;
import io.github.pangzixiang.ssh.script.runner.pojo.RestResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class FailureRouterHandler implements Handler<RoutingContext> {

    private static FailureRouterHandler failureRouterHandler;

    public static synchronized FailureRouterHandler create() {
        if (failureRouterHandler == null) {
            failureRouterHandler = new FailureRouterHandler();
        }
        return failureRouterHandler;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        Throwable throwable = routingContext.failure();
        var response = RestResponse.builder()
                .message(throwable.getMessage())
                .build();
        int statusCode = HttpResponseStatus.INTERNAL_SERVER_ERROR.code();
        if (throwable instanceof IllegalArgumentException || throwable instanceof SshKeyException) {
            statusCode = HttpResponseStatus.BAD_REQUEST.code();
        }
        response.respond(routingContext, statusCode);
    }
}
