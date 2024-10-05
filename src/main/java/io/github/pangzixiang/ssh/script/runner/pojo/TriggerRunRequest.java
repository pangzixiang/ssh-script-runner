package io.github.pangzixiang.ssh.script.runner.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class TriggerRunRequest {
    @NonNull
    private String gitSshUrl;
    @Builder.Default
    private String branch = "master";
    @NonNull
    private Server targetServer;
    private Server jumpServer;

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    @ToString
    public static class Server {
        @NonNull
        private String host;
        @Builder.Default
        private int port = 22;
        @NonNull
        private String username;
    }
}
