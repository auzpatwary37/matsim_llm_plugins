package chatcommons;


import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Thread-safe logger that appends IChatMessage entries as JSON lines
 * to a specified log file. Designed for use with multiple parallel agents.
 */
public class ChatLogger {

    private final String logFilePath;
    private final Object lock = new Object();

    public ChatLogger(String logFilePath) {
        this.logFilePath = logFilePath;
    }

    /**
     * Appends a single JSON-formatted line to the log file.
     * Thread-safe and flushes after each write.
     *
     * @param jsonLine a serialized JSON representation of IChatMessage
     */
    public void log(String jsonLine) {
        synchronized (lock) {
            try (FileWriter fw = new FileWriter(logFilePath, true);
                 PrintWriter pw = new PrintWriter(fw)) {
                pw.println(jsonLine);
            } catch (IOException e) {
                System.err.println("ChatLogger write failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}

