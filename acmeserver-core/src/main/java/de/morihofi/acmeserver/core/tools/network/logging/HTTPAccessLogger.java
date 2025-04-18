/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.core.tools.network.logging;

import de.morihofi.acmeserver.core.config.Config;
import io.javalin.http.Context;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class HTTPAccessLogger {
    private static final String LOG_FORMAT = "%s - %s [%s] \"%s\" %d %d \"%s\" \"%s\"";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z");
    private static final SimpleDateFormat FILENAME_FORMAT = new SimpleDateFormat("yyyy_MM_dd");

    private final BlockingQueue<String> logQueue = new LinkedBlockingQueue<>();
    private final Thread logWriterThread;
    private final Path logFileDirectory;

    private volatile boolean running = true;

    public HTTPAccessLogger(Config appConfig) throws IOException {

        String loggingDirectory = appConfig.getServer().getLoggingDirectory();

        if (loggingDirectory != null) {
            // Configure paths
            logFileDirectory = Paths.get(loggingDirectory);

            if (!Files.exists(logFileDirectory) && !Files.isDirectory(logFileDirectory)) {
                log.info("HTTP Access Log directory does not exist, creating it for you");
                Files.createDirectories(logFileDirectory);
            }

            logWriterThread = new Thread(this::writeLogs);
            logWriterThread.setName("HTTP Access Background Logger");
            logWriterThread.start();

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Shutting down HTTP Access Logger ...");
                this.stop();
            }));
        } else {
            log.info("HTTP Access Logger deactivated, because no logging directory was set");
            logFileDirectory = null;
            logWriterThread = new Thread(() -> {
                throw new IllegalArgumentException("HTTP Log writing is deactivated");
            });
        }
    }

    public void log(String remoteAddr, String remoteUser, String request, int status, int bodyBytesSent, String httpReferer,
            String httpUserAgent) {
        String timeLocal = DATE_FORMAT.format(new Date());
        String logEntry = String.format(
                LOG_FORMAT,
                remoteAddr,
                remoteUser == null ? "-" : remoteUser,
                timeLocal,
                request,
                status,
                bodyBytesSent,
                httpReferer == null ? "-" : httpReferer,
                httpUserAgent == null ? "-" : httpUserAgent
        );
        logQueue.offer(logEntry);
    }

    private void writeLogs() {
        log.info("HTTP Access Background Logger Thread started");
        while (running) {
            try {
                String logEntry = logQueue.take();
                String filename = "access_" + FILENAME_FORMAT.format(new Date()) + ".log";

                Path path = logFileDirectory.resolve(filename);

                // Ensure the file exists
                if (!Files.exists(path)) {
                    Files.createFile(path);
                }

                try (FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {

                    ByteBuffer buffer = ByteBuffer.wrap((logEntry + System.lineSeparator()).getBytes());
                    fileChannel.write(buffer);
                } catch (IOException e) {
                    log.error("Error writing to HTTP access log", e);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                log.error("Error creating HTTP access logfile", e);
            }
        }
    }

    public void stop() {
        running = false;
        logWriterThread.interrupt();
        try {
            logWriterThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void log(Context ctx) {
        String remoteAddr = ctx.ip();
        String remoteUser = ctx.basicAuthCredentials() != null ? ctx.basicAuthCredentials().getUsername() : "-";
        String timeLocal = DATE_FORMAT.format(new Date());
        String request = ctx.method() + " " + ctx.path() + " " + ctx.protocol();
        int status = ctx.statusCode();
        int bodyBytesSent = ctx.resultInputStream() != null ? ctx.resultInputStream().toString().length() : 0;
        String httpReferer = ctx.header("Referer") != null ? ctx.header("Referer") : "-";
        String httpUserAgent = ctx.userAgent() != null ? ctx.userAgent() : "-";

        String logEntry =
                String.format(LOG_FORMAT, remoteAddr, remoteUser, timeLocal, request, status, bodyBytesSent, httpReferer, httpUserAgent);
        logQueue.offer(logEntry);
    }
}
