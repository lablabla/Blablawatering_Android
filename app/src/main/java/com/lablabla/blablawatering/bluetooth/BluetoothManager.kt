package com.lablabla.blablawatering.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity
import com.google.gson.Gson
import com.lablabla.blablawatering.model.Station
import com.lablabla.blablawatering.model.TimeMessage
import com.lablabla.blablawatering.util.fromJson
import com.lablabla.blablawatering.util.isReadable
import com.lablabla.blablawatering.util.isWritable
import java.time.Instant
import java.util.*
import javax.inject.Inject

private const val GATT_MAX_MTU_SIZE = 517

@SuppressLint("MissingPermission")
class BluetoothManager @Inject constructor(
    private val activity: FragmentActivity
) {
    private val TAG = "BlablaBluetoothManager"

    private var scanning = false
    private var bluetoothGatt: BluetoothGatt? = null

    private val ADVERTISING_UUID = UUID.fromString("0000abcd-0000-1000-8000-00805f9b34fb")

    private val SERVICE_UUID = UUID.fromString("260bb0ea-6e32-4f94-adf6-b96ebda4c6ce")
    private val GET_STATIONS_UUID = UUID.fromString("1001b0ea-6e32-4f94-adf6-b96ebda4c6ce")
    private val SET_TIME_UUID = UUID.fromString("1005b0ea-6e32-4f94-adf6-b96ebda4c6ce")

    var btCallback: BlablaBTCallback? = null
    var connected: Boolean = false

    lateinit var bluetoothAdapter: BluetoothAdapter

    val gson = Gson()

    init {
        listenToBondStateChanges()
    }

    //region BLE Scanning
    private val bleScanner: BluetoothLeScanner? by lazy {
        if (this::bluetoothAdapter.isInitialized) {
            return@lazy bluetoothAdapter.bluetoothLeScanner
        }
        null
    }

    private val scanFilter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(ADVERTISING_UUID))
        .build()

    @RequiresApi(Build.VERSION_CODES.M)
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
        .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
        .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
        .setReportDelay(0)
        .build()


    fun startScan() {
        if (!scanning) {
            scanning = true
            bleScanner?.startScan(mutableListOf(scanFilter), scanSettings, leScanCallback)
        }
    }

    fun stopScan() {
        if (scanning) {
            scanning = false
            bleScanner?.stopScan(leScanCallback)
        }
    }

    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            val name: String? = result.scanRecord?.deviceName ?: result.device.name
            Log.e(TAG, "Scanning BLE devices found device named ${name}")
            stopScan()
            with(result.device) {
                Log.w(TAG, "Connecting to $address")
                connectGatt(activity, false, gattCallback)
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "Scanning BLE devices failed with error code ${errorCode}")
            stopScan()
        }
    }

    fun getDeviceName(): String? {
        return bluetoothGatt?.device?.name
    }

    fun getDeviceAddress(): String? {
        return bluetoothGatt?.device?.address
    }

    fun sync() {
        Log.d("sync", "syncing")
        bluetoothGatt?.apply {
            if (device.bondState != BluetoothDevice.BOND_BONDED) {
                device.createBond()
            }
            else {
//                updateTime()
                getStations()
            }
        }
    }

    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address
            val deviceName = gatt.device.name
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully connected to $deviceAddress")
                    bluetoothGatt = gatt
                    Handler(Looper.getMainLooper()).post {
                        bluetoothGatt?.discoverServices()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully disconnected from $deviceAddress")
                    gatt.close()

                    connected = false
                    btCallback?.onDeviceDisconnected(deviceName, deviceAddress)

                }
            } else {
                Log.w("BluetoothGattCallback", "Error $status encountered for $deviceAddress! Disconnecting...")
                gatt.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                Log.w("BluetoothGattCallback", "Discovered ${services.size} services for ${device.address}")
                requestMtu(GATT_MAX_MTU_SIZE)
                connected = true
                btCallback?.onDeviceConnected(gatt.device.name, gatt.device.address)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            Log.w("BluetoothGattCallback", "ATT MTU changed to $mtu, success: ${status == BluetoothGatt.GATT_SUCCESS}")

            sync()
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.d("BluetoothGattCallback", "Read characteristic $uuid:\n${value.decodeToString()}")
                        when (uuid) {
                            GET_STATIONS_UUID -> {
                                val utf8String = value.decodeToString()
                                val stations: List<Station> = gson.fromJson(utf8String)
                                btCallback?.onUpdateStations(stations)
                            }
                            else -> {
                                Log.e("BluetoothGattCallback", "Unknown UUID " + uuid)
                            }
                        }
                    }
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                        Log.e("BluetoothGattCallback", "Read not permitted for $uuid!")
                    }
                    else -> {
                        Log.e("BluetoothGattCallback", "Characteristic read failed for $uuid, error: $status")
                    }
                }
            }
        }
    }

    fun listenToBondStateChanges() {
        activity.registerReceiver(
            broadcastReceiver,
            IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        )
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            with(intent) {
                if (action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                    val device = getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val previousBondState = getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1)
                    val bondState = getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                    val bondTransition = "${previousBondState.toBondStateDescription()} to " +
                            bondState.toBondStateDescription()
                    Log.w("Bond state change", "${device?.address} bond state changed | $bondTransition")
                    if (bondState == BluetoothDevice.BOND_BONDED) {
                        sync()
                    }
                }

            }
        }

        private fun Int.toBondStateDescription() = when(this) {
            BluetoothDevice.BOND_BONDED -> "BONDED"
            BluetoothDevice.BOND_BONDING -> "BONDING"
            BluetoothDevice.BOND_NONE -> "NOT BONDED"
            else -> "ERROR: $this"
        }
    }

    private fun updateTime() {
        val calendar = GregorianCalendar()
        val ts = Instant.now().epochSecond
        val timeZone = calendar.timeZone
        val timeMessage = TimeMessage(ts, 0, timeZone.id)
        val setTimeChar = bluetoothGatt?.getService(SERVICE_UUID)?.getCharacteristic(SET_TIME_UUID)
        if (setTimeChar?.isWritable() == true) {
            val timeStr = gson.toJson(timeMessage)
            Log.d("updateTime", "setting time to $timeStr")
            setTimeChar.value = timeStr.toByteArray()
            bluetoothGatt?.writeCharacteristic(setTimeChar)
        }

    }

    private fun getStations() {
        Log.d("getStations", "getting stations")
        val getStationsChar = bluetoothGatt?.getService(SERVICE_UUID)?.getCharacteristic(GET_STATIONS_UUID)
        if (getStationsChar?.isReadable() == true) {
            Log.d("getStations", "reading")
            bluetoothGatt?.readCharacteristic(getStationsChar)
        }
    }
}