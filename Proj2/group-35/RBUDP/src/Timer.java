import java.util.GregorianCalendar;


public class Timer
{
    private double timeGone;
    private final double startMseconds;
    private double currentMseconds;
    private final double timeoutMseconds;
    
    Timer(final int n) {
        final GregorianCalendar gregorianCalendar = new GregorianCalendar();
        this.startMseconds = gregorianCalendar.get(14) + gregorianCalendar.get(13) * 1000 + gregorianCalendar.get(12) * 60000 + gregorianCalendar.get(11) * 3600000;
        this.timeoutMseconds = n;
    }
    
    double getTimeElapsed() {
        final GregorianCalendar gregorianCalendar = new GregorianCalendar();
        this.currentMseconds = gregorianCalendar.get(14) + gregorianCalendar.get(13) * 1000.0 + gregorianCalendar.get(12) * 60000.0 + gregorianCalendar.get(11) * 3600000.0;
        return this.timeGone = this.currentMseconds - this.startMseconds;
    }
    
    boolean timeout() {
        this.getTimeElapsed();
        return this.timeGone >= this.timeoutMseconds;
    }
}
