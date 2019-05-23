package ir.guftall.osproj;

import android.app.Activity;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import io.socket.emitter.Emitter;
import ir.guftall.osproj.Recorder2.AudioRecorderConfiguration;
import ir.guftall.osproj.Recorder2.ExtAudioRecorder;
import ir.guftall.osproj.Recorder2.FailRecorder;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final static String TAG = "MAIN";
    private static ScrollView scroll;
    private static Activity context;

    private ExtAudioRecorder extAudioRecorder;
    private static int fileIncrementPath = 1;

    private TextView tvRecord;

    private Timer timer;
    private TimerTask timerTask;

    private Connection connection;
    private Connection listenerConnection;

    private static int i = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        scroll = findViewById(R.id.scrollView1);
        scroll.setScrollBarFadeDuration(0);

        findViewById(R.id.btn_recorder).setOnClickListener(this);
        findViewById(R.id.btn_recorder_stop).setOnClickListener(this);
        tvRecord = findViewById(R.id.tv_recorder_timer);

        findViewById(R.id.tv_recorder_timer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addText("Omid" + ++i);
            }
        });

        connection = Connection.getConnection();
        listenerConnection = Connection.getConnection();

        listenerConnection.addListener("r", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                addText("" + args[0]);
            }
        });
        connection.connect();
        listenerConnection.connect();

        listenerConnection.sendListen();
    }

    private void initAudioRecorder2() {

        AudioRecorderConfiguration configuration = new AudioRecorderConfiguration.Builder()
                .recorderListener(listener)
                .handler(handler)
                .uncompressed(true)
                .builder();

        extAudioRecorder = new ExtAudioRecorder(configuration);
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    ExtAudioRecorder.RecorderListener listener = new ExtAudioRecorder.RecorderListener() {
        @Override
        public void recordFailed(FailRecorder failRecorder) {
            if (failRecorder.getType() == FailRecorder.FailType.NO_PERMISSION) {
                Toast.makeText(MainActivity.this, "No permission", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "what the ff", Toast.LENGTH_SHORT).show();
            }
        }
    };


    public static void addText(String text) {
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setFocusable(true);
        textView.setFocusableInTouchMode(true);
        addToLinearLayout(textView);
    }

    private static void addToLinearLayout(final View view) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LinearLayout root = context.findViewById(R.id.scrollLinearLayout1);
                root.addView(view);
                view.requestFocus();
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_recorder:

                initAudioRecorder2();
                if (!isExitsSdcard()) {
                    String needSd = "No sd card found";
                    Toast.makeText(MainActivity.this, needSd, Toast.LENGTH_SHORT).show();
                    return;
                }
                startRecording();
                break;
            case R.id.btn_recorder_stop:

                stopRecording();
                break;
        }
    }


    private void startRecording() {
        if (timer == null)
            timer = new Timer();
        if (timerTask != null) {
            timerTask.cancel();
        }

        updateFileIncrementPath();
        extAudioRecorder.setOutputFile(getFilePath());
        extAudioRecorder.prepare();
        extAudioRecorder.start();

        timerTask = new TimerTask() {
            @Override
            public void run() {

                extAudioRecorder.stop();
                extAudioRecorder.reInit();

                final String currentPath = getFilePath();
                updateFileIncrementPath();
                extAudioRecorder.setOutputFile(getFilePath());
                extAudioRecorder.prepare();
                extAudioRecorder.start();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {

                            ByteBuffer buffer = readAndDeleteRecordedFile(currentPath);

                            if (connection != null) {
                                connection.sendAudio(buffer);
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "read and delete file", e);
                        }
                    }
                }).start();
            }
        };
        timer.schedule(timerTask, 3000, 3000);
    }

    private void stopRecording() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        if (extAudioRecorder != null) {

            extAudioRecorder.stop();
            extAudioRecorder.release();
            extAudioRecorder = null;
        }
    }

    public static boolean isExitsSdcard() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    private ByteBuffer readAndDeleteRecordedFile(String filePath) throws IOException {
        RandomAccessFile f = new RandomAccessFile(filePath, "r");


        byte[] buffer = new byte[(int) f.length()];

        int size = f.read(buffer, 0, buffer.length);

        f.close();

        (new File(filePath)).delete();

//        Log.e(TAG, "firstByte:" + buffer[0]);
//        Log.e(TAG, "lastByte:" + buffer[size - 1]);
        return ByteBuffer.wrap(buffer, 0, size);
    }

    private static String getFilePath() {
        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fileIncrementPath + "media.wav";
        return filePath;
    }

    private static void updateFileIncrementPath() {
        ++fileIncrementPath;
    }
}
