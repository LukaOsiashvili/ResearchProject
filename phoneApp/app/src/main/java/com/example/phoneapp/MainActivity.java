package com.example.phoneapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "PhoneApp";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final int REQUEST_ENABLE_BT = 456;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothServerSocket serverSocket;
    private BluetoothSocket socket;
    private TextView statusText;
    private TextView dataText;
    private Handler mainHandler;
    private ExecutorService executorService;
    private EmotionalStateAnalyzer emotionalStateAnalyzer;
    private RecommendationEngine recommendationEngine;
    private SwipeRefreshLayout swipeRefreshLayout;

    private boolean isListening = true;
    private static final int AUTO_REFRESH_INTERVAL = 1000;

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        dataText = findViewById(R.id.dataText);
        Button youtubeButton = findViewById(R.id.youtubeButton);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        mainHandler = new Handler(Looper.getMainLooper());
        executorService = Executors.newFixedThreadPool(2);
        emotionalStateAnalyzer = new EmotionalStateAnalyzer();
        recommendationEngine = new RecommendationEngine(this, youtubeButton);

        checkAndRequestPermissions();
        initializeBluetooth();

        String data = "HR:90";
        processReceivedData(data);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            processReceivedData(data);
//            initializeBluetooth();
            swipeRefreshLayout.setRefreshing(false);
        });


    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void checkAndRequestPermissions() {
        String[] permissions = new String[0];

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12 (API 31) and higher
                permissions = new String[]{
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.ACCESS_FINE_LOCATION
            };
        } else {
            // For Android 11 (API 30) and lower
            permissions = new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
        }

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                updateStatus("Permissions denied. Cannot proceed without Bluetooth permissions.");
            } else {
                initializeBluetooth();
            }
        }
    }

    private void initializeBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            updateStatus("Bluetooth not supported on this device");
            updateUI("No Data");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            showBluetoothEnableDialog();
            updateUI("No Data");
            return;
        }

        startBluetoothServer();
    }

    // Show dialog window to prompt the user to enable Bluetooth
    private void showBluetoothEnableDialog() {
        new AlertDialog.Builder(this)
                .setMessage("Bluetooth is disabled. Please enable it to continue.")
                .setPositiveButton("Enable", (dialog, which) -> {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                })
                .setNegativeButton("Cancel", (dialog, which) -> updateStatus("Bluetooth is required for connection"))
                .create()
                .show();
    }

    private void startBluetoothServer() {
        executorService.execute(() -> {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("PhoneApp", MY_UUID);
                updateStatus("Waiting for WearOS device connection...");
                updateUI("No Data");

                while (isListening) {
                    try {
                        socket = serverSocket.accept();
                        if (socket != null) {
                            updateStatus("Connected to WearOS device");
                            startDataReception();
                            break;
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Socket accept failed", e);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Server socket creation failed", e);
                updateStatus("Bluetooth server error: " + e.getMessage());
            }
        });
    }

    private void startDataReception() {
        executorService.execute(() -> {
            try {
                InputStream inputStream = socket.getInputStream();
                byte[] buffer = new byte[1024];
                int bytes;

                while (isListening) {
                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        String data = new String(buffer, 0, bytes);
                        processReceivedData(data);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Data reception error", e);
                updateStatus("Connection lost: " + e.getMessage());
            }
        });
    }

//  Notice: Because of using the emulators to run the application, implementing
//  some parts is not possible due to virtual machine constraints.
//  Some code is commented and basic simpler approach is used, so that the application
//  can run and we can see how it works.
//  The commented out code is the part of the project and may be used for some functionalities,
//  but - as mentioned previously - it requires physical device.

//    Simple Implementation to Run and See the Basic Working Principle of Application
    private void processReceivedData(String data) {

        String[] parts = data.split(":");
        if (parts.length != 2) return;

        if (parts[0].equals("HR")) {
            float heartRate = Float.parseFloat(parts[1]);
            emotionalStateAnalyzer.setHeartRate(heartRate);
        } else if (parts[0].equals("ACC")) {
            String[] values = parts[1].split(",");
            if (values.length == 3) {
                float[] acceleration = new float[]{
                        Float.parseFloat(values[0]),
                        Float.parseFloat(values[1]),
                        Float.parseFloat(values[2])

                };
                emotionalStateAnalyzer.setACC(acceleration);
            }
        }

        if(true){
            EmotionalState state = emotionalStateAnalyzer.analyzeEmotionalState();
            String recommendation = recommendationEngine.getRecommendation(state);

            String diagnose = ("Raw Data: " + data +
                    "\nEmotional State: " + state +
                    "\nRecommendation: " + recommendation);

            updateUI(diagnose);
        } else {
            mainHandler.post(() -> updateUI("Insufficient data to process emotional state."));
        }
    }


//    The Original Code for Intended Functionality

//    private void processReceivedData(String data) {
//
//        String[] parts = data.split(":");
//        if (parts.length != 2) return;
//
//        if (parts[0].equals("HR")) {
//            float heartRate = Float.parseFloat(parts[1]);
//            emotionalStateAnalyzer.addHeartRateData(heartRate);
//        } else if (parts[0].equals("ACC")) {
//            String[] values = parts[1].split(",");
//            if (values.length == 3) {
//                float[] acceleration = new float[]{
//                        Float.parseFloat(values[0]),
//                        Float.parseFloat(values[1]),
//                        Float.parseFloat(values[2])
//                };
//
//                emotionalStateAnalyzer.addAccelerometerData(acceleration);
//            }
//        }
//
//        if (emotionalStateAnalyzer.hasEnoughData()) {
//            EmotionalState state = emotionalStateAnalyzer.analyzeEmotionalState();
//            String recommendation = recommendationEngine.getRecommendation(state);
//
//            String diagnose = ("Raw Data: " + data +
//                    "\nEmotional State: " + state +
//                    "\nRecommendation: " + recommendation);
//
//            updateUI(diagnose);
//        } else {
//            mainHandler.post(() -> updateUI("Insufficient data to process emotional state."));
//        }
//    }

    private void updateUI(String string) {
        mainHandler.post(() -> dataText.setText(string));
    }

    private void updateStatus(String message) {
        mainHandler.post(() -> statusText.setText(message));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isListening = false;
        executorService.shutdown();

        try {
            if (socket != null) socket.close();
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing sockets", e);
        }
    }
}
