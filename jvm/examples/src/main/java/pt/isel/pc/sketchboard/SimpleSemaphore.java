package pt.isel.pc.sketchboard;

public class SimpleSemaphore {

    private int units;
    private final Object theMonitor = new Object();

    public SimpleSemaphore(int initialUnits) {
        units = initialUnits;
    }

    public void acquire() throws InterruptedException {
        synchronized (theMonitor) {
            if (units > 0) {
                units -= 1;
                return;
            }
            while(!(units > 0)) {
                theMonitor.wait(); // wait-set
            }
            // units > 0
            units -= 1;
        }
    }

    public void release() {
        synchronized (theMonitor) {
            units += 1;
            theMonitor.notify();
            //theMonitor.notifyAll();
        }
    }
}
