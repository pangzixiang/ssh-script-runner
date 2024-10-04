package io.github.pangzixiang.ssh.script.runner.service;

import io.github.pangzixiang.ssh.script.runner.config.AppConfiguration;
import io.github.pangzixiang.ssh.script.runner.exception.AppInitializeException;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;

import java.io.InputStream;
import java.util.Objects;

public class JWTService {
    private static JWTService instance;
    public final JWTAuth jwtAuth;
    private final AppConfiguration appConfiguration = AppConfiguration.getInstance();
    private static final String AUDIENCE = "ssh-script-runner-user";
    private static final String SUBJECT = "ssh-script-runner-jwt-token";
    private static final String ISSUER = "ssh-script-runner";
    private JWTService(Vertx vertx) {

        try (InputStream privateKeyStream = JWTService.class.getClassLoader().getResourceAsStream("jwt/private_key.pem");
             InputStream publicKeyStream = JWTService.class.getClassLoader().getResourceAsStream("jwt/public.pem")
        ){
            JWTAuthOptions jwtAuthOptions = new JWTAuthOptions();
            jwtAuthOptions.addPubSecKey(new PubSecKeyOptions()
                    .setAlgorithm("RS256")
                    .setBuffer(new String(Objects.requireNonNull(privateKeyStream).readAllBytes())));
            jwtAuthOptions.addPubSecKey(new PubSecKeyOptions()
                    .setAlgorithm("RS256")
                    .setBuffer(new String(Objects.requireNonNull(publicKeyStream).readAllBytes())));
            this.jwtAuth = JWTAuth.create(vertx, jwtAuthOptions);
        } catch (Exception e) {
            throw new AppInitializeException("Failed to load jwt key", e);
        }
    }
    public static synchronized JWTService getInstance(Vertx vertx) {
        if (instance == null) {
            instance = new JWTService(vertx);
        }
        return instance;
    }

    public String generateToken() {
        return jwtAuth.generateToken(new JsonObject(), new JWTOptions()
                .setAlgorithm("RS256")
                .setExpiresInMinutes(appConfiguration.getInt("jwt.expire-time"))
                .addAudience(AUDIENCE)
                .setSubject(SUBJECT)
                .setIssuer(ISSUER)
        );
    }

    public Future<Void> authenticate(String token) {
        JsonObject credentials = new JsonObject()
                .put("token", token)
                .put("options", new JsonObject()
                        .put("audience", new JsonArray().add(ISSUER))
                        .put("subject", SUBJECT))
                        .put("issuer", ISSUER);
        TokenCredentials tokenCredentials = new TokenCredentials(credentials);
        return jwtAuth.authenticate(tokenCredentials).mapEmpty();
    }

}
