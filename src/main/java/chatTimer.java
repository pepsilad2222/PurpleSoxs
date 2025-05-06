import java.util.Timer;
import java.util.TimerTask;

public class chatTimer {
    private Timer timer;
    private final int seconds;
    private final Runnable onTimeout;

    public chatTimer(int seconds, Runnable onTimeout) {
        this.seconds = seconds;
        this.onTimeout = onTimeout;
        this.timer = new Timer(true);
        reset();
    }

    public synchronized void reset() {
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                onTimeout.run();
            }
        }, seconds * 1000L);
    }

    public void cancel() {
        if (timer != null) {
            timer.cancel();
        }
    }
}
