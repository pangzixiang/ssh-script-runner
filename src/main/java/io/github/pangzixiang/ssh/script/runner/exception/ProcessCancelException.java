package io.github.pangzixiang.ssh.script.runner.exception;

public class ProcessCancelException extends RuntimeException {
    public ProcessCancelException(String message) {
        super(message);
    }

    public ProcessCancelException(String message, Throwable cause) {
        super(message, cause);
    }
}
