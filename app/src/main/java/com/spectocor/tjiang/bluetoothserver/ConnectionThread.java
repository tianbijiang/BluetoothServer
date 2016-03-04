package com.spectocor.tjiang.bluetoothserver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.util.LinkedList;
import java.util.UUID;

public class ConnectionThread implements Runnable {

    private static LinkedList<SendingThread> sts = new LinkedList<>();
    private int i = 0;
    private LinkedList<String> clients = new LinkedList<>();

    private static MainActivity.MyHandler myHandler;
    private Context context;
    public Thread thread;
    private static boolean stopped;
    private static boolean suspended;
    private static boolean statusOnline = false;
    private final Object monitor = new Object();
    private static boolean cleaning = false;

    private static BluetoothAdapter mBluetoothAdapter = null;
    private static BluetoothServerSocket mServerSocket = null;
    private static BluetoothSocket socket = null;
    private static final UUID SOCKET_UUID = UUID.fromString("4e5d48e0-75df-11e3-981f-0800200c9a66");
    private static final String SOCKET_NAME = "BLTServer";
    public static final int SVR_STATUS = 2;
    public static final int CLT_LIST = 3;

    ConnectionThread(MainActivity.MyHandler m) {
        thread = new Thread(this, "ConnThread");
        stopped = true;
        suspended = false;
        myHandler = m;
    }

    public void setContext(Context c) {
        context = c;
    }

    public static void log(String str) {
        Log.d("ConnThread", str);
    }

    @Override
    public void run() {
        log("running...");
        stopped = false;

        try {
            if (checkSupport()) {
                enableBluetooth();

                // TODO: better waiting
                while (!mBluetoothAdapter.isEnabled()) {
                    Thread.sleep(1000);
                    log("waiting for enableBluetooth()");
                }
                createServerSocket();

                // TODO: better waiting
                while (mServerSocket == null) {
                    Thread.sleep(1000);
                    log("waiting for createServerSocket()");
                }

                try {
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
                        listenForConnection();
                    }
                } catch (InterruptedException exc) {
                    log("interrupted");
                    clean();
                }
            }
        } catch (Exception e) {
            log("could not create ServerSocket");
            clean();
        }
    }

    public boolean isRunning() {
        return !stopped;
    }

//    public static void updateOnline() {
//        myHandler.sendMessage(myHandler.obtainMessage(SVR_STATUS, statusOnline));
//    }
//
//    public void updateDevices(String address) {
//        clients.add(address);
//        myHandler.sendMessage(myHandler.obtainMessage(CLT_LIST, clients));
//        for (String item : clients)
//            log(item);
//    }
//
//    public boolean isSuspended() {
//        return suspended;
//    }
//
//    public void stop() {
//        synchronized (monitor) {
//            stopped = true;
//            suspended = false;
//            monitor.notifyAll();
//        }
//    }
//
//    public void suspend() {
//        log("suspended");
//        synchronized (monitor) {
//            suspended = true;
//            monitor.notifyAll();
//            for (SendingThread item : sts) {
//                if (item != null) {
//                    item.suspend();
//                }
//            }
//        }
//    }
//
//    public void resume() {
//        log("resuming");
//        synchronized (monitor) {
//            for (SendingThread item : sts) {
//                if (item != null) {
//                    item.resume();
//                }
//            }
//            suspended = false;
//            monitor.notifyAll();
//        }
//    }

    private boolean checkSupport() {
        log("entering checkSupport()");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            log("bluetooth not supported");
            return false;
        }
        return true;
    }

    private void enableBluetooth() {
        if (!mBluetoothAdapter.isEnabled()) {
            log("entering enableBluetooth()");
            Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            // TODO: avoid?
            turnOnIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(turnOnIntent);
        } else
            log("bluetooth already enabled");
    }

//    private void setDiscoverable() {
//        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
//            log("entering setDiscoverable()");
//            Intent discoverableIntent = new
//                    Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
//            discoverableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            context.startActivity(discoverableIntent);
//        } else
//            log("bluetooth already discoverable");
//    }

    private void createServerSocket() throws IOException {
        log("entering createServerSocket()");
        BluetoothServerSocket tmp;
        tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(SOCKET_NAME, SOCKET_UUID);
        mServerSocket = tmp;
    }

    private void listenForConnection() {
        log("entering listenForConnection()");
        try {
            log("listening...");
            socket = mServerSocket.accept();    // block until a connection is established
        } catch (IOException e) {
            log("listening force stopped");
            clean();
            return;
        }

        if (socket != null) {
            SendingThread st = new SendingThread(socket, i);
            sts.add(st);
            st.thread.start();
            i++;
        }
    }

    public static void clean() {
        stopped = true;
        if (!cleaning) {
            cleaning = true;
            log("cleaning...");

            if (mServerSocket != null) {
                try {
                    mServerSocket.close();
                } catch (IOException e) {
                    log("could not close server socket");
                }
                mServerSocket = null;
                log("ServerSocket cleaned");
            }

            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    log("could not close socket");
                }
                socket = null;
                log("socket closed");
            }

            for (SendingThread item : sts) {
                if (item != null) {
                    item.clean();
                }
            }
            sts.clear();
            log("ST cleaned");

            cleaning = false;
        }
    }
}
