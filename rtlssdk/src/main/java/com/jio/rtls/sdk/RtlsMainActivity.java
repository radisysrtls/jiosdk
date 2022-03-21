package com.jio.rtls.sdk;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.jio.rtls.sdk.model.BluetoothDeviceDetails;
import java.util.ArrayList;
import com.radisys.rtls.model.CellData;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.N)
public class RtlsMainActivity extends AppCompatActivity {

    private BluetoothAdapter adapter;
    private List<BluetoothDeviceDetails> bluetoothDeviceDetailsList;
    private WifiManager wifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rtls_main);
        adapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothDeviceDetailsList = new ArrayList<>();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiScanReceiver, intentFilter);
    }

    public void startBluetoothScan() {
        adapter.startDiscovery();
    }

    public void startWifiScan() {
        boolean success = wifiManager.startScan();
        if (!success) {
            scanFailure();
        }
    }

    private void turnOnWifi() {
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(getApplicationContext(), "Turning WiFi ON...", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                BluetoothDeviceDetails bluetoothDeviceDetails = new BluetoothDeviceDetails();
                String deviceName = device.getName();
                if (deviceName != null) {
                    bluetoothDeviceDetails.setDeviceName(deviceName);
                    bluetoothDeviceDetails.setDeviceAddress(device.getAddress());
                    int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                    bluetoothDeviceDetails.setDeviceRSSI(rssi);
                    bluetoothDeviceDetailsList.add(bluetoothDeviceDetails);
                    Log.d("BT device details", deviceName + " --> " + device.getAddress());
                }
            }
        }
    };

    BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
            if (success) {
                scanSuccess();
            } else {
                scanFailure();
            }
        }
    };

    private List<ScanResult> scanSuccess() {
        List<ScanResult> results = wifiManager.getScanResults();
        for (ScanResult scanResult : results) {
            Log.d("Wifi Details ", scanResult.SSID);
        }
        return results;
    }

    private List<ScanResult> scanFailure() {
        List<ScanResult> results = wifiManager.getScanResults();
        return results;
    }

    public List<BluetoothDeviceDetails> getBluetoothScannedDeviceList() {
        List<BluetoothDeviceDetails> mList = bluetoothDeviceDetailsList;
        bluetoothDeviceDetailsList.clear();
        return mList;
    }

    public List<CellData> getCellData() {
        TelephonyManager m_telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        List<CellInfo> cellLocation = m_telephonyManager.getAllCellInfo();
        List<CellData> cellDataList = new ArrayList<>();
        try {
            if (cellLocation != null) {
                for (CellInfo info : cellLocation) {
                    if (info instanceof CellInfoLte) {
                        CellSignalStrengthLte lte = ((CellInfoLte) info).getCellSignalStrength();
                        CellIdentityLte identityLte = ((CellInfoLte) info).getCellIdentity();
                        CellData cellData = setLteScanResult(lte, identityLte);
                        if (cellData != null)
                            cellDataList.add(cellData);
                    }
                    if (info instanceof CellInfoGsm) {
                        CellSignalStrengthGsm gsm = ((CellInfoGsm) info).getCellSignalStrength();
                        CellIdentityGsm identityGsm = ((CellInfoGsm) info).getCellIdentity();
                        CellData cellData = setGsmScannedCells(gsm, identityGsm);
                        if (cellData != null)
                            cellDataList.add(cellData);
                    }
                    if (info instanceof CellInfoWcdma) {
                        CellSignalStrengthWcdma wcdmaSignal = ((CellInfoWcdma) info).getCellSignalStrength();
                        CellIdentityWcdma wcdmaIdentity = ((CellInfoWcdma) info).getCellIdentity();
                        CellData cellData = setWcdmaScannedCells(wcdmaSignal, wcdmaIdentity);
                        if (cellData != null)
                            cellDataList.add(cellData);
                    }
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return cellDataList;
    }

    private CellData setLteScanResult(CellSignalStrengthLte lte, CellIdentityLte identityLte) {
        CellData cellData = null;
        if (identityLte.getCi() > 0) {
            cellData = new CellData();
            cellData.setNetworkType("lte");
            cellData.setCellId(identityLte.getCi());
            // MCC
            int mcc = identityLte.getMcc();
            if (mcc > 9 & mcc < 1000)
                cellData.setMcc(mcc);
            // MNC
            int mnc = identityLte.getMnc();
            if (mnc > 9 & mnc < 1000)
                cellData.setMnc(mnc);
            // Tac
            int tac = identityLte.getTac();
            if (tac >= 0 && tac <= 65536) {
                cellData.setTac(tac);
            }
            // RSSI
            int rssi = lte.getDbm();
            if (rssi >= -150 && rssi <= 0)
                cellData.setRssi(rssi);
            // Frequency
            int frequency = identityLte.getEarfcn();
            if (frequency >= 1 && frequency <= 99999999)
                cellData.setFrequency(frequency);
        }
        return cellData;
    }

    private CellData setGsmScannedCells(CellSignalStrengthGsm gsm, CellIdentityGsm identityGsm) {
        CellData cellData = null;
        if (identityGsm.getCid() > 0) {
            cellData = new CellData();
            cellData.setNetworkType("gsm");
            cellData.setCellId(identityGsm.getCid());
            int mcc = identityGsm.getMcc();
            if (mcc > 9 & mcc < 1000)
                cellData.setMcc(mcc);
            int mnc = identityGsm.getMnc();
            if (mnc > 9 & mnc < 1000)
                cellData.setMnc(mnc);
            int lac = identityGsm.getLac();
            if (lac >= 0 && lac <= 65535)
                cellData.setLac(lac);
            // RSSI
            int rssi = gsm.getDbm();
            if (rssi >= -120 && rssi <= -25)
                cellData.setRssi(rssi);
        }
        return cellData;
    }

    private CellData setWcdmaScannedCells(CellSignalStrengthWcdma wcdma, CellIdentityWcdma identityWcdma) {
        CellData cellData = null;
        if (identityWcdma.getCid() > 0) {
            cellData = new CellData();
            cellData.setNetworkType("wcdma");
            cellData.setCellId(identityWcdma.getCid());
            int lac = identityWcdma.getLac();
            if (lac >= 0 && lac <= 65535)
                cellData.setLac(lac);
            int mcc = identityWcdma.getMcc();
            if (mcc > 9 & mcc < 1000)
                cellData.setMcc(mcc);
            int mnc = identityWcdma.getMnc();
            if (mnc > 9 & mnc < 1000)
                cellData.setMnc(mnc);
            int rssi = wcdma.getDbm();
            if (rssi >= -130 && rssi <= -25)
                cellData.setRssi(rssi);
        }
        return cellData;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(receiver);
        } catch (Exception e) {
            Log.e("LocationFetch Service", "Receiver error");
        }
    }
}