package atunstall.server.core.impl.logging;

import atunstall.server.core.api.Module;
import atunstall.server.core.api.logging.Level;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

@Module
public class RootLogger extends LoggerImpl {
    private final Object lock = new Object();
    private final Thread thread;
    private final BlockingDeque<LogMessageImpl> queue;
    private boolean stopped;

    public RootLogger() {
        super(null, "");
        root = this;
        queue = new LinkedBlockingDeque<>();
        thread = new Thread(() -> {
            while (!stopped || queue.size() > 0) {
                try {
                    /*synchronized (lock) {
                        while (queue.size() == 0) {
                            if (stopped) {
                                break;
                            }
                            lock.wait();
                        }
                    }*/
                    LogMessageImpl message = queue.takeFirst();
                    LoggerImpl current = message.getLogger();
                    while (current != null) {
                        current.handle(message);
                        current = current.getParent();
                    }
                } catch (InterruptedException ignored) {}
            }
            handle(new LogMessageImpl(this, Level.DEBUG, "Logging thread stopped"));
            synchronized (lock) {
                stopped = false;
                lock.notifyAll();
            }
        }, "Logging");
        thread.setDaemon(true);
        thread.start();
    }

    public void terminate() {
        synchronized (lock) {
            stopped = true;
            thread.interrupt();
            while (stopped) {
                try {
                    lock.wait();
                } catch (InterruptedException ignored) {}
            }
        }
    }

    void log(LogMessageImpl message) {
        queue.add(message);
    }
}
