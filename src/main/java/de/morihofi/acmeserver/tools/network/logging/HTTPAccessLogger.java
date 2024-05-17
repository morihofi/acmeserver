package de.morihofi.acmeserver.tools.network.logging;

import de.morihofi.acmeserver.config.Config;
import io.javalin.http.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

public class HTTPAccessLogger {
    private static final String LOG_FORMAT = "%s - %s [%s] \"%s\" %d %d \"%s\" \"%s\"";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z");
    private static final SimpleDateFormat FILENAME_FORMAT = new SimpleDateFormat("yyyy_MM_dd");

    private final BlockingQueue<String> logQueue = new LinkedBlockingQueue<>();
    private final Thread logWriterThread;
    private volatile boolean running = true;
    private final Path logFileDirectory;

    private final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());

    public HTTPAccessLogger(Config appConfig) throws IOException {

        String loggingDirectory = appConfig.getServer().getLoggingDirectory();

       if(loggingDirectory != null){
           // Configure paths
           logFileDirectory = Paths.get(loggingDirectory);

           if(!Files.exists(logFileDirectory) && !Files.isDirectory(logFileDirectory)){
               LOG.info("HTTP Access Log directory does not exist, creating it for you");
               Files.createDirectories(logFileDirectory);
           }

           logWriterThread = new Thread(this::writeLogs);
           logWriterThread.setName("HTTP Access Background Logger");
           logWriterThread.start();

           // Add shutdown hook
           Runtime.getRuntime().addShutdownHook(new Thread(() -> {
               LOG.info("Shutting down HTTP Access Logger ...");
               this.stop();
           }));
       }else {
           LOG.info("HTTP Access Logger deactivated, because no logging directory was set");
           logFileDirectory = null;
           logWriterThread = new Thread(() -> {
               throw new IllegalArgumentException("HTTP Log writing is deactivated");
           });

       }
    }

    public void log(String remoteAddr, String remoteUser, String request, int status, int bodyBytesSent, String httpReferer, String httpUserAgent) {
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
        LOG.info("HTTP Access Background Logger Thread started");
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
                    LOG.error("Error writing to HTTP access log", e);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                LOG.error("Error creating HTTP access logfile", e);
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

        String logEntry = String.format(LOG_FORMAT, remoteAddr, remoteUser, timeLocal, request, status, bodyBytesSent, httpReferer, httpUserAgent);
        logQueue.offer(logEntry);
    }
}
