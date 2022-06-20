package com.example.bluetoothtestwithandroid12;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class LyreActivity extends AppCompatActivity {
    private static Handler mHandler;
    private Button backHomeBtn, keyDown, keyUp;
    private TextView txt_key;
    private Button[] musicBtnArray;
    private BluetoothAdapter bluetoothAdapter;
    private MainActivity.MyBluetoothService.ConnectedThread myThread;
    private BluetoothSocket socket;
    private String my_uuid;
    // #defines for identifying shared types between calling functions
    private final static int REQUEST_ENABLE_BT = 1;
    // used to identify adding bluetooth names
    private final static int MESSAGE_READ = 2;
    // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3;
    // used in bluetooth handler to identify message status
    private int key = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lyre);
        initVarible();
        checkPermission();
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        Set <BluetoothDevice> set = bluetoothAdapter.getBondedDevices();
        if(set.size() > 0){
            BluetoothDevice device = set.iterator().next();                // Spawn a new thread to avoid blocking the GUI one
            new Thread() {
                public void run() {
                    try {
                        if (ActivityCompat.checkSelfPermission(LyreActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            finish();
                        }
                        socket = device.createRfcommSocketToServiceRecord(UUID.fromString(my_uuid));
                        //建立藍芽socket
                    } catch (IOException e) {
                        Log.e("hey", "fail", e);
                        Looper.prepare();
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                        Looper.loop();
                        finish();
                    }
                    // Establish the Bluetooth socket connection.
                    try {
                        if (ActivityCompat.checkSelfPermission(LyreActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            finish();
                        }
                        socket.connect(); //建立藍芽連線
                        startThread(socket);
                    } catch (IOException e) {
                        Log.e("hey", "failed", e);
                        //insert code to deal with this
                        Looper.prepare();
                        Toast.makeText(getBaseContext(), "Socket creation failed",
                                Toast.LENGTH_SHORT).show();
                        Looper.loop();
                        try {
                            socket.close(); //關閉socket
                        } catch (IOException e2) {
                            Log.e("hey", "failed2", e2);
                            //insert code to deal with this
                            Looper.prepare();
                            Toast.makeText(getBaseContext(), "Socket close failed", Toast.LENGTH_LONG);
                            Looper.loop();
                            finish();
                        }
                    }
                }
            }.start();
        }else {
            Toast.makeText(this, "no paired device found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void startThread(BluetoothSocket s){
        myThread = new MainActivity.MyBluetoothService.ConnectedThread(socket);
        myThread.start();
        mHandler.sendEmptyMessage(0);
    }

    public void checkPermission() {
        if (ContextCompat.checkSelfPermission(LyreActivity.this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(LyreActivity.this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 0);
        }

        if (ContextCompat.checkSelfPermission(LyreActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(LyreActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
        }

        if (ContextCompat.checkSelfPermission(LyreActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(LyreActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2);
        }
        if (ContextCompat.checkSelfPermission(LyreActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(LyreActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 3);
        }
    }

    // This function is called when user accept or decline the permission.
    // Request Code is used to check which permission called this function.
    // This request code is provided when user is prompt for permission.
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Checking whether user granted the permission or not.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Showing the toast message
            Toast.makeText(LyreActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(LyreActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }

    public void initVarible(){
        my_uuid = "00001101-0000-1000-8000-00805F9B34FB";
        backHomeBtn = findViewById(R.id.back_home_botton);
        keyDown = findViewById(R.id.key_down);
        keyUp = findViewById(R.id.key_up);
        txt_key = findViewById(R.id.key_txt);
        musicBtnArray = new Button[]{
                findViewById(R.id.c2),
                findViewById(R.id.d2),
                findViewById(R.id.e2),
                findViewById(R.id.f2),
                findViewById(R.id.g2),
                findViewById(R.id.a2),
                findViewById(R.id.b2),
                findViewById(R.id.c3),
                findViewById(R.id.d3),
                findViewById(R.id.e3),
                findViewById(R.id.f3),
                findViewById(R.id.g3),
                findViewById(R.id.a3),
                findViewById(R.id.b3),
                findViewById(R.id.c4),
                findViewById(R.id.d4),
                findViewById(R.id.e4),
                findViewById(R.id.f4),
                findViewById(R.id.g4),
                findViewById(R.id.a4),
                findViewById(R.id.b4)
        };
        mHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                for(int i = 0; i < musicBtnArray.length; i++){
                    musicBtnArray[i].setEnabled(true);
                }
            }
        };
        for(int i = 0; i < musicBtnArray.length; i++){
            musicBtnArray[i].setEnabled(false);
            final int tmp = i;
            musicBtnArray[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    myThread.write(tmp+" ");
                }
            });
        }
        backHomeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    new Thread(){
                        public void run(){
                            myThread.cancel();
                        }
                    }.start();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        keyUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(key > 13){
                    Toast.makeText(LyreActivity.this, "maximum key value reached", Toast.LENGTH_SHORT);
                    return;
                }
                key++;
                myThread.write("+ ");
                txt_key.setText(key+"");
            }
        });
        keyDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(key < -13){
                    Toast.makeText(LyreActivity.this, "minimum key value reached", Toast.LENGTH_SHORT);
                    return;
                }
                key--;
                myThread.write("- ");
                txt_key.setText(key+"");
            }
        });
    }
}