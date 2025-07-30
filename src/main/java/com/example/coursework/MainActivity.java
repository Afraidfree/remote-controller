package com.example.coursework;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<String> deviceNames = new ArrayList<>();
    private ArrayList<BluetoothDevice> devices = new ArrayList<>();
    private ArrayAdapter<String> deviceAdapter;
    private ListView deviceListView;

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;
    private static final int REQUEST_ENABLE_BLUETOOTH = 2;

    private boolean isBluetoothOn = false;

    private ImageButton btnMenu, btnClose, btnBluetooth;
    private ImageView menuBackground;
    private ImageButton btnLightOn, btnLightOff;
    private ImageButton btnSignal;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Инициализация элементов интерфейса
        btnMenu = findViewById(R.id.btnMenu);
        btnClose = findViewById(R.id.btnClose);
        btnBluetooth = findViewById(R.id.btnBluetooth);
        menuBackground = findViewById(R.id.menuBackground);
        deviceListView = findViewById(R.id.deviceListView);

        // Кнопки управления машинкой
        ImageButton upButton = findViewById(R.id.btnUp);
        ImageButton downButton = findViewById(R.id.btnDown);
        ImageButton leftButton = findViewById(R.id.btnLeft);
        ImageButton rightButton = findViewById(R.id.btnRight);
        ImageButton stopButton = findViewById(R.id.btnStop);

        // Кнопки управления светом
        btnLightOn = findViewById(R.id.btnLightOn);
        btnLightOff = findViewById(R.id.btnLightOff);
        ImageButton lightLeftButton = findViewById(R.id.btnLightLeft);
        ImageButton lightRightButton = findViewById(R.id.btnLightRight);

        // Новая кнопка сигнала
        btnSignal = findViewById(R.id.btnSignal);

        // Инициализация адаптера для списка устройств
        deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceNames);
        deviceListView.setAdapter(deviceAdapter);

        // Кнопки меню
        btnMenu.setOnClickListener(v -> toggleMenu(true));
        btnClose.setOnClickListener(v -> toggleMenu(false));
        btnBluetooth.setOnClickListener(v -> toggleBluetooth());

        // Кнопки управления машинкой с использованием OnTouchListener
        setupButtonTouchListener(upButton, "F");
        setupButtonTouchListener(downButton, "B");
        setupButtonTouchListener(leftButton, "L");
        setupButtonTouchListener(rightButton, "R");
        setupButtonTouchListener(stopButton, "S");

        // Кнопки управления светом с анимацией
        btnLightOn.setOnClickListener(v -> {
            sendCommand("H");
            btnLightOn.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction(() -> {
                        btnLightOn.setVisibility(View.GONE);
                        btnLightOn.setAlpha(1f);
                        btnLightOff.setAlpha(0f);
                        btnLightOff.setTranslationX(-200f);
                        btnLightOff.setTranslationY(0f);
                        btnLightOff.setVisibility(View.VISIBLE);
                        btnLightOff.animate()
                                .alpha(1f)
                                .translationX(0f)
                                .setDuration(300)
                                .start();
                    })
                    .start();
        });

        btnLightOff.setOnClickListener(v -> {
            sendCommand("O");
            btnLightOff.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction(() -> {
                        btnLightOff.setVisibility(View.GONE);
                        btnLightOff.setAlpha(1f);
                        btnLightOn.setAlpha(0f);
                        btnLightOn.setTranslationX(200f);
                        btnLightOn.setTranslationY(0f);
                        btnLightOn.setVisibility(View.VISIBLE);
                        btnLightOn.animate()
                                .alpha(1f)
                                .translationX(0f)
                                .setDuration(300)
                                .start();
                    })
                    .start();
        });

        lightLeftButton.setOnClickListener(v -> sendCommand("Q"));
        lightRightButton.setOnClickListener(v -> sendCommand("E"));

        btnSignal.setOnClickListener(v -> sendCommand("W"));

        deviceListView.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDevice selectedDevice = devices.get(position);
            connectToDevice(selectedDevice);
        });

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
    }

    private void setupButtonTouchListener(ImageButton button, String command) {
        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    sendCommand(command);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    sendCommand("S");
                    return true;
            }
            return false;
        });
    }

    private void toggleMenu(boolean open) {
        if (open) {
            btnMenu.setVisibility(View.GONE);
            btnClose.setVisibility(View.VISIBLE);
            btnBluetooth.setVisibility(View.VISIBLE);
            menuBackground.setVisibility(View.VISIBLE);
            deviceListView.setVisibility(View.VISIBLE);
        } else {
            btnMenu.setVisibility(View.VISIBLE);
            btnClose.setVisibility(View.GONE);
            btnBluetooth.setVisibility(View.GONE);
            menuBackground.setVisibility(View.GONE);
            deviceListView.setVisibility(View.GONE);
        }
    }

    private void toggleBluetooth() {
        try {
            if (bluetoothAdapter == null) {
                Toast.makeText(this, "Bluetooth не поддерживается", Toast.LENGTH_SHORT).show();
                Log.e("Bluetooth", "Bluetooth adapter is null");
                return;
            }

            // Проверка разрешений
            String[] permissions = {
                    "android.permission.BLUETOOTH",
                    "android.permission.BLUETOOTH_ADMIN",
                    "android.permission.BLUETOOTH_SCAN",
                    "android.permission.BLUETOOTH_CONNECT"
            };
            ArrayList<String> permissionsToRequest = new ArrayList<>();
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }

            if (!permissionsToRequest.isEmpty()) {
                ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), REQUEST_BLUETOOTH_PERMISSIONS);
                return;
            }

            if (!isBluetoothOn) {
                if (!bluetoothAdapter.isEnabled()) {
                    Toast.makeText(this, "Попытка подключения к Bluetooth", Toast.LENGTH_SHORT).show();
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
                } else {
                    isBluetoothOn = true;
                    btnBluetooth.setImageResource(R.drawable.ic_connect_blutooth);
                    Toast.makeText(this, "Bluetooth уже включен", Toast.LENGTH_SHORT).show();
                    startDeviceDiscovery();
                }
            } else {
                bluetoothAdapter.disable();
                btnBluetooth.setImageResource(R.drawable.ic_disconnect_blutooth);
                Toast.makeText(this, "Bluetooth выключен", Toast.LENGTH_SHORT).show();
                deviceNames.clear();
                devices.clear();
                deviceAdapter.notifyDataSetChanged();
                isBluetoothOn = false;
            }
        } catch (Exception e) {
            Log.e("Bluetooth", "Ошибка при подключении к Bluetooth: " + e.getMessage(), e);
            Toast.makeText(this, "Ошибка подключения к Bluetooth: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Разрешения Bluetooth отклонены", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            // Разрешения получены, повторяем попытку включения Bluetooth
            toggleBluetooth();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                isBluetoothOn = true;
                btnBluetooth.setImageResource(R.drawable.ic_connect_blutooth);
                Toast.makeText(this, "Bluetooth включен", Toast.LENGTH_SHORT).show();
                startDeviceDiscovery();
            } else {
                Toast.makeText(this, "Ошибка подключения: Bluetooth не включен", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void startDeviceDiscovery() {
        deviceNames.clear();
        devices.clear();
        deviceAdapter.notifyDataSetChanged();

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices != null && !pairedDevices.isEmpty()) {
            for (BluetoothDevice device : pairedDevices) {
                deviceNames.add(device.getName() + "\n" + device.getAddress());
                devices.add(device);
            }
            deviceAdapter.notifyDataSetChanged();
        }

        bluetoothAdapter.startDiscovery();
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && device.getName() != null && !devices.contains(device)) {
                    deviceNames.add(device.getName() + "\n" + device.getAddress());
                    devices.add(device);
                    deviceAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        BluetoothSocket tmpSocket = null;
        final Handler handler = new Handler(Looper.getMainLooper());
        final boolean[] isConnected = {false};

        try {
            tmpSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            Toast.makeText(this, "Попытка подключения к " + device.getName(), Toast.LENGTH_SHORT).show();

            BluetoothSocket finalTmpSocket = tmpSocket;
            new Thread(() -> {
                try {
                    finalTmpSocket.connect();
                    isConnected[0] = true;
                    bluetoothSocket = finalTmpSocket;
                    outputStream = bluetoothSocket.getOutputStream();
                    runOnUiThread(() -> Toast.makeText(this, "Подключено к устройству: " + device.getName(), Toast.LENGTH_SHORT).show());
                } catch (IOException e) {
                    runOnUiThread(() -> Toast.makeText(this, "Ошибка подключения к " + device.getName() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }).start();

            handler.postDelayed(() -> {
                if (!isConnected[0]) {
                    try {
                        if (finalTmpSocket != null && !finalTmpSocket.isConnected()) {
                            finalTmpSocket.close();
                        }
                    } catch (IOException e) {
                        // Игнорируем ошибки закрытия
                    }
                    Toast.makeText(this, "Не удалось подключиться к устройству: " + device.getName(), Toast.LENGTH_SHORT).show();
                }
            }, 5000);

        } catch (IOException e) {
            Toast.makeText(this, "Ошибка создания сокета: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            try {
                if (tmpSocket != null) tmpSocket.close();
            } catch (IOException closeException) {
                // Игнорируем
            }
        }
    }

    private void sendCommand(String command) {
        if (outputStream != null) {
            try {
                outputStream.write(command.getBytes());
                Toast.makeText(this, "Команда отправлена: " + command, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(this, "Ошибка отправки команды", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Нет подключения к устройству", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (outputStream != null) outputStream.close();
            if (bluetoothSocket != null) bluetoothSocket.close();
        } catch (IOException e) {
            Toast.makeText(this, "Ошибка закрытия Bluetooth", Toast.LENGTH_SHORT).show();
        }
        unregisterReceiver(receiver);
    }
}