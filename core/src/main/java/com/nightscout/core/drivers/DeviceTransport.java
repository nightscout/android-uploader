package com.nightscout.core.drivers;

import java.io.IOException;

public interface DeviceTransport {
    /**
     * Opens and initializes the device as a USB serial device. Upon success,
     * caller must ensure that {@link #close()} is eventually called.
     *
     * @throws java.io.IOException on error opening or initializing the device.
     */
    void open() throws IOException;

    /**
     * Closes the serial device.
     *
     * @throws java.io.IOException on error closing the device.
     */
    void close() throws IOException;

    /**
     * Reads as many bytes as possible into the destination buffer.
     *
     * @param size          size to read
     * @param timeoutMillis the timeout for reading
     * @return the actual number of bytes read
     * @throws java.io.IOException if an error occurred during reading
     */
    byte[] read(int size, final int timeoutMillis) throws IOException;

    /**
     * Writes as many bytes as possible from the source buffer.
     *
     * @param src           the source byte buffer
     * @param timeoutMillis the timeout for writing
     * @return the actual number of bytes written
     * @throws java.io.IOException if an error occurred during writing
     */
    int write(final byte[] src, final int timeoutMillis) throws IOException;

    boolean isConnected();
}
