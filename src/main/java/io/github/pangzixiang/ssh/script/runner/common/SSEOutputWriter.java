package io.github.pangzixiang.ssh.script.runner.common;

import java.io.IOException;
import java.io.Writer;
import java.util.function.Function;

public class SSEOutputWriter extends Writer {
    private final Function<String, Void> handleString;
    public SSEOutputWriter(Function<String, Void> handleString) {
        this.handleString = handleString;
    }
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        String content = new String(cbuf).trim().replace("\n", "");
        handleString.apply("[console] >> %s".formatted(content));
    }

    @Override
    public void flush() throws IOException {

    }

    @Override
    public void close() throws IOException {

    }
}
