package io.github.pangzixiang.ssh.script.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import io.github.pangzixiang.ssh.script.runner.config.AppConfiguration;
import io.github.pangzixiang.ssh.script.runner.handler.AuthenticationHandler;
import io.github.pangzixiang.ssh.script.runner.handler.AuthorizationHandler;
import io.github.pangzixiang.ssh.script.runner.handler.DeleteSshKeyHandler;
import io.github.pangzixiang.ssh.script.runner.handler.FailureRouterHandler;
import io.github.pangzixiang.ssh.script.runner.handler.GenerateSshKeyHandler;
import io.github.pangzixiang.ssh.script.runner.handler.GetQueueLockHandler;
import io.github.pangzixiang.ssh.script.runner.handler.GetRunProcessHistoryHandler;
import io.github.pangzixiang.ssh.script.runner.handler.GetSshKeyHandler;
import io.github.pangzixiang.ssh.script.runner.handler.PostSshKeyHandler;
import io.github.pangzixiang.ssh.script.runner.handler.QueueLockHandler;
import io.github.pangzixiang.ssh.script.runner.handler.SSESubscriptionHandler;
import io.github.pangzixiang.ssh.script.runner.handler.TriggerScriptRunHandler;
import io.github.pangzixiang.ssh.script.runner.handler.ViewSshKeyHandler;
import io.github.pangzixiang.ssh.script.runner.pojo.TriggerRunRequest;
import io.github.pangzixiang.ssh.script.runner.pojo.TriggerRunRequestCodec;
import io.github.pangzixiang.ssh.script.runner.verticle.RunProcessWorkerVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.ThreadingModel;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class SshScriptRunner extends AbstractVerticle {
    private final AppConfiguration appConfiguration = AppConfiguration.getInstance();

    static {
        ObjectMapper objectMapper = DatabindCodec.mapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        LocalDateTimeSerializer localDateTimeSerializer = new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        LocalDateTimeDeserializer localDateTimeDeserializer = new LocalDateTimeDeserializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        LocalDateSerializer localDateSerializer = new LocalDateSerializer(DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDateDeserializer localDateDeserializer = new LocalDateDeserializer(DateTimeFormatter.ISO_LOCAL_DATE);
        LocalTimeSerializer localTimeSerializer = new LocalTimeSerializer(DateTimeFormatter.ISO_LOCAL_TIME);
        LocalTimeDeserializer localTimeDeserializer = new LocalTimeDeserializer(DateTimeFormatter.ISO_LOCAL_TIME);
        javaTimeModule.addSerializer(LocalDateTime.class, localDateTimeSerializer);
        javaTimeModule.addSerializer(LocalDate.class, localDateSerializer);
        javaTimeModule.addSerializer(LocalTime.class, localTimeSerializer);
        javaTimeModule.addDeserializer(LocalDateTime.class, localDateTimeDeserializer);
        javaTimeModule.addDeserializer(LocalDate.class, localDateDeserializer);
        javaTimeModule.addDeserializer(LocalTime.class, localTimeDeserializer);

        objectMapper.registerModule(javaTimeModule);
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        log.info("Starting ssh-script-runner");
        Instant startTime = Instant.now();
        int port = appConfiguration.getInt("app.port", 0);
        Router mainRouter = Router.router(getVertx());
        mainRouter.route().handler(BodyHandler.create()
                .setDeleteUploadedFilesOnEnd(true)
                .setUploadsDirectory(FileUtils.getTempDirectoryPath() + "/sshsrtmp"));

        Router router = Router.router(getVertx());
        router.post("/api/ssh-key").handler(AuthenticationHandler.create(getVertx())).handler(PostSshKeyHandler.create());
        router.get("/api/ssh-key").handler(AuthenticationHandler.create(getVertx())).handler(GetSshKeyHandler.create());
        router.get("/api/ssh-key/:name").handler(AuthenticationHandler.create(getVertx())).handler(ViewSshKeyHandler.create());
        router.patch("/api/ssh-key/:name").handler(AuthenticationHandler.create(getVertx())).handler(GenerateSshKeyHandler.create());
        router.delete("/api/ssh-key/:name").handler(AuthenticationHandler.create(getVertx())).handler(DeleteSshKeyHandler.create());
        router.get("/api/sse-subscription").handler(AuthenticationHandler.create(getVertx())).handler(SSESubscriptionHandler.create());
        router.post("/api/run").handler(AuthenticationHandler.create(getVertx())).handler(TriggerScriptRunHandler.create());
        router.get("/api/run/history").handler(AuthenticationHandler.create(getVertx())).handler(GetRunProcessHistoryHandler.create());
        router.post("/api/issue-token").handler(AuthorizationHandler.create(getVertx()));
        router.patch("/api/queue-lock").handler(AuthenticationHandler.create(getVertx())).handler(QueueLockHandler.create());
        router.get("/api/queue-lock").handler(AuthenticationHandler.create(getVertx())).handler(GetQueueLockHandler.create());
        router.get("/assets/*").handler(StaticHandler.create("static/assets"));
        router.get().handler(routingContext -> routingContext.response().sendFile("static/index.html"));

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
