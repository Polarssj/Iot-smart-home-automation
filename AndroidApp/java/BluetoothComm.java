package com.example.smarthome;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BluetoothComm extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSION = 1001;
    private static final int REQUEST_BLUETOOTH_CONNECT_PERMISSION = 1002;
    private static final int REQUEST_BLUETOOTH_SCAN_PERMISSION = 1003; // Declare the constant
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<String> discoveredDevicesList;
    private ArrayAdapter<String> discoveredDevicesAdapter;
    private BluetoothSocket socket;
    private ToggleButton toggleButton1;
    private ToggleButton toggleButton2;
    private ToggleButton toggleButton3;
    private ToggleButton toggleButton4;
    private ToggleButton toggleButton5;
    private ToggleButton toggleButton6;
    private ToggleButton toggleButton7;
    private ToggleButton toggleButton8;
    private ToggleButton toggleButton9;
    private boolean receivingData = false;
    private final Handler mHandler = new Handler();
    private static final int MESSAGE_READ = 2;
    private static final String LAST_CONNECTED_DEVICE_ADDRESS_KEY = "last_connected_device_address";
    private String lastConnectedDeviceAddress;
    ImageView connectionStatusImageView;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_comm);

        toggleButton1 = findViewById(R.id.toggleButton1);
        toggleButton2 = findViewById(R.id.toggleButton2);
        toggleButton3 = findViewById(R.id.toggleButton3);
        toggleButton4 = findViewById(R.id.toggleButton4);
        toggleButton5 = findViewById(R.id.toggleButton5);
        toggleButton6 = findViewById(R.id.toggleButton6);
        toggleButton7 = findViewById(R.id.toggleButton7);
        toggleButton8 = findViewById(R.id.toggleButton8);
        toggleButton9 = findViewById(R.id.toggleButton9);
        shrinkMotion(toggleButton1);
        shrinkMotion(toggleButton2);
        shrinkMotion(toggleButton3);
        shrinkMotion(toggleButton4);
        shrinkMotion(toggleButton5);
        shrinkMotion(toggleButton6);
        shrinkMotion(toggleButton7);
        shrinkMotion(toggleButton8);
        shrinkMotion(toggleButton9);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        connectionStatusImageView = findViewById(R.id.imageView);

        Button btnDiscoverDevices = findViewById(R.id.button1);
        shrinkMotion(btnDiscoverDevices);
        Button Disconnect = findViewById(R.id.button2);
        shrinkMotion(Disconnect);
        disableToggleButtons();

        // Load the last connected device address from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("bluetooth_prefs", MODE_PRIVATE);
        lastConnectedDeviceAddress = sharedPreferences.getString(LAST_CONNECTED_DEVICE_ADDRESS_KEY, null);

// Log the value of lastConnectedDeviceAddress
        Log.d("LastDeviceAddress", "Last Connected Device Address: " + lastConnectedDeviceAddress);

        if (lastConnectedDeviceAddress != null) {
            // Delay the connection to the last connected device by 1000 milliseconds (1 second)
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    connectToDevice(lastConnectedDeviceAddress);
                }
            }, 1000);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH
            }, REQUEST_BLUETOOTH_PERMISSION);
        }

        // Initialize BluetoothAdapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            // Disable or hide buttons related to Bluetooth functionality
            btnDiscoverDevices.setEnabled(false);
