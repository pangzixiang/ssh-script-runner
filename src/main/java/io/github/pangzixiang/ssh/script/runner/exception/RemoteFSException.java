package io.github.pangzixiang.ssh.script.runner.exception;

public class RemoteFSException extends RuntimeException {
    public RemoteFSException(String message) {
        super(message);
    }

    public RemoteFSException(String message, Throwable cause) {
        super(message, cause);
    }
}
