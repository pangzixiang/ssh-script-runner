package io.github.pangzixiang.ssh.script.runner.handler;

import io.github.pangzixiang.ssh.script.runner.pojo.RestResponse;
import io.github.pangzixiang.ssh.script.runner.service.JWTService;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.ext.web.RoutingContext;

public class AuthenticationHandler implements Handler<RoutingContext> {
    private static AuthenticationHandler instance;
    public static synchronized AuthenticationHandler create(Vertx vertx) {
        if (instance == null) {
            instance = new AuthenticationHandler(vertx);
        }
        return instance;
    }
    private final JWTService jwtService;
    private AuthenticationHandler(Vertx vertx) {
        this.jwtService = JWTService.getInstance(vertx);
    }
    @Override
    public void handle(RoutingContext routingContext) {
        Cookie cookie = routingContext.request().getCookie("sshsr_token");
        if (cookie != null) {
            String token = cookie.getValue();
            jwtService.authenticate(token)
                    .onSuccess(unused -> routingContext.next())
                    .onFailure(throwable -> RestResponse.builder()
                            .message(throwable.getMessage())
                            .build()
                            .respond(routingContext, HttpResponseStatus.UNAUTHORIZED.code()));
        } else {
            RestResponse.builder()
                    .message("token not found")
                    .build()
                    .respond(routingContext, HttpResponseStatus.UNAUTHORIZED.code());
        }
    }
}
