package com.datalogger;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.BLUETOOTH;
import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_SCAN;
import static android.os.Build.VERSION.SDK_INT;

import static com.datalogger.utilis.Utility.my_UUID;
import static java.lang.Thread.sleep;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.datalogger.adapter.PairedDeviceAdapter;
import com.datalogger.model.PairDeviceModel;
import com.datalogger.utilis.Utility;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements PairedDeviceAdapter.deviceSelectionListener {

    private static final int REQUEST_CODE_PERMISSION = 1;
    ArrayList<PairDeviceModel> pairedDeviceList = new ArrayList<>();
    List<String> mMonthHeaderList = new ArrayList<>();
    RecyclerView bluetoothDeviceList;
    TextView bluetoothState;
    PairedDeviceAdapter pairedDeviceAdapter;

    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    int kk = 0, mmCount = 0, positionFinal = 0, checkFirstTimeOlineStatus = 0,mLengthCount=0,selectedIndex;
    BluetoothSocket bluetoothSocket;
    boolean boolFlag = false;
    String RMS_DEBUG_EXTRN = "", SS = "";

    private InputStream iStream = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Init();


    }

    private void Init() {
        bluetoothState = findViewById(R.id.bluetoothState);
        bluetoothDeviceList = findViewById(R.id.bluetoothDeviceList);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermission()) {
            checkGpsEnabled();
        } else {
            requestPermission();
        }
    }


    private boolean checkPermission() {
        int FineLocation = ContextCompat.checkSelfPermission(getApplicationContext(), ACCESS_FINE_LOCATION);
        int CoarseLocation = ContextCompat.checkSelfPermission(getApplicationContext(), ACCESS_COARSE_LOCATION);
        int Bluetooth = ContextCompat.checkSelfPermission(getApplicationContext(), BLUETOOTH);
        int BluetoothConnect = ContextCompat.checkSelfPermission(getApplicationContext(), BLUETOOTH_CONNECT);
        int BluetoothScan = ContextCompat.checkSelfPermission(getApplicationContext(), BLUETOOTH_SCAN);

        return FineLocation == PackageManager.PERMISSION_GRANTED && CoarseLocation == PackageManager.PERMISSION_GRANTED
                && Bluetooth == PackageManager.PERMISSION_GRANTED && BluetoothConnect == PackageManager.PERMISSION_GRANTED
                && BluetoothScan == PackageManager.PERMISSION_GRANTED;

    }

    private void requestPermission() {
        if (SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN},
                    REQUEST_CODE_PERMISSION);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.BLUETOOTH},
                    REQUEST_CODE_PERMISSION);
        }
    }


    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION:
                if (grantResults.length > 0) {

                    if (SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        boolean CoarseLocation = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                        boolean FineLocation = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                        boolean BluetoothConnect = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                        boolean BluetoothScan = grantResults[3] == PackageManager.PERMISSION_GRANTED;

                        if (CoarseLocation && FineLocation && BluetoothConnect && BluetoothScan) {
                            checkGpsEnabled();

                        } else {
                            requestPermission();
                        }
                    } else {

                        boolean FineLocation = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                        boolean CoarseLocation = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                        boolean Bluetooth = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                        if (FineLocation && CoarseLocation && Bluetooth) {
                            checkGpsEnabled();
                        } else {
                            requestPermission();
                        }
                    }

                }

        }
    }

    private void checkGpsEnabled() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (!gps_enabled && !network_enabled) {
            // notify user
            new AlertDialog.Builder(this)
                    .setTitle(R.string.location_permission)
                    .setMessage(R.string.gps_network_not_enabled)
                    .setPositiveButton(R.string.settings, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton(R.string.Cancel, null)
                    .show();
        } else {
            registerBroadcastManager();
        }
    }


    private final BroadcastReceiver mBluetoothStatusChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        bluetoothDeviceOff();
                        break;

                    case BluetoothAdapter.STATE_ON:
                        getPairedDeviceList();
                        break;

                }

            }
        }
    };

    private void bluetoothDeviceOff() {
        bluetoothDeviceList.setVisibility(View.GONE);
        bluetoothState.setVisibility(View.VISIBLE);
        bluetoothState.setText(getResources().getString(R.string.stateoff));
    }


    private void registerBroadcastManager() {
        IntentFilter filter1 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetoothStatusChangedReceiver, filter1);

        if (Utility.isBluetoothEnabled()) {
            bluetoothState.setVisibility(View.GONE);
            getPairedDeviceList();

        } else {
            bluetoothDeviceOff();
        }


    }

    private void getPairedDeviceList() {
        if (ActivityCompat.checkSelfPermission(this, BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        pairedDeviceList = new ArrayList<PairDeviceModel>();
        if (pairedDevices.size() > 0) {
            bluetoothState.setVisibility(View.GONE);
            bluetoothDeviceList.setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                PairDeviceModel pairDeviceModel = new PairDeviceModel();
                pairDeviceModel.setDeviceName(device.getName());
                pairDeviceModel.setDeviceAddress(device.getAddress());
                pairedDeviceList.add(pairDeviceModel);

            }
            pairedDeviceAdapter = new PairedDeviceAdapter(this, pairedDeviceList);
            bluetoothDeviceList.setHasFixedSize(true);
            bluetoothDeviceList.setAdapter(pairedDeviceAdapter);
            pairedDeviceAdapter.deviceSelection(this);


        } else {
            bluetoothDeviceList.setVisibility(View.GONE);
            bluetoothState.setVisibility(View.VISIBLE);
            bluetoothState.setText(getResources().getString(R.string.nodatafound));

        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothStatusChangedReceiver != null) {
            unregisterReceiver(mBluetoothStatusChangedReceiver);
        }

    }

    @Override
    public void DeviceSelectionListener(PairDeviceModel pairDeviceModel, int position) {
        selectedIndex = position;
        Log.e("MethodStart","true");
        DataExtract();
    }

    private void DataExtract() {
        kk = 0;
        mmCount = 0;
        boolFlag = false;
        positionFinal = 0;
        RMS_DEBUG_EXTRN = "ONLINE FROM DEBUG";
        checkFirstTimeOlineStatus = 1;
        mMonthHeaderList = new ArrayList<>();
        new BluetoothCommunicationGetMonthParameter().execute(":MLENGTH#", ":MDATA#", "START");
    }


    private void connectWithSocket() throws IOException {

        bluetoothSocket = null;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
        BluetoothDevice dispositivo = bluetoothAdapter.getRemoteDevice(pairedDeviceList.get(selectedIndex).getDeviceAddress());//connects to the device's address and checks if it's available
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        bluetoothSocket = dispositivo.createRfcommSocketToServiceRecord(my_UUID);//create a RFCOMM (SPP) connection
        bluetoothAdapter.cancelDiscovery();
        bluetoothSocket.connect();
    }


    private class BluetoothCommunicationGetMonthParameter extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            bluetoothSocket = null;
           // Utility.showProgressDialogue(getApplicationContext());
        }


        @Override
        protected Boolean doInBackground(String... requests) {
            try {
                if (bluetoothSocket != null) {
                    if (!bluetoothSocket.isConnected()) {
                        Log.e("connectWithSocket","true");
                        connectWithSocket();
                    }
                } else {
                    connectWithSocket();
                }

                if (bluetoothSocket.isConnected()) {
                    Log.e("bluetoothSocket", "Connected");
                    byte[] STARTRequest = requests[0].getBytes(StandardCharsets.US_ASCII);
                    bluetoothSocket.getOutputStream().write(STARTRequest);
                    sleep(1000);
                    iStream = bluetoothSocket.getInputStream();


                    while (iStream.available() > 0) {
                        SS += (char) iStream.read();
                    }
                    if (!SS.isEmpty()) {
                        String SSS = SS.replace(",", "VIKASGOTHI");

                        String[] mS = SSS.split("VIKASGOTHI");

                        Log.e("SS1234==========>", SS);
                        Log.e("SSS5678==========>", SSS);
                        Log.e("mS9010==========>", Arrays.toString(mS));
                        Log.e("mS_length==========>", String.valueOf(mS.length));
                        if (mS.length > 0) {

                            for (int i = 0; i < mS.length; i++) {

                                System.out.println("mSmSmS====>>" + mS[i]);

                                if (!mS[i].trim().equalsIgnoreCase("")) {
                                    if (i == 0) {
                                        //mLengthCount = Integer.parseInt(mS[i]);
                                        mLengthCount = Integer.valueOf(mS[i]);
                                    } else {
                                        mMonthHeaderList.add(mS[i]);
                                    }
                                }
                                Log.e("mLengthCount==========>", String.valueOf(mLengthCount));

                                Log.e("mMonthHeaderList==========>", String.valueOf(mMonthHeaderList.size()));

                                //    headerLenghtMonth = "" + mMonthHeaderList.size();
                            }

                        /*if (mS.length > 0) {

                            for (int i = 0; i < mS.length; i++) {

                                System.out.println("mSmSmS====>>" + mS[i]);

                                if (!mS[i].trim().equalsIgnoreCase("")) {
                                    if (i == 0) {
                                        //mLengthCount = Integer.parseInt(mS[i]);
                                        mLengthCount = Integer.valueOf(mS[i]);
                                    } else {
                                        mMonthHeaderList.add(mS[i]);
                                    }
                                }
                                headerLenghtMonth = "" + mMonthHeaderList.size();
                            }

                            System.out.println("headerLenghtMonth==>> " + headerLenghtMonth);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    new BluetoothCommunicationForFirstActivity().execute(":MDATA#", ":MDATA#", "START");
                                }
                            });

                        }*/

                        }
                    }
                }


            }catch (Exception e){
                e.printStackTrace();
            }

          return  false;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            }
        }



}