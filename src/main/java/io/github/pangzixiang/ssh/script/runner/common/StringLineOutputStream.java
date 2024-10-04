package io.github.pangzixiang.ssh.script.runner.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

public abstract class StringLineOutputStream extends OutputStream {
    private static final class ByteArrayOutputStreamX extends ByteArrayOutputStream {
        private ByteArrayOutputStreamX(final int size) {
            super(size);
        }
    }

    /** Initial buffer size. */
    private static final int INTIAL_SIZE = 132;

    /** Carriage return. */
    private static final int CR = 0x0d;

    /** Line-feed. */
    private static final int LF = 0x0a;

    /** The internal buffer. */
    private final ByteArrayOutputStreamX buffer = new ByteArrayOutputStreamX(INTIAL_SIZE);

    private boolean skip;

    private final Charset charset;

    /**
     * Creates a new instance of this class. Uses the default level of 999.
     */
    public StringLineOutputStream() {
        this(null);
    }

    /**
     * Creates a new instance of this class, specifying the character set that should be used for outputting the string for each line
     *
     * @param charset Character Set to use when processing lines.
     */
    public StringLineOutputStream(final Charset charset) {
        this.charset = charset == null ? Charset.defaultCharset() : charset;
    }

    /**
     * Writes all remaining data from the buffer.
     *
     * @see OutputStream#close()
     */
    @Override
    public void close() throws IOException {
        if (buffer.size() > 0) {
            processBuffer();
        }
        super.close();
    }

    /**
     * Flushes this log stream.
     *
     * @see OutputStream#flush()
     */
    @Override
    public void flush() {
        if (buffer.size() > 0) {
            processBuffer();
        }
    }

    /**
     * Converts the buffer to a string and sends it to {@code processLine}.
     */
    protected void processBuffer() {
        processLine(buffer.toString(charset));
        buffer.reset();
    }

    /**
     * Logs a line to the log system of the user.
     *
     * @param line     the line to log.
     */
    protected abstract void processLine(final String line);

    /**
     * Writes a block of characters to the output stream.
     *
     * @param b   the array containing the data.
     * @param off the offset into the array where data starts.
     * @param len the length of block.
     * @throws IOException if the data cannot be written into the stream.
     * @see OutputStream#write(byte[], int, int)
     */
    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        // find the line breaks and pass other chars through in blocks
        int offset = off;
        int blockStartOffset = offset;
        int remaining = len;
        while (remaining > 0) {
            while (remaining > 0 && b[offset] != LF && b[offset] != CR) {
                offset++;
                remaining--;
            }
            // either end of buffer or a line separator char
            final int blockLength = offset - blockStartOffset;
            if (blockLength > 0) {
                buffer.write(b, blockStartOffset, blockLength);
            }
            while (remaining > 0 && (b[offset] == LF || b[offset] == CR)) {
                write(b[offset]);
                offset++;
                remaining--;
            }
            blockStartOffset = offset;
        }
    }

    /**
     * Writes the data to the buffer and flush the buffer, if a line separator is detected.
     *
     * @param cc data to log (byte).
     * @see OutputStream#write(int)
     */
    @Override
    public void write(final int cc) throws IOException {
        final byte c = (byte) cc;
        if (c == '\n' || c == '\r') {
            if (!skip) {
                processBuffer();
            }
        } else {
            buffer.write(cc);
        }
        skip = c == '\r';
    }
}
