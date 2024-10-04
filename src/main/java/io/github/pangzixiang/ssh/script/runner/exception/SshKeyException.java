package io.github.pangzixiang.ssh.script.runner.exception;

public class SshKeyException extends RuntimeException {
    public SshKeyException(String message) {
        super(message);
    }

    public SshKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    public SshKeyException(Throwable cause) {
        super(cause);
    }
}
