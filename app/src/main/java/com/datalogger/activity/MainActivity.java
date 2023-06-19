package com.datalogger.activity;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.BLUETOOTH;
import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_SCAN;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION.SDK_INT;
import static java.lang.Thread.sleep;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.datalogger.R;
import com.datalogger.adapter.PairedDeviceAdapter;
import com.datalogger.model.PairDeviceModel;
import com.datalogger.utilis.Utility;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements PairedDeviceAdapter.deviceSelectionListener {
    public static UUID my_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_CODE_PERMISSION = 1;

    private static Workbook wb = null;
    private static boolean success = false;

    private static Sheet sheet1 = null;
    private static Row row;
    private static Cell cell = null;

    private static final CellStyle cellStyle = null;

    ArrayList<PairDeviceModel> pairedDeviceList = new ArrayList<>();
    private List<String> mMonthHeaderList = new ArrayList<>();
    RecyclerView bluetoothDeviceList;
    TextView bluetoothState;
    BluetoothSocket bluetoothSocket;
    int mLengthCount, selectedIndex = 0;
    String SS = "", headerLenghtMonth = "",filePath="";
    private InputStream iStream = null;

    int  kk = 0,  mDay = 0,mMonth = 0, mYear = 0, mHour = 0, mMinut = 0, mStatus = 0, mRPM = 0, mFault = 0,mPostionFinal = 0,bytesRead=0;
    float fFrequency = 0;
    int[] mTotalTime;

    boolean mBoolflag = false;



    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Init();
        if (!checkPermission()) {
            requestPermission();
        }else {
            registerBroadcastManager();
        }
    }

    private void getPairedDeviceList() {
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth Not Supported", Toast.LENGTH_SHORT).show();
        } else {
            if (SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
            }
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {

                for (BluetoothDevice device : pairedDevices) {
                    pairedDeviceList.add(new PairDeviceModel(device.getName(), device.getAddress(), false));
                }
                bluetoothDeviceList.setVisibility(View.VISIBLE);
                bluetoothState.setVisibility(View.GONE);
                PairedDeviceAdapter pairedDeviceAdapter = new PairedDeviceAdapter(this, pairedDeviceList);
                bluetoothDeviceList.setAdapter(pairedDeviceAdapter);
                pairedDeviceAdapter.deviceSelection(this);
            } else {
                bluetoothDeviceOff();
                Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void Init() {
        bluetoothDeviceList = findViewById(R.id.bluetoothDeviceList);
        bluetoothState = findViewById(R.id.bluetoothState);
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
        bluetoothState.setText(getResources().getString(R.string.bluetooth_is_off));
        pairedDeviceList = new ArrayList<>();
    }

    private void registerBroadcastManager() {
        IntentFilter filter1 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetoothStatusChangedReceiver, filter1);

        Log.e("BLuetoothEnabled====>","true");
        if (Utility.isBluetoothEnabled()) {
            bluetoothState.setVisibility(View.GONE);
            getPairedDeviceList();

        } else {
            bluetoothDeviceOff();
        }

    }

    private boolean checkPermission() {
        int FineLocation = ContextCompat.checkSelfPermission(getApplicationContext(), ACCESS_FINE_LOCATION);
        int CoarseLocation = ContextCompat.checkSelfPermission(getApplicationContext(), ACCESS_COARSE_LOCATION);
        int Bluetooth = ContextCompat.checkSelfPermission(getApplicationContext(), BLUETOOTH);
        int BluetoothConnect = ContextCompat.checkSelfPermission(getApplicationContext(), BLUETOOTH_CONNECT);
        int BluetoothScan = ContextCompat.checkSelfPermission(getApplicationContext(), BLUETOOTH_SCAN);
        int ReadExternalStorage = ContextCompat.checkSelfPermission(getApplicationContext(), READ_EXTERNAL_STORAGE);
        int WriteExternalStorage = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);

        if(SDK_INT>=Build.VERSION_CODES.S){
            return FineLocation == PackageManager.PERMISSION_GRANTED && CoarseLocation == PackageManager.PERMISSION_GRANTED
                    && Bluetooth == PackageManager.PERMISSION_GRANTED && BluetoothConnect == PackageManager.PERMISSION_GRANTED
                    && BluetoothScan == PackageManager.PERMISSION_GRANTED;
        }else {
            return FineLocation == PackageManager.PERMISSION_GRANTED && CoarseLocation == PackageManager.PERMISSION_GRANTED
                    && Bluetooth == PackageManager.PERMISSION_GRANTED && ReadExternalStorage == PackageManager.PERMISSION_GRANTED
                    && WriteExternalStorage == PackageManager.PERMISSION_GRANTED;

        }

    }

    private void requestPermission() {
        if (SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this,
                    new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION,
                            BLUETOOTH_CONNECT, BLUETOOTH_SCAN},
                    REQUEST_CODE_PERMISSION);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, BLUETOOTH,READ_EXTERNAL_STORAGE,WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_PERMISSION);
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.length > 0) {

                if (SDK_INT >= Build.VERSION_CODES.S) {
                    boolean CoarseLocation = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean FineLocation = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean BluetoothConnect = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                    boolean BluetoothScan = grantResults[3] == PackageManager.PERMISSION_GRANTED;

                   Log.e("CoarseLocation",String.valueOf(CoarseLocation));
                    Log.e("FineLocation",String.valueOf(FineLocation));
                    Log.e("BluetoothConnect",String.valueOf(BluetoothConnect));
                    Log.e("BluetoothScan",String.valueOf(BluetoothScan));
                    if (CoarseLocation && FineLocation && BluetoothConnect && BluetoothScan) {
                        registerBroadcastManager();
                    } else {
                        requestPermission();
                    }
                } else {

                    boolean FineLocation = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean CoarseLocation = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean Bluetooth = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                    boolean ReadExternalStorage = grantResults[3] == PackageManager.PERMISSION_GRANTED;
                    boolean WriteExternalStorage = grantResults[4] == PackageManager.PERMISSION_GRANTED;

                    Log.e("CoarseLocation",String.valueOf(CoarseLocation));
                    Log.e("FineLocation",String.valueOf(FineLocation));
                    Log.e("Bluetooth",String.valueOf(Bluetooth));
                    Log.e("ReadExternalStorage",String.valueOf(ReadExternalStorage));
                    Log.e("WriteExternalStorage",String.valueOf(WriteExternalStorage));
                    if (FineLocation && CoarseLocation && Bluetooth && ReadExternalStorage && WriteExternalStorage) {
                        registerBroadcastManager();
                    } else {
                        requestPermission();
                    }
                }

            }
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
        new BluetoothCommunicationGetMonthParameter().execute(":MLENGTH#", ":MDATA#", "START");
    }

    private class BluetoothCommunicationGetMonthParameter extends AsyncTask<String, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Utility.showProgressDialogue(MainActivity.this);


        }


        @Override
        protected Boolean doInBackground(String... requests) //while the progress dialog is shown, the connection is done in background
        {

            try {
                if (bluetoothSocket != null) {
                    if (!bluetoothSocket.isConnected()) {
                        connectToBluetoothSocket();

                    } else {
                        connectToBluetoothSocket();
                    }
                } else {
                    connectToBluetoothSocket();
                }

                if (bluetoothSocket.isConnected()) {

                    byte[] STARTRequest = requests[0].getBytes(StandardCharsets.US_ASCII);
                    bluetoothSocket.getOutputStream().write(STARTRequest);
                    sleep(1000);
                    iStream = bluetoothSocket.getInputStream();


                    while (iStream.available() > 0) {
                        SS += (char) iStream.read();
                    }

                    if (!SS.trim().isEmpty()) {

                        String SSS = SS.replace(",", " ");
                        String[] mS = SSS.split(" ");
                         Log.e("sss====>",SSS);
                        Log.e("sss====>",Arrays.toString(mS));
                        if (mS.length > 0) {

                            for (int i = 0; i < mS.length; i++) {

                                System.out.println("mSmSmS====>>" + mS[i]);

                                if (!mS[i].trim().isEmpty()) {
                                    if (i == 0) {
                                        //mLengthCount = Integer.parseInt(mS[i]);
                                        mLengthCount = Integer.parseInt(mS[i]);
                                    } else {
                                        mMonthHeaderList.add(mS[i]);
                                    }
                                }
                                headerLenghtMonth = "" + mMonthHeaderList.size();
                            }

                            System.out.println("headerLenghtMonth==>> " + headerLenghtMonth);
                      /*   String[] mS = SSS.split("VIKASGOTHI");


                        Log.e("SSS=====>", SSS);
                        Log.e("mS=====>", Arrays.toString(mS));
                        if (mS.length > 0) {

                            for (int i = 0; i < mS.length; i++) {

                                System.out.println("mSmSmS====>>" + mS[i]);

                                if (!mS[i].trim().isEmpty()) {
                                    if (i == 0) {

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
                                });*/

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    new BluetoothCommunicationForFirstActivity().execute(":MDATA#", ":MDATA#", "START");
                                }
                            });

                        }
                    }
                }

            } catch (Exception e) {
                    Log.e("Exception====>",e.getMessage());
                Utility.hideProgressDialogue();
                return false;
            }

            //  baseRequest.hideLoader();
            return false;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(Boolean result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);
            Utility.hideProgressDialogue();
            if (mMonthHeaderList.size() > 0) {
                new BluetoothCommunicationForFirstActivity().execute(":MDATA#", ":MDATA#", "START");
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new BluetoothCommunicationGetMonthParameter().execute(":MLENGTH#", ":MLENGTH#", "OKAY");
                    }
                });
            }
        }
    }


    private void connectToBluetoothSocket() {
        try {
            bluetoothSocket = null;
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(pairedDeviceList.get(selectedIndex).getDeviceAddress());//connects to the device's address and checks if it's available

            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(my_UUID);//create a RFCOMM (SPP) connection
            bluetoothAdapter.cancelDiscovery();
            if(!bluetoothSocket.isConnected())
            bluetoothSocket.connect();

        }catch (Exception e){
            Utility.hideProgressDialogue();
            runOnUiThread(new Runnable() {
                public void run() {
                    Utility.ShowToast(getResources().getString(R.string.pairedDevice), getApplicationContext());
                }});
            e.printStackTrace();
        }

    }

    private class BluetoothCommunicationForFirstActivity extends AsyncTask<String, Void, Boolean>  // UI thread
    {
        @Override
        protected void onPreExecute() {
        //   Utility.showProgressDialogue(MainActivity.this);
        }
        @Override
        protected Boolean doInBackground(String... requests) {
            try {
                // btSocket.close();
                if (!bluetoothSocket.isConnected())
                    bluetoothSocket.connect();//start connection
                if (bluetoothSocket.isConnected()) {
                    byte[] STARTRequest = requests[0].getBytes(StandardCharsets.US_ASCII);
                    try {
                        bluetoothSocket.getOutputStream().write(STARTRequest);
                        sleep(400);
                        iStream = bluetoothSocket.getInputStream();
                    } catch (InterruptedException e1) {
                        System.out.println("vikas--1==>1");
                        //baseRequest.hideLoader();
                        e1.printStackTrace();
                    }
                    for (int i = 0; i < 12; i++) {
                        try {
                            bytesRead = iStream.read();
                        } catch (IOException e) {
                            System.out.println("vikas--2==>2");
                            //baseRequest.hideLoader();
                            e.printStackTrace();
                        }
                    }
                    int[] bytesReaded;
                    //   while (iStream.available() > 0)
                    while (true) {
                        bytesReaded = new int[mLengthCount];
                        for (int i = 0; i < mLengthCount; i++) {
                            // Character mCharOne = (char) iStream.read();
                            //  Character mCharTwo = (char) iStream.read();
                            int mCharOne = 0;
                            int mCharTwo = 0;
                            try {
                                mCharOne = iStream.read();
                                mCharTwo = iStream.read();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            try {
                                System.out.println("vikas--3==>" + mCharOne + "" + mCharTwo);
                                if ("TX".equalsIgnoreCase((char) mCharOne + "" + (char) mCharTwo)) {

                                   Utility.hideProgressDialogue();
                                    mBoolflag = true;
                                    break;
                                } else {
                                    if (mCharOne == 0 || mCharTwo == 0) {
                                        bytesReaded[i] = Integer.parseInt("" + mCharOne + mCharTwo, 16);// iStream.read();
                                    } else {
                                        bytesReaded[i] = Integer.parseInt("" + (char) mCharOne + (char) mCharTwo, 16);// iStream.read();
                                    }
                                }
                            } catch (NumberFormatException e) {
                                Utility.hideProgressDialogue();
                                System.out.println("vikas--3==>N");
                                e.printStackTrace();
                            }
                        }


                        if (!mBoolflag) {
                            kk++;

                            System.out.println("kk++ ==>> " + kk);
                            mDay = bytesReaded[0];
                            mMonth = bytesReaded[1];
                            mYear = bytesReaded[2];
                            mHour = bytesReaded[3];
                            mMinut = bytesReaded[4];
                            mStatus = bytesReaded[5];

                            mTotalTime = new int[10];

                            mTotalTime[0] = mDay;
                            mTotalTime[1] = mMonth;
                            mTotalTime[2] = mYear;
                            mTotalTime[3] = mHour;
                            mTotalTime[4] = mMinut;
                            mTotalTime[5] = mStatus;

                            int i = 6;
                            // System.out.println("bytesReadednm==>> ");
                            for (int k = 6; k < mLengthCount; k += 2) {
                                mTotalTime = Arrays.copyOf(mTotalTime, i + 1);
                                mTotalTime[i] = bytesReaded[k] << 8;
                                mTotalTime[i] |= bytesReaded[k + 1];

                                i++;
                            }
                        } else {

                            File file = new File(Utility.commonDocumentDirPath("ShaktiDataLoggerFile"), "Year_" + pairedDeviceList.get(selectedIndex).getDeviceName()  + ".xls");

                            FileOutputStream os = null;
                            System.out.println("vikas--4==>4");
                            try {
                                os = new FileOutputStream(file);
                                wb.write(os);
                                Log.w("FileUtils", "Writing file" + file);
                                success = true;
                            } catch (IOException e) {
                                Log.w("FileUtils", "Error writing " + file, e);
                            } catch (Exception e) {
                                Log.w("FileUtils", "Failed to save file", e);
                            } finally {
                                try {
                                    os = new FileOutputStream(file);
                                    wb.write(os);
                                    if (null != os)
                                        os.close();
                                } catch (Exception ex) {
                                    System.out.println("vikas--5==>5");
                                    ex.printStackTrace();
                                }
                            }
                            break;
                        }
                        if (((mDay == 255) && (mMonth == 255) && (mYear == 255)) || ((mDay == 0) && (mMonth == 0) && (mYear == 0))) {

                            File file = new File(Utility.commonDocumentDirPath("ShaktiDataLoggerFile"), "Year_" + pairedDeviceList.get(selectedIndex).getDeviceName()  + ".xls");
                            FileOutputStream os = null;
                            try {
                                os = new FileOutputStream(file);
                                wb.write(os);
                                Log.w("FileUtils", "Writing file" + file);
                                success = true;
                            } catch (IOException e) {
                                Log.w("FileUtils", "Error writing " + file, e);
                            } catch (Exception e) {
                                Log.w("FileUtils", "Failed to save file", e);
                            } finally {
                                try {
                                    os = new FileOutputStream(file);
                                    wb.write(os);
                                    if (null != os)
                                        os.close();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                            mBoolflag = true;
                        } else {
                            if (mPostionFinal == 0) {
                                wb = new HSSFWorkbook();
                                sheet1 = wb.createSheet("myOrder");
                                row = sheet1.createRow(0);

                                for (int k = 0; k < mMonthHeaderList.size(); k++) {
                                    String[] mStringSplitStart = mMonthHeaderList.get(k).split("-");
                                    sheet1.setColumnWidth(k, (10 * 200));
                                    cell = row.createCell(k);
                                    cell.setCellValue(mStringSplitStart[0]);
                                    cell.setCellStyle(cellStyle);
                                }

                                row = sheet1.createRow(mPostionFinal + 1);
                                cell = row.createCell(0);
                                cell.setCellValue("" + mDay);
                                cell.setCellStyle(cellStyle);

                                cell = row.createCell(1);
                                cell.setCellValue("" + mMonth);
                                cell.setCellStyle(cellStyle);

                                cell = row.createCell(2);
                                cell.setCellValue("" + mYear);
                                cell.setCellStyle(cellStyle);

                                cell = row.createCell(3);
                                cell.setCellValue("" + mHour);
                                cell.setCellStyle(cellStyle);

                                cell = row.createCell(4);
                                cell.setCellValue("" + mMinut);
                                cell.setCellStyle(cellStyle);

                                cell = row.createCell(5);
                                cell.setCellValue("" + mStatus);
                                cell.setCellStyle(cellStyle);

                                try {
                                    for (int j = 6; j < mMonthHeaderList.size(); j++) {
                                        String[] mStringSplitStart = mMonthHeaderList.get(j).split("-");
                                        int mmIntt = 1;
                                        mmIntt = Integer.parseInt(mStringSplitStart[1]);
                                        try {
                                            if (mmIntt == 1) {
                                                sheet1.setColumnWidth(j, (10 * 200));
                                                fFrequency = mTotalTime[j];

                                                cell = row.createCell(j);
                                                cell.setCellValue("" + fFrequency);
                                                cell.setCellStyle(cellStyle);
                                            } else {
                                                sheet1.setColumnWidth(j, (10 * 200));
                                                fFrequency = mTotalTime[j];

                                                float mmValue = (((float) mTotalTime[j]) / ((float) mmIntt));
                                                cell = row.createCell(j);
                                                cell.setCellValue("" + mmValue);
                                                cell.setCellStyle(cellStyle);
                                            }

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }


                                    }
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }


                            } else {
                                row = sheet1.createRow(mPostionFinal + 1);

                                cell = row.createCell(0);
                                cell.setCellValue("" + mDay);
                                cell.setCellStyle(cellStyle);

                                cell = row.createCell(1);
                                cell.setCellValue("" + mMonth);
                                cell.setCellStyle(cellStyle);

                                cell = row.createCell(2);
                                cell.setCellValue("" + mYear);
                                cell.setCellStyle(cellStyle);

                                cell = row.createCell(3);
                                cell.setCellValue("" + mHour);
                                cell.setCellStyle(cellStyle);

                                cell = row.createCell(4);
                                cell.setCellValue("" + mMinut);
                                cell.setCellStyle(cellStyle);

                                cell = row.createCell(5);
                                cell.setCellValue("" + mStatus);
                                cell.setCellStyle(cellStyle);


                                try {
                                    //  for (int j = 3; j < mLengthCount; j++)
                                    for (int j = 6; j < mMonthHeaderList.size(); j++) {
                                        //     fTotalEnergy = Float.intBitsToFloat(mDayDataList.get(i)[j]);


                                        String[] mStringSplitStart = mMonthHeaderList.get(j).split("-");
                                        int mmIntt = 1;
                                        mmIntt = Integer.parseInt(mStringSplitStart[1]);

                                        try {

                                            if (mmIntt == 1) {


                                                sheet1.setColumnWidth(j, (10 * 200));
                                                fFrequency = mTotalTime[j];

                                                cell = row.createCell(j);
                                                cell.setCellValue("" + fFrequency);
                                                cell.setCellStyle(cellStyle);

                                                // tr.addView(getTextView(counter, ((mTotalTime[i] / mmIntt)) + "", Color.BLACK, Typeface.NORMAL, ContextCompat.getColor(this, R.color.white)));
                                            } else {


                                                sheet1.setColumnWidth(j, (10 * 200));
                                                fFrequency = mTotalTime[j];

                                                float mmValue = (((float) mTotalTime[j]) / ((float) mmIntt));

                                                cell = row.createCell(j);
                                                // c.setCellValue("" + fFrequency);
                                                cell.setCellValue("" + mmValue);
                                                cell.setCellStyle(cellStyle);

                                                //  tr.addView(getTextView(counter, ( (((float)mTotalTime[i]) / ((float)mmIntt))) + "", Color.BLACK, Typeface.NORMAL, ContextCompat.getColor(this, R.color.white)));
                                            }

                                        } catch (Exception e) {
                                            //   baseRequest.hideLoader();
                                            e.printStackTrace();
                                        }

                                    }
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                    //      baseRequest.hideLoader();
                                }


                            }

                          /*  mvDay = mDay;
                            mvMonth = mMonth;
                            mvYear = mYear;
                            mvHour = "" + mHour;
                            mvMinute = "" + mMinut;
                            mvNo_of_Start = "" + mStatus;
                            fvFrequency = fFrequency;
                            fvRMSVoltage = fRMSVoltage;
                            fvOutputCurrent = fOutputCurrent;
                            mvRPM = "" + mRPM;
                            fvLPM = fLPM;
                            fvPVVoltage = fPVVoltage;
                            fvPVCurrent = fPVCurrent;
                            mvFault = "" + mFault;
                            fvInvTemp = fInvTemp;*/

                            mPostionFinal++;
                        }
                    }
                    while (iStream.available() > 0) {
                        int djdjd = iStream.read();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("vikas--8==>8");
                // baseRequest.hideLoader();
            }
            
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {

            super.onPostExecute(result);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    try {
                        filePath = new File(Utility.commonDocumentDirPath("ShaktiDataLoggerFile"), "Year_" + pairedDeviceList.get(selectedIndex).getDeviceName()  + ".xls").getAbsolutePath();


                        Log.d("filePath=====>", filePath);
                        String[] mDataNameString = filePath.split("ShaktiDataLoggerFile/");
                        String[] mDataNameString1 = mDataNameString[1].split(".xls");
                        String[] mDataNameString2 = mDataNameString1[0].split("_");
                     //   GetProfileUpdate_Task(mDataNameString2[1], mDataNameString2[0], headerLenghtMonth, filePath);
                        Utility.hideProgressDialogue();
                    } catch (Exception e) {
                        Utility.hideProgressDialogue();
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}