package io.github.pangzixiang.ssh.script.runner.exception;

public class AppInitializeException extends RuntimeException {
    public AppInitializeException(String message) {
        super(message);
    }
    public AppInitializeException(String message, Throwable cause) {
        super(message, cause);
    }
}
