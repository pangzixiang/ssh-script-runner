package io.github.pangzixiang.ssh.script.runner.pojo;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.Json;

public class TriggerRunRequestCodec implements MessageCodec<TriggerRunRequest, TriggerRunRequest> {
    @Override
    public void encodeToWire(Buffer buffer, TriggerRunRequest triggerRunRequest) {
        buffer.appendBuffer(Json.encodeToBuffer(triggerRunRequest));
    }

    @Override
    public TriggerRunRequest decodeFromWire(int pos, Buffer buffer) {
        return buffer.toJsonObject().mapTo(TriggerRunRequest.class);
    }

    @Override
    public TriggerRunRequest transform(TriggerRunRequest triggerRunRequest) {
        return triggerRunRequest;
    }

    @Override
    public String name() {
        return TriggerRunRequestCodec.class.getName();
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }
}
