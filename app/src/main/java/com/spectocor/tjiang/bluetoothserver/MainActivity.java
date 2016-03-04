package com.spectocor.tjiang.bluetoothserver;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MainActivity extends AppCompatActivity {

    public static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case WorkThread.MSG_COUNTER: {
                        textView.setText(String.valueOf((short) msg.obj));
                        break;
                    }
                    default:
                        break;
                }
            }
        }
    }

    public final MyHandler mHandler = new MyHandler(this);

    private static WorkThread wt;
    private static ConnectionThread ct;
    private static boolean cleaning = false;
    public static TextView textView;
    private static int selected = 0;
    private static int DEFAULT_SAMPLE_RATE = 3;

    public static void log(String str) {
        Log.d("MainActivity", str);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("entering onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.textView1);

        final Spinner spinner = (Spinner) findViewById(R.id.sample_rates);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.sample_rates_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (spinner.getSelectedItemPosition()) {
                    case 0:
                        WorkThread.SAMPLE_RATE = 3;
                        break;
                    case 1:
                        WorkThread.SAMPLE_RATE = 360;
                        break;
                    case 2:
                        WorkThread.SAMPLE_RATE = 300;
                        break;
                    case 3:
                        WorkThread.SAMPLE_RATE = 400;
                        break;
                    case 4:
                        WorkThread.SAMPLE_RATE = 500;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        final Spinner spinner2 = (Spinner) findViewById(R.id.data);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this,
                R.array.data_array, android.R.layout.simple_spinner_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner2.setAdapter(adapter2);
        spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int newSelected = spinner2.getSelectedItemPosition();
                if (selected != newSelected) {
                    stopWorking();
                    startWorkingAt(WorkThread.SAMPLE_RATE);
                    switch (newSelected) {
                        case 0:
                            WorkThread.DATA_TYPE = "SampleData";
                            break;
                        case 1:
                            WorkThread.DATA_TYPE = "NODATA";
                            break;
                    }
                    selected = newSelected;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public void onResume() {
        log("entering onResume()");
        startWorkingAt(DEFAULT_SAMPLE_RATE);    // onResume restarts app
        setUpConnection();
        super.onResume();
    }

    @Override
    public void onPause() {
        log("entering onPause()");
        clean();
        stopWorking();
        super.onPause();
    }

//    public void resume(View view) throws InterruptedException {
//        if (wt != null && wt.isSuspended()) {
//            wt.resume();
//        }
//        if (ct != null && ct.isSuspended()) {
//            ct.resume();
//        }
//    }
//
//    public void pause(View view) {
//        if (wt != null && wt.isRunning())
//            wt.suspend();
//
//        if (ct != null && ct.isRunning()) {
//            ct.suspend();
//        }
//    }
//
//    public void connect(View view) {
//        log("entering connect()");
//        setUpConnection();
//    }
//
//    public void disconnect(View view) {
//        log("entering disconnect()");
//        clean();
//    }

    private void setUpConnection() {
        if (ct == null)
            ct = new ConnectionThread(mHandler);
        if (!ct.isRunning()) {
            log("create new ConnThread");
            ct.setContext(getApplicationContext());
            ct.thread.start();
        }
    }

    private static void clean() {
        if (!cleaning && ct != null) {
            cleaning = true;
            log("cleaning...");
            if (ct.isRunning()) {
                ConnectionThread.clean();
            }
            ct = null;
            log("CT cleaned");
            cleaning = false;
        }
    }

    private void startWorkingAt(int sr) {
        if (wt == null)
            wt = new WorkThread(mHandler);
        if (!wt.isRunning()) {
            WorkThread.SAMPLE_RATE = sr;
            wt.thread.start();
        }
    }

    private void stopWorking() {
        if (wt != null) {
            wt.stop();
            wt = null;
        }
    }
}