//            return false;
        }

        ImageView imageView1 = findViewById(R.id.imageView1);
        imageView1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupMenu(v);
            }
        });

        // Initialize ArrayList and ArrayAdapter
        discoveredDevicesList = new ArrayList<>();
        discoveredDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, discoveredDevicesList);

        btnDiscoverDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableBluetooth();
            }
        });

        Disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnectDevice();
            }
        });

        // Add OnBackPressedCallback
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(BluetoothComm.this, choosing.class);
                closeSocket();
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        });

        // Set up toggle button listeners
        setupToggleButtonListeners();

        registerBluetoothReceiver();

        // Start the HandlerThread only once
        timeoutHandlerThread.start();
        // Create a Handler associated with the HandlerThread
        timeoutHandler = new Handler(timeoutHandlerThread.getLooper());
    }

    

    @SuppressLint("ClickableViewAccessibility")
    void shrinkMotion (View view1) {
        view1.setOnTouchListener((view, motionEvent) -> {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(0.92f).scaleY(0.92f).setDuration(30).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1f).scaleY(1f).setDuration(30).start();
                    break;
            }
            return false;
        });
    }

    private void disableToggleButtons() {
        runOnUiThread(() -> {
            toggleButton1.setEnabled(false);
            toggleButton2.setEnabled(false);
            toggleButton3.setEnabled(false);
            toggleButton4.setEnabled(false);
            toggleButton5.setEnabled(false);
            toggleButton6.setEnabled(false);
            toggleButton7.setEnabled(false);
            toggleButton8.setEnabled(false);
            toggleButton9.setEnabled(false);
        });
    }

    private void uncheckButton() {
        runOnUiThread(() -> {
            toggleButton1.setChecked(false);
            toggleButton2.setChecked(false);
            toggleButton3.setChecked(false);
            toggleButton4.setChecked(false);
            toggleButton5.setChecked(false);
            toggleButton6.setChecked(false);
            toggleButton7.setChecked(false);
            toggleButton8.setChecked(false);
            toggleButton9.setEnabled(false);
        });
    }

    private void setupToggleButtonListeners() {
        toggleButton1.setOnCheckedChangeListener(createToggleListener('A', 'a'));
        toggleButton2.setOnCheckedChangeListener(createToggleListener('B', 'b'));
        toggleButton3.setOnCheckedChangeListener(createToggleListener('C', 'c'));
        toggleButton4.setOnCheckedChangeListener(createToggleListener('D', 'd'));
        toggleButton5.setOnCheckedChangeListener(createToggleListener('E', 'e'));
        toggleButton6.setOnCheckedChangeListener(createToggleListener('F', 'f'));
        toggleButton7.setOnCheckedChangeListener(createToggleListener('G', 'g'));
        toggleButton8.setOnCheckedChangeListener(createToggleListener('H', 'h'));
        toggleButton9.setOnCheckedChangeListener(createToggleListener('J', 'j'));
    }

    private CompoundButton.OnCheckedChangeListener createToggleListener(final char upperCase, final char lowerCase) {
        return new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isPressed()) {
                    // Send upper case character when toggle button is turned on
                    sendCharacter(isChecked ? upperCase : lowerCase);
                }
            }
        };
    }

    @SuppressLint("ResourceType")
    private void showPopupMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.inflate(R.menu.popup_menu); // Inflate the menu resource
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.logout) {
                    // Handle menu item click for logout
                    SharedPreferences sharedPreferences = getSharedPreferences("login_status", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("isLoggedIn", false);
                    editor.apply();
                    closeSocket();
                    Intent intent = new Intent(BluetoothComm.this, MainActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    return true;
                }
                return false;
            }
        });
        popupMenu.show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister BluetoothReceiver when the activity is destroyed
        unregisterBluetoothReceiver();
    }

    private void registerBluetoothReceiver() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(bluetoothReceiver, filter);
    }

    private void unregisterBluetoothReceiver() {
        unregisterReceiver(bluetoothReceiver);
    }

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Device found
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Toast.makeText(context, "Device found: " + device.getName(), Toast.LENGTH_SHORT).show();
                ConnectedThread connectedThread = new ConnectedThread(socket);
                connectedThread.start();
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                // Bluetooth device disconnected
                if (socket != null) { // Check if socket is not null before attempting to close
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // Handle IOException appropriately
                        e.printStackTrace();
                    }
                }
                connectionStatusImageView.setImageResource(R.drawable.bluetooth_off);
                disableToggleButtons();
                uncheckButton();
                Toast.makeText(context, "Bluetooth device disconnected", Toast.LENGTH_SHORT).show();
                // Perform any necessary actions upon disconnection
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                // Bluetooth device connected
                Toast.makeText(context, "Bluetooth device connected", Toast.LENGTH_SHORT).show();
                // Start a thread to read data from the connected device
                ConnectedThread connectedThread = new ConnectedThread(socket);
                connectedThread.start();
            }
        }
    };

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmInStream = tmpIn;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI thread for display
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();

                    // Display received data through toast message
                    displayReceivedData(buffer, bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        private void displayReceivedData(final byte[] buffer, final int bytesRead) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String receivedMessage1 = new String(buffer, 0, bytesRead);
                    String receivedMessage = receivedMessage1.trim();
                    updateToggleState(receivedMessage);
                }
            });
        }
    }

    private void updateToggleState(String receivedMessage) {
        if (receivedMessage.equals("A")) {
            // Turn on all toggle buttons
            toggleButton1.setChecked(true);
        } else if (receivedMessage.equals("a")) {
            // Turn off all toggle buttons
            toggleButton1.setChecked(false);
        } else if (receivedMessage.equals("B")) {
            // Turn on all toggle buttons
            toggleButton2.setChecked(true);
        } else if (receivedMessage.equals("b")) {
            // Turn off all toggle buttons
            toggleButton2.setChecked(false);
        } else if (receivedMessage.equals("C")) {
            // Turn on all toggle buttons
            toggleButton3.setChecked(true);
        } else if (receivedMessage.equals("c")) {
            // Turn off all toggle buttons
            toggleButton3.setChecked(false);
        } else if (receivedMessage.equals("D")) {
            // Turn on all toggle buttons
            toggleButton4.setChecked(true);
        } else if (receivedMessage.equals("d")) {
            // Turn off all toggle buttons
            toggleButton4.setChecked(false);
        } else if (receivedMessage.equals("E")) {
            // Turn on all toggle buttons
            toggleButton5.setChecked(true);
        } else if (receivedMessage.equals("e")) {
            // Turn off all toggle buttons
            toggleButton5.setChecked(false);
        } else if (receivedMessage.equals("F")) {
            // Turn on all toggle buttons
            toggleButton6.setChecked(true);
        } else if (receivedMessage.equals("f")) {
            // Turn off all toggle buttons
            toggleButton6.setChecked(false);
        } else if (receivedMessage.equals("G")) {
            // Turn on all toggle buttons
            toggleButton7.setChecked(true);
        } else if (receivedMessage.equals("g")) {
            // Turn off all toggle buttons
            toggleButton7.setChecked(false);
        } else if (receivedMessage.equals("H")) {
            // Turn on all toggle buttons
            toggleButton8.setChecked(true);
        } else if (receivedMessage.equals("h")) {
            // Turn off all toggle buttons
            toggleButton8.setChecked(false);
        }else if (receivedMessage.equals("J")) {
            // Turn on all toggle buttons
            toggleButton9.setChecked(true);
        } else if (receivedMessage.equals("j")) {
            // Turn off all toggle buttons
            toggleButton9.setChecked(false);
        } else {
            // Invalid message received
//            Toast.makeText(BluetoothComm.this, "Invalid message received", Toast.LENGTH_SHORT).show();
            // Iterate over each character of the received message
            if (receivedMessage != null && receivedMessage.length() > 0) {
                for (int i = 0; i < 9; i++) {
                    // Get the character at the current position
                    char toggleStateChar = receivedMessage.charAt(i);

                    // Determine the corresponding toggle button
                    ToggleButton toggleButton = null;
                    switch (i) {
                        case 0:
                            toggleButton = toggleButton1;
                            break;
                        case 1:
                            toggleButton = toggleButton2;
                            break;
                        case 2:
                            toggleButton = toggleButton3;
                            break;
                        case 3:
                            toggleButton = toggleButton4;
                            break;
                        case 4:
                            toggleButton = toggleButton5;
                            break;
                        case 5:
                            toggleButton = toggleButton6;
                            break;
                        case 6:
                            toggleButton = toggleButton7;
                            break;
                        case 7:
                            toggleButton = toggleButton8;
                            break;
                        case 8:
                            toggleButton = toggleButton9;
                            break;
                    }

                    // Set the state of the toggle button based on the character
                    if (toggleButton != null) {
                        boolean toggleState = toggleStateChar == '1'; // Assuming '1' represents true and '0' represents false
                        toggleButton.setChecked(toggleState);
                    }
                }
            } else {
                Log.e(TAG, "The string is empty or null.");
            }
        }
    }

    private void disconnectDevice() {
        // Check if the socket is currently connected
        disableToggleButtons();
        uncheckButton();
        if (socket != null && socket.isConnected()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "No device is currently connected", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("MissingPermission")
    private void enableBluetooth() {
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                // Bluetooth is not enabled, request to enable it
                Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
            } else {
                // Bluetooth is already enabled
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    checkBluetoothPermission();
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void checkBluetoothPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH
            }, REQUEST_BLUETOOTH_PERMISSION);
        } else {
            // Permission already granted, proceed with Bluetooth functionality
            startDiscovery();
        }
    }

    @SuppressLint("MissingPermission")
    private void startDiscovery() {
        if (bluetoothAdapter == null) {
            // Bluetooth is not supported on this device
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        bluetoothAdapter.startDiscovery();
        Toast.makeText(this, "Discovering devices...", Toast.LENGTH_SHORT).show();

        // Clear the list before discovering new devices
        discoveredDevicesList.clear();

        // Get the bonded devices
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            discoveredDevicesList.add(device.getName() + "\n" + device.getAddress() + " (Paired)" + "\n");
        }

        // Show the dialog with the discovered devices
        showDeviceListDialog(discoveredDevicesList);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // Bluetooth is enabled
                Toast.makeText(this, "Bluetooth is enabled", Toast.LENGTH_SHORT).show();
            } else {
                // User denied enabling Bluetooth or an error occurred
                Toast.makeText(this, "Bluetooth enabling denied or error occurred", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with Bluetooth functionality
                Toast.makeText(this, "Bluetooth permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied, inform the user
                Toast.makeText(this, "Bluetooth permissions denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showDeviceListDialog(final ArrayList<String> devices) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Available Bluetooth Devices");

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, devices);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String deviceInfo = devices.get(which);
                String[] deviceParts = deviceInfo.split("\n");
                if (deviceParts.length >= 2) { // Check if array has at least 2 elements
                    String deviceAddress = deviceParts[1].replace(" (Paired)", ""); // Remove "(Paired)" text
                    connectToDevice(deviceAddress);
                } else {
                    // Handle case where device address cannot be extracted
                    Toast.makeText(BluetoothComm.this, "Invalid device information", Toast.LENGTH_SHORT).show();
                }
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private static final int CONNECTION_TIMEOUT = 3000; // 3 seconds

    // Create a HandlerThread for timeout handling
    private HandlerThread timeoutHandlerThread = new HandlerThread("TimeoutHandler");
    private Handler timeoutHandler;

    private Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (socket != null && !socket.isConnected()) {
                closeSocket();
            }
        }
    };

    @SuppressLint("MissingPermission")
    private void connectToDevice(String deviceAddress) {
        // Check if Bluetooth is available
        if (bluetoothAdapter == null) {
            showToast("Bluetooth is not supported on this device");
            return;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);

        if (device == null) {
            showToast("Device not found");
            return;
        }

        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

            try {
                // Attempt to create a socket and connect to the device
                socket = device.createRfcommSocketToServiceRecord(uuid);

                // Start the timeout timer
                timeoutHandler.postDelayed(timeoutRunnable, CONNECTION_TIMEOUT);

                socket.connect(); // This may throw IOException

                // If the connection is successful, proceed with data communication and remove timeout task
                timeoutHandler.removeCallbacks(timeoutRunnable);
                // ... rest of your successful connection logic ...
                try {
                    socket.getOutputStream().write('I');
                    // Flush the OutputStream to ensure the character is sent immediately
                    socket.getOutputStream().flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                showToast("Connected to device: " + device.getName());
                connectionStatusImageView.setImageResource(R.drawable.bluetooth);
                startDataReceiving();
                saveLastConnectedDeviceAddress(deviceAddress);
                enableToggleButtons();
            } catch (IOException e) {
                // Handle connection errors
                closeSocket();
                disableToggleButtons();
                Log.e("ConnectionError", "Failed to connect to device: " + device.getName(), e);
                showToast("Failed to connect to device: " + device.getName() + ". Please try again.");
            }
    }

    private void enableToggleButtons() {
        runOnUiThread(() -> {
            toggleButton1.setEnabled(true);
            toggleButton2.setEnabled(true);
            toggleButton3.setEnabled(true);
            toggleButton4.setEnabled(true);
            toggleButton5.setEnabled(true);
            toggleButton6.setEnabled(true);
            toggleButton7.setEnabled(true);
            toggleButton8.setEnabled(true);
            toggleButton9.setEnabled(true);
        });
    }



    private void closeSocket() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e("ConnectionError", "Error while closing socket", e);
                e.printStackTrace();
            }
            socket = null;
        }
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    private void startDataReceiving() {
        ConnectedThread connectedThread = new ConnectedThread(socket);
        connectedThread.start();
        receivingData = true;
    }
    private void saveLastConnectedDeviceAddress(String address) {
        // Save the last connected device address to SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("bluetooth_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(LAST_CONNECTED_DEVICE_ADDRESS_KEY, address);
        editor.apply();
    }

    private void sendCharacter(char character) {
        if (socket != null && socket.isConnected()) {
            try {
                // Write the character to the OutputStream of the socket
                socket.getOutputStream().write(character);
                // Flush the OutputStream to ensure the character is sent immediately
                socket.getOutputStream().flush();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to send", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No device is currently connected", Toast.LENGTH_SHORT).show();
        }
    }


    @SuppressLint("ResourceType")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.id.imageView1, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // Handle settings menu item click
            showPopupWindow();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private void showPopupWindow() {
        // Inflate the popup window layout
        @SuppressLint("ResourceType") View popupView = getLayoutInflater().inflate(R.menu.popup_menu, null);

        // Create a PopupWindow object
        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        // Set an elevation value for the popup window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.setElevation(20);
        }

        // Set a dismiss listener for the popup window
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                // Perform actions when the popup window is dismissed
            }
        });

        // Show the popup window
        popupWindow.showAtLocation(popupView, Gravity.TOP | Gravity.END, 16, 16);
    }

//    @Override
//    public void onStop () {
//        super.onStop();
//        closeSocket();
//    }
}
