package com.example.bluetoothtestwithandroid12;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private static Handler mHandler;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> bluetoothStringAdapter;
    private Button findDeviceBtn, sendBtn, leaveBtn;
    private ListView bluetoothList;
    private MyBluetoothService myService;
    private MyBluetoothService.ConnectedThread myThread;
    private BluetoothSocket socket;

    private String my_uuid;

    private TextView connectingStatue;
    // #defines for identifying shared types between calling functions
    private final static int REQUEST_ENABLE_BT = 1;
    // used to identify adding bluetooth names
    private final static int MESSAGE_READ = 2;
    // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3;
    // used in bluetooth handler to identify message status

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }


        connectingStatue = findViewById(R.id.connectingStatue);
        leaveBtn = findViewById(R.id.leaveBtn);
        sendBtn = findViewById(R.id.sendBtn);
        findDeviceBtn = findViewById(R.id.findDeviceBtn);
        my_uuid = "00001101-0000-1000-8000-00805F9B34FB";

        // Ask for permissions
        // bluetooth_scan, bluetooth_connect, fine_location, coarse_location
        checkPermission();

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        // set handler
        bluetoothStringAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        bluetoothList = findViewById(R.id.listView);
        bluetoothList.setAdapter(bluetoothStringAdapter); // assign model to view
        mHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == CONNECTING_STATUS) {
                    //收到CONNECTING_STATUS 顯示以下訊息
                    if (msg.arg1 == 1)
                        connectingStatue.setText("Connected to Device: " + (String) (msg.obj));
                    else
                        connectingStatue.setText("Connection Failed");
                }
            }
        };

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        leaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                bluetoothAdapter.disable(); // turn off bluetooth]
                Toast.makeText(getApplicationContext(), "Bluetooth turned Off",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        findDeviceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                if (bluetoothAdapter.isDiscovering()) { //如果已經找到裝置
                    bluetoothAdapter.cancelDiscovery(); //取消尋找
                    Toast.makeText(getApplicationContext(), "Discovery stopped", Toast.LENGTH_SHORT).show();
                } else {
                    if (bluetoothAdapter.isEnabled()) { //如果沒找到裝置且已按下尋找
                        bluetoothStringAdapter.clear(); // clear items
                        bluetoothAdapter.startDiscovery(); //開始尋找
                        Toast.makeText(getApplicationContext(), "Discovery started",
                                Toast.LENGTH_SHORT).show();
                        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
                    } else {
                        Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        bluetoothList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                connectingStatue.setText("Connecting...");
                // Get the device MAC address, which is the last 17 chars in the View
                String info = ((TextView) v).getText().toString();
                final String address = info.substring(info.length() - 17);
                final String name = info.substring(0, info.length() - 17);

                // Spawn a new thread to avoid blocking the GUI one
                new Thread() {
                    public void run() {
                        boolean fail = false;
                        //取得裝置MAC找到連接的藍芽裝置
                        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);

                        try {
                            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                // TODO: Consider calling
                                //    ActivityCompat#requestPermissions
                                // here to request the missing permissions, and then overriding
                                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                //                                          int[] grantResults)
                                // to handle the case where the user grants the permission. See the documentation
                                // for ActivityCompat#requestPermissions for more details.
                                return;
                            }
                            socket = device.createRfcommSocketToServiceRecord(UUID.fromString(my_uuid));
                            //建立藍芽socket
                        } catch (IOException e) {
                            fail = true;
                            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                        }
                        // Establish the Bluetooth socket connection.
                        try {
                            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                // TODO: Consider calling
                                //    ActivityCompat#requestPermissions
                                // here to request the missing permissions, and then overriding
                                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                //                                          int[] grantResults)
                                // to handle the case where the user grants the permission. See the documentation
                                // for ActivityCompat#requestPermissions for more details.
                                return;
                            }
                            socket.connect(); //建立藍芽連線
                        } catch (IOException e) {
                            try {
                                fail = true;
                                socket.close(); //關閉socket
                                //開啟執行緒 顯示訊息
                                mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                        .sendToTarget();
                            } catch (IOException e2) {
                                //insert code to deal with this
                                Toast.makeText(getBaseContext(), "Socket creation failed",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                        if (fail == false) {
                            //開啟執行緒用於傳輸及接收資料
                            myService = new MyBluetoothService();
                            myThread = new MyBluetoothService.ConnectedThread(socket);
                            myThread.start();
                            //開啟新執行緒顯示連接裝置名稱
                            mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                                    .sendToTarget();
                        }
                    }
                }.start();
            }
        });
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                if (bluetoothAdapter.getBondedDevices().size() > 0) {
                    try {
                        new Thread(){
                            public void run(){
                                myThread.cancel();
                            }
                        }.start();
                        socket.close();
                    } catch (IOException e) {
                        Log.e("e", "e", e);
                        e.printStackTrace();
                    }
                }
                Intent intent = new Intent(MainActivity.this, LyreActivity.class);
                startActivity(intent);
            }
        });
    }

    final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name to the list
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                bluetoothStringAdapter.add(device.getName() + "\n" + device.getAddress());
                bluetoothStringAdapter.notifyDataSetChanged();
            }
        }
    };

    public void checkPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 0);
        }

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
        }

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 3);
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
            Toast.makeText(MainActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }


    public static class MyBluetoothService {
        private static final String TAG = "MY_APP_DEBUG_TAG";

        // Defines several constants used when transmitting messages between the
        // service and the UI.
        private interface MessageConstants {
            public static final int MESSAGE_READ = 0;
            public static final int MESSAGE_WRITE = 1;
            public static final int MESSAGE_TOAST = 2;

            // ... (Add other message types here as needed.)
        }

        public static class ConnectedThread extends Thread {
            private final BluetoothSocket mmSocket;
            private final InputStream mmInStream;
            private final OutputStream mmOutStream;

            public ConnectedThread(BluetoothSocket socket) {
                mmSocket = socket;
                InputStream tmpIn = null;
                OutputStream tmpOut = null;

                // Get the input and output streams, using temp objects because
                // member streams are final
                try {
                    tmpIn = socket.getInputStream();
                    tmpOut = socket.getOutputStream();
                } catch (IOException e) { }

                mmInStream = tmpIn;
                mmOutStream = tmpOut;
            }

            public void run() {
                byte[] buffer = new byte[1024];  // buffer store for the stream
                int bytes; // bytes returned from read()
                // Keep listening to the InputStream until an exception occurs
                while (true) {
                    try {
                        // Read from the InputStream
                        bytes = mmInStream.available();
                        if(bytes != 0) {
                            SystemClock.sleep(100);
                            //pause and wait for rest of data
                            bytes = mmInStream.available();
                            // how many bytes are ready to be read?
                            bytes = mmInStream.read(buffer, 0, bytes);
                            // record how many bytes we actually read
                            mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                    .sendToTarget(); // Send the obtained bytes to the UI activity
                        }
                    } catch (IOException e) {
                        e.printStackTrace();

                        break;
                    }
                }
            }

            /* Call this from the main activity to send data to the remote device */
            public void write(String input) {
                byte[] bytes = input.getBytes();           //converts entered String into bytes
                try {
                    mmOutStream.write(bytes);
                } catch (IOException e) {
                    Log.e(TAG, "failed", e);
                }
            }

            /* Call this from the main activity to shutdown the connection */
            public void cancel() {
                try {
                    mmSocket.close();
                } catch (IOException e) { }
            }
        }
    }
}