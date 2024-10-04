package io.github.pangzixiang.ssh.script.runner.handler;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class SSESubscriptionHandler implements Handler<RoutingContext> {

    private static SSESubscriptionHandler instance;

    public static final String LOG_SUBSCRIPTION_ADDRESS = UUID.randomUUID().toString();

    public static SSESubscriptionHandler create() {
        if (instance == null) {
            instance = new SSESubscriptionHandler();
        }
        return instance;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        HttpServerResponse httpResponse = routingContext.response();
        httpResponse.putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_EVENT_STREAM);
        httpResponse.putHeader(HttpHeaderNames.ACCEPT_CHARSET, StandardCharsets.UTF_8.name());
        httpResponse.putHeader(HttpHeaderNames.CACHE_CONTROL, HttpHeaderValues.NO_CACHE);
        httpResponse.putHeader(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        httpResponse.setChunked(true);

        httpResponse.write("event: notification\n");
        httpResponse.write("data: SSE connection established successfully\n\n");
        routingContext.vertx().eventBus().consumer(LOG_SUBSCRIPTION_ADDRESS).handler(msg -> {
            String content = (String) msg.body();
            httpResponse.write("event: logging\n");
            httpResponse.write("data: %s\n\n".formatted(content));
        });
    }
}
