package com.spectocor.tjiang.bluetoothserver;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectOutputStream;

public class SendingThread implements Runnable {

    public Thread thread;
    private boolean suspended;
    private boolean stopped;
    private final Object monitor = new Object();
    private int index;
    private BluetoothSocket socket = null;
    private ObjectOutputStream oos = null;
    private boolean cleaning = false;

    public SendingThread(BluetoothSocket s, int i) {
        thread = new Thread(this, "SendingThread" + i);
        suspended = false;
        stopped = true;
        index = i;
        socket = s;
    }

    public void log(String str) {
        Log.d("SendingThread" + index, str);
    }

    @Override
    public void run() {
        stopped = false;
        try {
            log("sending...");
            while (true) {
                synchronized (monitor) {
                    while (suspended) {
                        log("suspended");
                        monitor.wait();
                    }
                    if (stopped) {
                        log("stopped");
                        clean();
                        break;
                    }
                }
                serverSend();
                // TODO: sleep time & packet size
                Thread.sleep(WorkThread.PACKET_SIZE * 1000 + 1);   // 1001 ms (more than half a chunk)
            }
        } catch (Exception e) {
            log("interrupted / could not write to DataOutputStream: client might be disconnected");
            stopped = true;
            clean();
        }
    }

    private void serverSend() throws IOException, InterruptedException {
        try {
            oos = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            log("could not create DataOutputStream");
            clean();
            return;
        }

        Short[] packet = new Short[WorkThread.packetLen];
        for (int i = 0; i < WorkThread.packetLen; i++) {
            packet[i] = WorkThread.queue.poll();
        }
        String items = "";
        for (Short item : packet)
            items += "," + String.valueOf(item);
        log(items);

        oos.writeObject(packet);
    }

//    public void stop() {
//        synchronized (monitor) {
//            stopped = true;
//            suspended = false;
//            monitor.notifyAll();
//        }
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

    public void clean() {
        if (!cleaning) {
            cleaning = true;
            stopped = true;
            log("cleaning...");

            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    log("could not close DataOutputStream");
                }
                oos = null;
                log("DS cleaned");
            }
//            ConnectionThread.clean();
            cleaning = false;
        }
    }
}
