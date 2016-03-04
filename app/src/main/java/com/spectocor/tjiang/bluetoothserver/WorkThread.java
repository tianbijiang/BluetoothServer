package com.spectocor.tjiang.bluetoothserver;

import android.util.Log;

import com.spectocor.tjiang.bluetoothserver.rawsignals.SampleData;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class WorkThread implements Runnable {

    public Thread thread;
    private boolean suspended;
    private boolean stopped;
    private final Object monitor = new Object();
    private MainActivity.MyHandler mHandler;

    public static Short current = null;
    public static final int MSG_COUNTER = 1;
    private static short[] results;
    public static int PACKET_SIZE = 5;
    public static int SAMPLE_RATE = 360;
    public static String DATA_TYPE = "SampleData";
    public static int packetLen;

    public static ConcurrentLinkedQueue<Short> queue = new ConcurrentLinkedQueue<>();

    WorkThread(MainActivity.MyHandler handler) {
        mHandler = handler;
        thread = new Thread(this, "WorkingThread");
        suspended = false;
        stopped = true;
    }

    public static void log(String str) {
        Log.d("WorkingThread", str);
    }

    @Override
    public void run() {
        log("running...");
        stopped = false;

        readSampleData(DATA_TYPE);

        int count = 0;
        packetLen = SAMPLE_RATE * PACKET_SIZE;
        try {
            while (true) {
                synchronized (monitor) {
                    while (suspended) {
                        log("suspended");
                        monitor.wait();
                    }
                    if (stopped) {
                        log("stopped");
                        break;
                    }
                }
                serverCount(count++);
                sleepSampleRate(SAMPLE_RATE);
            }
        } catch (InterruptedException exc) {
            log("interrupted");
        }
    }

    public boolean isRunning() {
        return !stopped;
    }

    public void stop() {
        synchronized (monitor) {
            stopped = true;
            suspended = false;
            monitor.notifyAll();
        }
    }

//    public boolean isSuspended() {
//        return suspended;
//    }
//
//    public void suspend() {
//        synchronized (monitor) {
//            suspended = true;
//            monitor.notifyAll();
//        }
//    }
//
//    public void resume() {
//        log("resuming");
//        synchronized (monitor) {
//            suspended = false;
//            monitor.notifyAll();
//        }
//    }

    private void serverCount(int i) throws InterruptedException {
        current = results[i % results.length];
        mHandler.sendMessage(mHandler.obtainMessage(MSG_COUNTER, current));

        queue.offer(current);
    }

    private void readSampleData(String type) {
        log("reading data...");

        switch (type) {
            case "SampleData": {
                results = new short[SampleData.data.length];
                // TODO: need a better way
                for (int k = 0; k < SampleData.data.length; k++) {
                    results[k] = (short) SampleData.data[k];
                }
                break;
            }
            case "NODATA": {
                results = new short[]{-15000};
                break;
            }
        }
    }

    public static void sleepSampleRate(int sRate) throws InterruptedException {
        TimeUnit.MICROSECONDS.sleep(1000000 / sRate);
    }

}
