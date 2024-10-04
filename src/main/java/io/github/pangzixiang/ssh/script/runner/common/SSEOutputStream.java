package io.github.pangzixiang.ssh.script.runner.common;

import java.util.function.Function;

public class SSEOutputStream extends StringLineOutputStream {
    private final Function<String, Void> handleString;
    public SSEOutputStream(Function<String, Void> handleString) {
        this.handleString = handleString;
    }
    @Override
    protected void processLine(String line) {
        handleString.apply("sshsr $ %s".formatted(line));
    }
}
