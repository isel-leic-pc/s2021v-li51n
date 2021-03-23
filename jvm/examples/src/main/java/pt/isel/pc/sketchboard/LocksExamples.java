package pt.isel.pc.sketchboard;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LocksExamples {

    public static class UsingTheInstanceLock {
        // The data structure
        private int counter = 0;

        public synchronized void inc() {
            counter += 1;
        }

        public synchronized int getValue() {
            return counter;
        }

        public void dec() {
            synchronized (this) {
                counter -= 1;
            }
        }
    }

    public static class UsingAPrivateLock {

        private final Object theLock = new Object();
        private int counter = 0;

        public void inc() {
            synchronized (theLock) {
                counter += 1;
            }
        }

        public int getValue() {
            synchronized (theLock) {
                return counter;
            }
        }

        public void dec() {
            synchronized (theLock) {
                counter -= 1;
            }
        }

    }

    public static class UsingAPrivateExplicitLock {

        private final Lock theLock = new ReentrantLock();
        private int counter = 0;

        public void inc() {
            theLock.lock();
            try {
                counter += 1;
            } finally {
                theLock.unlock();
            }
        }

        public int getValue() {
            theLock.lock();
            try {
                return counter;
            } finally {
                theLock.unlock();
            }
        }

        public void dec() {
            theLock.lock();
            try {
                counter -= 1;
            } finally {
                theLock.unlock();
            }
        }

    }
}
