package io.github.pangzixiang.ssh.script.runner.pojo;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RestResponse<T> {
    private T data;
    private String message;
    public Future<Void> respond(RoutingContext routingContext, MultiMap headers, List<Cookie> cookies, int statusCode) {
        HttpServerResponse httpServerResponse = routingContext.response()
                .putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                .setStatusCode(statusCode);
        headers.forEach(httpServerResponse::putHeader);
        cookies.forEach(httpServerResponse::addCookie);
        return httpServerResponse.end(Json.encode(this));
    }
    public Future<Void> respond(RoutingContext routingContext, int statusCode) {
        return respond(routingContext, MultiMap.caseInsensitiveMultiMap(), Collections.emptyList(), statusCode);
    }
    public Future<Void> respond(RoutingContext routingContext, MultiMap headers, int statusCode) {
        return respond(routingContext, headers, Collections.emptyList(), statusCode);
    }
    public Future<Void> respond(RoutingContext routingContext, List<Cookie> cookies, int statusCode) {
        return respond(routingContext, MultiMap.caseInsensitiveMultiMap(), cookies, statusCode);
    }

}
