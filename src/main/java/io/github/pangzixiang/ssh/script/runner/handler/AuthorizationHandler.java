package io.github.pangzixiang.ssh.script.runner.handler;

import io.github.pangzixiang.ssh.script.runner.config.AppConfiguration;
import io.github.pangzixiang.ssh.script.runner.exception.AppInitializeException;
import io.github.pangzixiang.ssh.script.runner.pojo.RestResponse;
import io.github.pangzixiang.ssh.script.runner.service.JWTService;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;
import org.jasypt.util.password.StrongPasswordEncryptor;

import java.util.List;
import java.util.Objects;
import java.util.Properties;

public class AuthorizationHandler implements Handler<RoutingContext> {
    private static AuthorizationHandler instance;
    private final JWTService jwtService;
    private final StrongPasswordEncryptor strongPasswordEncryptor;
    private final String user;
    private final String password;
    private final AppConfiguration appConfiguration = AppConfiguration.getInstance();

    private AuthorizationHandler(Vertx vertx) {
        this.jwtService = JWTService.getInstance(vertx);
        this.strongPasswordEncryptor = new StrongPasswordEncryptor();
        Properties properties = new Properties();
        try {
            properties.load(AuthorizationHandler.class.getClassLoader().getResourceAsStream("jwt/user.properties"));
            this.user = properties.getProperty("user");
            this.password = properties.getProperty("password");
            Objects.requireNonNull(this.user, "user property is required");
            Objects.requireNonNull(this.password, "password property is required");
        } catch (Exception e) {
            throw new AppInitializeException("failed to load jwt user properties", e);
        }
    }
    public static synchronized AuthorizationHandler create(Vertx vertx) {
        if (instance == null) {
            instance = new AuthorizationHandler(vertx);
        }
        return instance;
    }
    @Override
    public void handle(RoutingContext routingContext) {
        JsonObject body = routingContext.body().asJsonObject();
        if (body == null) {
            throw new IllegalArgumentException("request body is null");
        }
        String user = body.getString("user");
        String password = body.getString("password");
        if (StringUtils.isAnyBlank(user, password)) {
            throw new IllegalArgumentException("user or password are required");
        }
        if (user.equals(this.user) && strongPasswordEncryptor.checkPassword(password, this.password)) {
            String token = this.jwtService.generateToken();
            Cookie cookie = Cookie.cookie("sshsr_token", token)
                    .setHttpOnly(false)
                    .setMaxAge(appConfiguration.getInt("jwt.expire-time") * 60)
                    .setSameSite(CookieSameSite.STRICT)
                    .setDomain(routingContext.request().authority().host())
                    .setPath("/");
            RestResponse.builder()
                    .message("succeeded to generate token")
                    .data(token)
                    .build()
                    .respond(routingContext, List.of(cookie), HttpResponseStatus.OK.code());
        } else {
            RestResponse.builder()
                    .message("user or password is incorrect")
                    .build()
                    .respond(routingContext, HttpResponseStatus.UNAUTHORIZED.code());
        }
    }
}
