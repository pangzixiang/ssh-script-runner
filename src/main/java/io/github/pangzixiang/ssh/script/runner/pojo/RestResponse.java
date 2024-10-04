package io.github.pangzixiang.ssh.script.runner.pojo;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RestResponse<T> {
    private T data;
    private String message;
    public Future<Void> respond(RoutingContext routingContext, MultiMap headers, int statusCode) {
        HttpServerResponse httpServerResponse = routingContext.response()
                .setStatusCode(statusCode);
        headers.forEach(httpServerResponse::putHeader);
        return httpServerResponse.end(Json.encode(this));
    }
    public Future<Void> respond(RoutingContext routingContext, int statusCode) {
        return respond(routingContext, MultiMap.caseInsensitiveMultiMap(), statusCode);
    }
}
