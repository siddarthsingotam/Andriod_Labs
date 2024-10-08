package com.example.lab13

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lab13.ui.theme.Andriod_LabsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.livedata.observeAsState

class MyViewModel : ViewModel() {
    companion object GattAttributes {
        const val SCAN_PERIOD: Long = 3000
        val HEART_RATE_SERVICE_UUID = java.util.UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_MEASUREMENT_UUID = java.util.UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb")
        val CLIENT_CHARACTERISTIC_CONFIG_UUID = java.util.UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    val scanResults = MutableLiveData<List<ScanResult>>(null)
    val fScanning = MutableLiveData<Boolean>(false)
    private val mResults = HashMap<String, ScanResult>()
    private var bluetoothGatt: BluetoothGatt? = null

    fun scanDevices(scanner: BluetoothLeScanner, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                try {
                    fScanning.postValue(true)
                    val settings = ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .setReportDelay(0)
                        .build()
                    scanner.startScan(null, settings, listScanCallback)
                    delay(SCAN_PERIOD)
                    scanner.stopScan(listScanCallback)
                    scanResults.postValue(mResults.values.toList())
                    fScanning.postValue(false)
                } catch (e: SecurityException) {
                    Log.e("DBG", "Permission not granted for Bluetooth scan", e)
                }
            } else {
                Log.e("DBG", "Bluetooth scan permission not granted")
            }
        }
    }

    fun connectToDevice(device: BluetoothDevice, context: Context) {
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            bluetoothGatt = device.connectGatt(context, false, gattCallback(context))
        } else {
            Log.e("DBG", "BLUETOOTH_CONNECT permission not granted")
        }
    }

    private fun gattCallback(context: Context) = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.i("DBG", "Connected to GATT server.")
                        try {
                            gatt.discoverServices()
                        } catch (e: SecurityException) {
                            Log.e("DBG", "Permission not granted for discoverServices", e)
                        }
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.i("DBG", "Disconnected from GATT server.")
                        bluetoothGatt?.close()
                        bluetoothGatt = null
                    }
                }
            } else {
                Log.e("DBG", "BLUETOOTH_CONNECT permission not granted")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val heartRateService = gatt.getService(HEART_RATE_SERVICE_UUID)
                val heartRateMeasurementCharacteristic = heartRateService?.getCharacteristic(HEART_RATE_MEASUREMENT_UUID)

                heartRateMeasurementCharacteristic?.let { characteristic ->
                    if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        gatt.setCharacteristicNotification(characteristic, true)

                        // Enable notifications
                        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(descriptor)
                    } else {
                        Log.e("DBG", "BLUETOOTH_CONNECT permission not granted")
                    }
                }
            } else {
                Log.w("DBG", "onServicesDiscovered received: $status")
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == HEART_RATE_MEASUREMENT_UUID) {
                val bpm = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1)
                Log.d("DBG", "BPM: $bpm")
                // Here you could post the value to LiveData if needed
            }
        }
    }

    val listScanCallback: ScanCallback = object : ScanCallback() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device: BluetoothDevice = result.device
            val deviceAddress = device.address
            mResults[deviceAddress] = result
            Log.d("DBG", "Device address: $deviceAddress (${result.isConnectable})")
        }
    }
}

class MainActivity : ComponentActivity() {
    private var mBluetoothAdapter: BluetoothAdapter? = null

    @RequiresApi(Build.VERSION_CODES.S)
    private fun hasPermissions(): Boolean {
        if (mBluetoothAdapter == null || !mBluetoothAdapter!!.isEnabled) {
            Log.e("DBG", "No Bluetooth LE capability")
            return false
        } else if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADMIN), 1)
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter

        setContent {
            Andriod_LabsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        if (hasPermissions()) {
                            if (mBluetoothAdapter == null) {
                                Text("Bluetooth is not supported on this device")
                            } else if (!mBluetoothAdapter!!.isEnabled) {
                                Text("Bluetooth is turned OFF")
                            } else {
                                ShowDevices(mBluetoothAdapter!!)
                            }
                        } else {
                            Text("Please grant Bluetooth permissions.")
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ShowDevices(mBluetoothAdapter: BluetoothAdapter, model: MyViewModel = viewModel()) {
    val context = LocalContext.current
    val value: List<ScanResult>? by model.scanResults.observeAsState(null)
    val fScanning: Boolean by model.fScanning.observeAsState(false)

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { model.scanDevices(mBluetoothAdapter.bluetoothLeScanner, context) },
            enabled = !fScanning,
            modifier = Modifier.padding(8.dp).height(64.dp).width(144.dp)
        ) {
            Text(if (fScanning) "Scanning" else "Scan Now")
        }

        if (value.isNullOrEmpty()) {
            Text(text = "No devices found", modifier = Modifier.padding(8.dp))
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(2.dp)) {
                    items(value ?: emptyList()) { result ->
                        val deviceName = if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            result.device?.name ?: "UNKNOWN"
                        } else {
                            "Permission Required"
                        }
                        val deviceAddress = result.device.address
                        val deviceStrength = result.rssi

                        Row(
                            modifier = Modifier.padding(2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${deviceStrength}dBm",
                                modifier = Modifier.padding(end = 10.dp).align(Alignment.CenterVertically)
                            )

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = deviceName,
                                    modifier = Modifier.padding(4.dp),
                                    maxLines = 1
                                )
                                Text(text = deviceAddress)
                            }

                            Button(
                                enabled = result.isConnectable,
                                onClick = {
                                    model.connectToDevice(result.device, context)
                                    Log.d("DBG", "Clicked ${result.device.address} ${deviceName}")
                                },
                                modifier = Modifier.padding(8.dp).align(Alignment.CenterVertically)
                            ) {
                                Text("Connect")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ShowDevicesPreview() {
    Andriod_LabsTheme {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Preview of Bluetooth Devices")
        }
    }
}