package io.github.pangzixiang.ssh.script.runner;

import io.github.pangzixiang.ssh.script.runner.config.AppConfiguration;
import io.github.pangzixiang.ssh.script.runner.handler.FailureRouterHandler;
import io.github.pangzixiang.ssh.script.runner.handler.PostSshKeyHandler;
import io.github.pangzixiang.ssh.script.runner.handler.SSESubscriptionHandler;
import io.github.pangzixiang.ssh.script.runner.handler.TriggerScriptRunHandler;
import io.github.pangzixiang.ssh.script.runner.pojo.TriggerRunRequest;
import io.github.pangzixiang.ssh.script.runner.pojo.TriggerRunRequestCodec;
import io.github.pangzixiang.ssh.script.runner.verticle.RunProcessWorkerVerticle;
import io.vertx.core.*;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;

@Slf4j
public class SshScriptRunner extends AbstractVerticle {
    private final AppConfiguration appConfiguration = AppConfiguration.getInstance();

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        log.info("Starting ssh-script-runner");
        Instant startTime = Instant.now();
        int port = appConfiguration.getInt("app.port", 0);
        Router mainRouter = Router.router(getVertx());
        mainRouter.route().handler(BodyHandler.create()
                .setDeleteUploadedFilesOnEnd(true)
                .setUploadsDirectory(appConfiguration.getString("app.dir") + "/tmp"));

        Router router = Router.router(getVertx());
        router.post("/ssh-key").handler(PostSshKeyHandler.create());
        router.get("/sse-subscription").handler(SSESubscriptionHandler.create());
        router.post("/run").handler(TriggerScriptRunHandler.create());

        mainRouter.route("/ssh-script-runner/*").subRouter(router);
        mainRouter.route().failureHandler(FailureRouterHandler.create());

        getVertx().eventBus().registerDefaultCodec(TriggerRunRequest.class, new TriggerRunRequestCodec());

        Future<String> deployRunProcessWorkerVerticleFuture = getVertx().deployVerticle(new RunProcessWorkerVerticle(), new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER));
        Future<HttpServer> createHttpServerFuture = getVertx().createHttpServer().requestHandler(mainRouter).listen(port);

        Future.all(deployRunProcessWorkerVerticleFuture, createHttpServerFuture).onComplete(ar -> {
            if (ar.succeeded()) {
                Duration duration = Duration.between(startTime, Instant.now());
                log.info("ssh-script-runner started successfully at port {} in {} ms", createHttpServerFuture.result().actualPort(), duration.toMillis());
                startPromise.complete();
            } else {
                startPromise.fail(ar.cause());
            }
        });
    }

    public static void main(String[] args) {
        AppConfiguration appConfiguration = AppConfiguration.getInstance();
        VertxOptions vertxOptions = new VertxOptions();
        vertxOptions.setEventLoopPoolSize(appConfiguration.getInt("app.event-loop-pool-size"));
        vertxOptions.setWorkerPoolSize(appConfiguration.getInt("app.worker-pool-size"));
        Vertx vertx = Vertx.vertx(vertxOptions);
        vertx.deployVerticle(new SshScriptRunner())
                .onFailure(throwable -> {
                    log.error("ssh-script-runner started failed", throwable);
                    System.exit(1);
                });
    }
}
