package com.datalogger.activity;

import static android.Manifest.permission.BLUETOOTH;
import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_SCAN;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION.SDK_INT;
import static java.lang.Thread.sleep;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.Volley;
import com.datalogger.R;
import com.datalogger.adapter.PairedDeviceAdapter;
import com.datalogger.model.PairDeviceModel;
import com.datalogger.utilis.FileUtils;
import com.datalogger.utilis.Utility;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity implements PairedDeviceAdapter.deviceSelectionListener {
    private static final int ATTACHMENT_REQUEST = 1;
    public static UUID my_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_CODE_PERMISSION = 1;
    AlertDialog alertDialog, alertDialogMonth;
    private static Workbook wb = null;
    private static Sheet sheet1 = null;
    private static Row row;
    private static Cell cell = null;
    ArrayList<PairDeviceModel> pairedDeviceList = new ArrayList<>();
    private List<String> mMonthHeaderList = new ArrayList<>();
    RecyclerView bluetoothDeviceList;

    RelativeLayout fileAttachRelative;
    TextView bluetoothState, fileNametxt, uploadBtn;
    BluetoothSocket bluetoothSocket;
    int mLengthCount, selectedIndex = 0, kk = 0, mmCount = 0, mCheckCLICKDayORMonth = 0, mvDay = 0, mvMonth = 0, mvYear = 0, mPostionFinal = 0, bytesRead = 0;
    String dirName = "", filePath = "", SS = "", headerLenghtMonth = "", headerLenghtMonthDongle = "", mvRPM = "", mvFault = "", mvHour = "", mvMinute = "", mvNo_of_Start = "", monthValue, type = "";
    private InputStream iStream = null;
    float fvFrequency = 0, fvRMSVoltage = 0, fvOutputCurrent = 0, fvLPM = 0, fvPVVoltage = 0, fvPVCurrent = 0, fvInvTemp = 0;
    int[] mTotalTime;
    boolean mBoolflag = false, vkFinalcheck = false;

    File selectedFile;
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    BufferedReader reader = null;

    String[] mimetypes =
            {"application/vnd.ms-excel", // .xls
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" // .xlsx
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Init();
        if (!checkPermission()) {
            requestPermission();
        } else {
            registerBroadcastManager();
        }
    }

    @SuppressLint("HandlerLeak")
    android.os.Handler mHandler = new android.os.Handler() {
        @Override
        public void handleMessage(Message msg) {
            String mString = (String) msg.obj;
            Toast.makeText(MainActivity.this, mString, Toast.LENGTH_LONG).show();
        }
    };



    /*-------------------------------------------------------------Paired Bluetooth Device List and Click listner-----------------------------------------------------------------------------*/

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
                bluetoothDeviceOff(getResources().getString(R.string.no_paired_device));
                //Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void Init() {
        bluetoothDeviceList = findViewById(R.id.bluetoothDeviceList);
        bluetoothState = findViewById(R.id.bluetoothState);
        fileAttachRelative = findViewById(R.id.fileAttachRelative);
        fileNametxt = findViewById(R.id.fileNametxt);
        uploadBtn = findViewById(R.id.uploadBtn);
        listner();
    }

    private void listner() {
        fileAttachRelative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file = new File(Environment.getRootDirectory() + File.separator + Environment.DIRECTORY_DOCUMENTS);
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
                intent.setDataAndType(Uri.parse(file.getAbsolutePath()), "*/*");
                //  startActivityForResult(intent, ATTACHMENT_REQUEST);
                startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), ATTACHMENT_REQUEST);


            }
        });

        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Utility.isConnectingToInternet(MainActivity.this)) {
                    if (filePath.isEmpty() && type.isEmpty()) {
                        Utility.ShowToast(getResources().getString(R.string.selectFileFirst), getApplicationContext());
                    } else {
                        uploadFile();
                    }
                } else {
                    Utility.ShowToast(getResources().getString(R.string.net_connection), getApplicationContext());
                }
            }
        });
    }

    private final BroadcastReceiver mBluetoothStatusChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        bluetoothDeviceOff(getResources().getString(R.string.bluetooth_is_off));
                        break;

                    case BluetoothAdapter.STATE_ON:

                        getPairedDeviceList();

                        break;

                }

            }
        }
    };

    private void bluetoothDeviceOff(String message) {
        bluetoothDeviceList.setVisibility(View.GONE);
        bluetoothState.setVisibility(View.VISIBLE);
        bluetoothState.setText(message);
        pairedDeviceList = new ArrayList<>();
    }

    private void registerBroadcastManager() {
        IntentFilter filter1 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetoothStatusChangedReceiver, filter1);

        if (Utility.isBluetoothEnabled()) {
            bluetoothState.setVisibility(View.GONE);
            getPairedDeviceList();

        } else {
            bluetoothDeviceOff(getResources().getString(R.string.bluetooth_is_off));
        }

    }

    private boolean checkPermission() {
        int Bluetooth = ContextCompat.checkSelfPermission(getApplicationContext(), BLUETOOTH);
        int BluetoothConnect = ContextCompat.checkSelfPermission(getApplicationContext(), BLUETOOTH_CONNECT);
        int BluetoothScan = ContextCompat.checkSelfPermission(getApplicationContext(), BLUETOOTH_SCAN);
        int ReadExternalStorage = ContextCompat.checkSelfPermission(getApplicationContext(), READ_EXTERNAL_STORAGE);
        int WriteExternalStorage = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);

        if (SDK_INT >= Build.VERSION_CODES.S) {
            return Bluetooth == PackageManager.PERMISSION_GRANTED && BluetoothConnect == PackageManager.PERMISSION_GRANTED
                    && BluetoothScan == PackageManager.PERMISSION_GRANTED;
        } else {
            return Bluetooth == PackageManager.PERMISSION_GRANTED && ReadExternalStorage == PackageManager.PERMISSION_GRANTED
                    && WriteExternalStorage == PackageManager.PERMISSION_GRANTED;

        }

    }

    private void requestPermission() {
        if (SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this,
                    new String[]{BLUETOOTH_CONNECT, BLUETOOTH_SCAN},
                    REQUEST_CODE_PERMISSION);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{BLUETOOTH, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_PERMISSION);
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.length > 0) {

                if (SDK_INT >= Build.VERSION_CODES.S) {

                    boolean BluetoothConnect = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean BluetoothScan = grantResults[1] == PackageManager.PERMISSION_GRANTED;


                    if (BluetoothConnect && BluetoothScan) {
                        registerBroadcastManager();
                    } else {
                        requestPermission();
                    }
                } else {

                    boolean Bluetooth = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean ReadExternalStorage = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean WriteExternalStorage = grantResults[2] == PackageManager.PERMISSION_GRANTED;

                    if (Bluetooth && ReadExternalStorage && WriteExternalStorage) {
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
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBluetoothStatusChangedReceiver);
        }

    }


    @Override
    public void DeviceSelectionListener(PairDeviceModel pairDeviceModel, int position) {
        selectedIndex = position;

        DataExtractPopup();
    }



    /*-------------------------------------------------------------Retrive Drive Yearly Data-----------------------------------------------------------------------------*/

    private class BluetoothCommunicationGetDeviceYearlyData extends AsyncTask<String, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            alertDialog.dismiss();
            Utility.showProgressDialogue(MainActivity.this);
        }


        @Override
        protected Boolean doInBackground(String... requests) //while the progress dialog is shown, the connection is done in background
        {

            try {
                if (bluetoothSocket != null) {
                    if (!bluetoothSocket.isConnected()) {
                        connectToBluetoothSocket();

                    }
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
                        Log.e("sss====>", SSS);
                        Log.e("sss====>", Arrays.toString(mS));
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

                        }
                    }
                }

            } catch (Exception e) {
                Log.e("Exception====>", e.getMessage());

                Message mess = new Message();
                mess.obj = " Some conflict occurred in data extraction. Please remove and reconnect dongle";
                mHandler.sendMessage(mess);
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
            Log.e("DeviceMonthHeaderList====>", String.valueOf(mMonthHeaderList.size()));
            super.onPostExecute(result);
            if (mMonthHeaderList.size() > 0) {
                Log.e("DeviceMonthHeaderList1====>", String.valueOf(mMonthHeaderList.size()));
                new BluetoothCommunicationForGetDeviceData().execute(":MDATA#", ":MDATA#", "START");
            } else {
                Utility.hideProgressDialogue();
                Message msg = new Message();
                msg.obj = "Please try again!";
                mHandler.sendMessage(msg);
                /*runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new BluetoothCommunicationGetDeviceYearlyData().execute(":MLENGTH#", ":MLENGTH#", "OKAY");
                    }
                });*/
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class BluetoothCommunicationForGetDeviceData extends AsyncTask<String, Void, Boolean>  // UI thread
    {
        public int RetryCount = 0;
        private int bytesRead;

        @Override
        protected void onPreExecute() {
            kk = 0;
            mBoolflag = false;
            //  baseRequest.showLoader();
        }

        @Override
        protected Boolean doInBackground(String... requests) //while the progress dialog is shown, the connection is done in background
        {
            try {
                // bluetoothSocket.close();
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
                        Utility.hideProgressDialogue();
                        e1.printStackTrace();
                    }
                    for (int i = 0; i < 12; i++) {
                        try {
                            bytesRead = iStream.read();
                        } catch (IOException e) {
                            System.out.println("vikas--2==>2");
                            Utility.hideProgressDialogue();
                            e.printStackTrace();
                        }
                    }
                    int[] bytesReaded;
                    while (true) {
                        bytesReaded = new int[mLengthCount];
                        for (int i = 0; i < mLengthCount; i++) {
                            int mCharOne = 0;
                            int mCharTwo = 0;
                            try {
                                mCharOne = iStream.read();
                                mCharTwo = iStream.read();
                            } catch (IOException e) {
                                Utility.hideProgressDialogue();
                                e.printStackTrace();
                            }
                            try {
                                System.out.println("vikas--3==>" + mCharOne + "" + mCharTwo);
                                if ("TX".equalsIgnoreCase((char) mCharOne + "" + (char) mCharTwo)) {

                                    Utility.hideProgressDialogue();
                                    Message message = new Message();
                                    message.obj = "Data Extraction Completed!";
                                    mHandler.sendMessage(message);
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
                                System.out.println("vikas--3==>N");
                                e.printStackTrace();
                            }
                        }

                        int mDay = 0;
                        int mMonth = 0;
                        int mYear = 0;
                        int mHour = 0;
                        int mMinut = 0;
                        int mStatus = 0;
                        int mRPM = 0;
                        int mFault = 0;

                        float fFrequency = 0;
                        float fRMSVoltage = 0;
                        float fOutputCurrent = 0;
                        float fLPM = 0;
                        float fPVVoltage = 0;
                        float fPVCurrent = 0;
                        float fInvTemp = 0;

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
                            if (sheet1 == null) {
                                wb = new HSSFWorkbook();
                                wb.createSheet(pairedDeviceList.get(selectedIndex).getDeviceName() + "_" + Calendar.getInstance().getTimeInMillis() + ".xls");
                            }
                            try {
                                FileOutputStream os = new FileOutputStream(dirName);
                                wb.write(os);
                                os.close();
                                Log.w("FileUtils", "Writing file" + dirName);

                            } catch (IOException e) {
                                Log.w("FileUtils", "Error writing " + dirName, e);
                            } catch (Exception e) {
                                Log.w("FileUtils", "Failed to save file", e);

                            }
                            break;
                        }
                        //  if((mDay == 255) && (mMonth == 255) && (mYear == 255) && (mHour == 255) && (mMinut == 255) && (mStatus == 255))
                        if (((mDay == 255) && (mMonth == 255) && (mYear == 255)) || ((mDay == 0) && (mMonth == 0) && (mYear == 0))) {

                            if (sheet1 == null) {
                                wb = new HSSFWorkbook();
                                wb.createSheet(pairedDeviceList.get(selectedIndex).getDeviceName() + "_" + Calendar.getInstance().getTimeInMillis() + ".xls");
                            }
                            try {
                                FileOutputStream os = new FileOutputStream(dirName);
                                wb.write(os);
                                os.close();
                                Log.w("FileUtils", "Writing file" + dirName);

                            } catch (IOException e) {
                                Log.w("FileUtils", "Error writing " + dirName, e);
                            } catch (Exception e) {
                                Log.w("FileUtils", "Failed to save file", e);

                            }


                        } else {
                            if (mPostionFinal == 0) {
                                //New Workbook
                                wb = new HSSFWorkbook();

                                sheet1 = wb.createSheet(pairedDeviceList.get(selectedIndex).getDeviceName() + "_" + Calendar.getInstance().getTimeInMillis() + ".xls");
                                row = sheet1.createRow(0);

                                for (int k = 0; k < mMonthHeaderList.size(); k++) {

                                    String[] mStringSplitStart = mMonthHeaderList.get(k).split("-");

                                    sheet1.setColumnWidth(k, (10 * 200));
                                    cell = row.createCell(k);
                                    //cell.setCellValue(mMonthHeaderList.get(k));
                                    cell.setCellValue(mStringSplitStart[0]);

                                }

                                row = sheet1.createRow(mPostionFinal + 1);

                                cell = row.createCell(0);
                                cell.setCellValue("" + mDay);


                                cell = row.createCell(1);
                                cell.setCellValue("" + mMonth);


                                cell = row.createCell(2);
                                cell.setCellValue("" + mYear);


                                cell = row.createCell(3);
                                cell.setCellValue("" + mHour);


                                cell = row.createCell(4);
                                cell.setCellValue("" + mMinut);


                                cell = row.createCell(5);
                                cell.setCellValue("" + mStatus);


                                try {
                                    //  for (int j = 3; j < mLengthCount; j++)
                                    for (int j = 6; j < mMonthHeaderList.size(); j++) {
                                        //     fTotalEnergy = Float.intBitsToFloat(mDayDataList.get(i)[j]);


                                        String[] mStringSplitStart = mMonthHeaderList.get(j).split("-");
                                        int mmIntt = 1;
                                        Log.e("mStringSplitStart===>", Arrays.toString(mStringSplitStart));


                                        try {
                                            mmIntt = Integer.parseInt(mStringSplitStart[1]);
                                        } catch (Exception e) {
                                            mmIntt = 10;
                                        }

                                        try {

                                            if (mmIntt == 1) {


                                                sheet1.setColumnWidth(j, (10 * 200));
                                                fFrequency = mTotalTime[j];

                                                cell = row.createCell(j);
                                                cell.setCellValue("" + fFrequency);


                                                // tr.addView(getTextView(counter, ((mTotalTime[i] / mmIntt)) + "", Color.BLACK, Typeface.NORMAL, ContextCompat.getColor(this, R.color.white)));
                                            } else {


                                                sheet1.setColumnWidth(j, (10 * 200));
                                                fFrequency = mTotalTime[j];

                                                float mmValue = (((float) mTotalTime[j]) / ((float) mmIntt));

                                                cell = row.createCell(j);
                                                //cell.setCellValue("" + fFrequency);
                                                cell.setCellValue("" + mmValue);


                                                //  tr.addView(getTextView(counter, ( (((float)mTotalTime[i]) / ((float)mmIntt))) + "", Color.BLACK, Typeface.NORMAL, ContextCompat.getColor(this, R.color.white)));
                                            }

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }


                                    }
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();

                                }

                            } else {
                                // cs.setFillPattern(HSSFCellStyle.NO_FILL);
                                row = sheet1.createRow(mPostionFinal + 1);

                                cell = row.createCell(0);
                                cell.setCellValue("" + mDay);


                                cell = row.createCell(1);
                                cell.setCellValue("" + mMonth);


                                cell = row.createCell(2);
                                cell.setCellValue("" + mYear);


                                cell = row.createCell(3);
                                cell.setCellValue("" + mHour);


                                cell = row.createCell(4);
                                cell.setCellValue("" + mMinut);


                                cell = row.createCell(5);
                                cell.setCellValue("" + mStatus);


                                try {
                                    //  for (int j = 3; j < mLengthCount; j++)
                                    for (int j = 6; j < mMonthHeaderList.size(); j++) {
                                        //     fTotalEnergy = Float.intBitsToFloat(mDayDataList.get(i)[j]);


                                        String[] mStringSplitStart = mMonthHeaderList.get(j).split("-");
                                        int mmIntt = 1;
                                        try {
                                            mmIntt = Integer.parseInt(mStringSplitStart[1]);
                                        } catch (Exception e) {
                                            mmIntt = 10;
                                        }

                                        try {

                                            if (mmIntt == 1) {

                                                if (j < 15) {
                                                    sheet1.setColumnWidth(j, (10 * 200));
                                                    fFrequency = mTotalTime[j];

                                                    cell = row.createCell(j);
                                                    cell.setCellValue("" + fFrequency);

                                                }
                                                // tr.addView(getTextView(counter, ((mTotalTime[i] / mmIntt)) + "", Color.BLACK, Typeface.NORMAL, ContextCompat.getColor(this, R.color.white)));
                                            } else {


                                                sheet1.setColumnWidth(j, (10 * 200));
                                                fFrequency = mTotalTime[j];

                                                float mmValue = (((float) mTotalTime[j]) / ((float) mmIntt));

                                                cell = row.createCell(j);
                                                cell.setCellValue("" + mmValue);


                                                //  tr.addView(getTextView(counter, ( (((float)mTotalTime[i]) / ((float)mmIntt))) + "", Color.BLACK, Typeface.NORMAL, ContextCompat.getColor(this, R.color.white)));
                                            }

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                    }
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }


                            }

                            mvDay = mDay;
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
                            fvInvTemp = fInvTemp;

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
                Utility.hideProgressDialogue();
            }
            return false;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

        }
    }


    /*-------------------------------------------------------------Retrieve Dongle Yearly Data-----------------------------------------------------------------------------*/

    @SuppressLint("StaticFieldLeak")
    private class BluetoothCommunicationGetDongleYearlyData extends AsyncTask<String, Void, Boolean>  // UI thread
    {

        @Override
        protected void onPreExecute() {
            alertDialog.dismiss();
            alertDialogMonth.dismiss();
            Utility.showProgressDialogue(MainActivity.this);
            super.onPreExecute();
        }

        //while the progress dialog is shown, the connection is done in background
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected Boolean doInBackground(String... requests) {

            try {
                if (bluetoothSocket != null) {
                    if (!bluetoothSocket.isConnected()) {
                        connectToBluetoothSocket();

                    }
                }

                if (!bluetoothSocket.isConnected())
                    bluetoothSocket.connect();//start connection


                if (bluetoothSocket.isConnected()) {

                    byte[] STARTRequest = requests[0].getBytes(StandardCharsets.US_ASCII);

                    try {
                        bluetoothSocket.getOutputStream().write(STARTRequest);
                        sleep(10000);
                        iStream = bluetoothSocket.getInputStream();
                    } catch (InterruptedException e1) {
                        Utility.hideProgressDialogue();
                        e1.printStackTrace();
                    }

                    SS = "";

                    System.out.println("iStream.available()==>>" + iStream.available());

                    while (iStream.available() > 0) {
                        SS += (char) iStream.read();
                    }
                    if (!SS.trim().isEmpty()) {

                        String SSS = SS.replace(",", "VIKASGOTHI");
                        // String [] mS = SS.split(",");
                        String[] mS = SSS.split("VIKASGOTHI");

                        Log.e("sss====>", SSS);
                        Log.e("sss====>", Arrays.toString(mS));
                        if (mS.length > 0) {

                            for (int i = 0; i < mS.length; i++) {
                                System.out.println("mSmSmS====>>" + mS[i]);

                                if (i == 0) {
                                    mLengthCount = Integer.valueOf(mS[i]);
                                } else {
                                    mMonthHeaderList.add(mS[i]);
                                }
                            }
                            headerLenghtMonthDongle = "" + mMonthHeaderList.size();
                        }
                        System.out.println("headerLenghtMonthDongle==>> " + headerLenghtMonthDongle);

                    }


                }
            } catch (Exception e) {
                Log.e("Exception=====>", e.getMessage());
                Utility.hideProgressDialogue();
                connectToBluetoothSocket();
                return false;
            }


            return false;
        }

        //after the doInBackground, it checks if everything went fine
        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            Log.e("mMonthHeaderList====>", String.valueOf(mMonthHeaderList.size()));
            if (mMonthHeaderList.size() > 0) {
                Log.e("mMonthHeaderList2====>", String.valueOf(mMonthHeaderList.size()));
                new BluetoothCommunicationForGetDongleData().execute(":YDATA" + monthValue + "#", ":YDATA" + monthValue + "#", "START");
                //   new BluetoothCommunicationForYdataEXTActivity().execute(":YDATA06#", ":YDATA01#", "START");

            } else {
                Utility.hideProgressDialogue();

                Message msg = new Message();
                msg.obj = "Please try again!";
                mHandler.sendMessage(msg);

                // runOnUiThread(() -> new BluetoothCommunicationGetDongleYearlyData().execute(":YLENGTH#", ":YLENGTH#", "OKAY"));
            }
        }
    }

    // UI thread
    @SuppressLint("StaticFieldLeak")

    private class BluetoothCommunicationForGetDongleData extends AsyncTask<String, Void, Boolean> {
        public int RetryCount = 0;
        private int bytesRead;

        @Override
        protected void onPreExecute() {
            kk = 0;
            mBoolflag = false;
            mPostionFinal = 0;
            //baseRequest.showLoader();
        }


        @Override
        protected Boolean doInBackground(String... requests) //while the progress dialog is shown, the connection is done in background
        {
            try {

                // bluetoothSocket.close();
                if (!bluetoothSocket.isConnected())
                    bluetoothSocket.connect();
                if (bluetoothSocket.isConnected()) {

                    Log.e("requests====>", Arrays.toString(requests));
                    Log.e("requests2====>", String.valueOf(requests[0]));

                    Log.e("requests3====>", String.valueOf(requests[0].getBytes(StandardCharsets.US_ASCII)));

                    try {
                        byte[] STARTRequest = requests[0].getBytes(StandardCharsets.US_ASCII);
                        bluetoothSocket.getOutputStream().write(STARTRequest);
                        sleep(3000);
                        iStream = bluetoothSocket.getInputStream();
                    } catch (InterruptedException e1) {
                        System.out.println("vikas--1==>1" + e1.getMessage());
                        //baseRequest.hideLoader();
                        e1.printStackTrace();
                    }
                    if (iStream.available() > 0) {
                        //Code For TX BTY START ======> 84 88 32 66 84 89 32 83 84 65 82 84
                        for (int i = 0; i < 12; i++) {
                            try {
                                bytesRead = iStream.read();
                                Log.e("bytesRead=====>", String.valueOf(bytesRead) + "=======>" + i);
                            } catch (IOException e) {
                                System.out.println("vikas--2==>2" + bytesRead);
                                Utility.hideProgressDialogue();
                                e.printStackTrace();
                            }
                        }


                        int[] bytesReaded;
                        int jjkk = 0;
                        while (true) {

                            bytesReaded = new int[mLengthCount];
                            int jk = 0;
                            int i = 0;
                            int kp = 0;
                            System.out.print("spspsp==>>" + jjkk + " =");

                            for (int j = 0; j < 125; j++) {

                                bytesReaded[kp] = iStream.read();
                                System.out.print(bytesReaded[kp] + " ");
                                kp++;
                                if ("TX".equalsIgnoreCase((char) bytesReaded[0] + "" + (char) bytesReaded[1])) {
                                    Utility.hideProgressDialogue();
                                    System.out.println("TX_COMPLETE_i==" + i);
                                    vkFinalcheck = true;
                                    mBoolflag = true;
                                    break;
                                }
                            }

                            if (bytesReaded[0] == 255 && bytesReaded[1] == 255) {

                                Utility.hideProgressDialogue();
                                vkFinalcheck = true;
                                System.out.println("TX_COMPLETE_ghgi==" + i);
                                mBoolflag = true;

                                Message msg = new Message();
                                msg.obj = "Data Extraction Completed!";
                                mHandler.sendMessage(msg);

                                break;
                            }

                            jjkk++;
                            System.out.println("ForTesting==" + jjkk + " = " + bytesReaded[0] + ", " + bytesReaded[1]);
                            System.out.println("Main_while_i==" + jjkk + " = " + (char) bytesReaded[0] + ", " + (char) bytesReaded[1]);
                            if ("TX".equalsIgnoreCase((char) bytesReaded[0] + "" + (char) bytesReaded[1])) {
                                Utility.hideProgressDialogue();
                                vkFinalcheck = true;
                                System.out.println("TX_COMPLETE_i==" + i);
                                mBoolflag = true;
                                break;
                            } else {
                                jk = 0;
                                mTotalTime = new int[10];

                                for (int k = 0; k < 5; k++) {
                                    //System.out.println("first_loop_i=="+k);
                                    mTotalTime = Arrays.copyOf(mTotalTime, jk + 1);
                                    long d;
                                    mTotalTime[jk] = bytesReaded[k];

                                    System.out.println("float_jk==" + jk + " " + Float.intBitsToFloat(mTotalTime[jk]));

                                    jk++;

                                }

                                for (int k = 5; k < 125; ) {
                                    //System.out.println("first_loop_i=="+k);
                                    mTotalTime = Arrays.copyOf(mTotalTime, jk + 1);

                                    mTotalTime[jk] = bytesReaded[k];
                                    mTotalTime[jk] |= bytesReaded[k + 1] << 8;
                                    mTotalTime[jk] |= bytesReaded[k + 2] << 16;
                                    mTotalTime[jk] |= bytesReaded[k + 3] << 24;
                                    System.out.println("float_jk==" + jk + " " + Float.intBitsToFloat(mTotalTime[jk]));

                                    jk++;
                                    k += 4;

                                }


                            }


                            float fFrequency = 0;

                            if (!mBoolflag) {
                                kk++;
                                System.out.println("kk++ ==>> " + kk);
                            } else {

                                if (sheet1 == null) {
                                    Log.e("PairdDeviceName", pairedDeviceList.get(selectedIndex).getDeviceName());
                                    wb = new HSSFWorkbook();
                                    sheet1 = wb.createSheet("Dongle_" + pairedDeviceList.get(selectedIndex).getDeviceName() + "_" + Calendar.getInstance().getTimeInMillis() + ".xls");
                                }
                                try {
                                    FileOutputStream os = new FileOutputStream(dirName);
                                    wb.write(os);
                                    os.close();
                                    Log.w("FileUtils", "Writing file" + dirName);

                                } catch (IOException e) {
                                    Log.w("FileUtils", "Error writing " + dirName, e);
                                } catch (Exception e) {
                                    Log.w("FileUtils", "Failed to save file", e);

                                }

                            }

                            if (vkFinalcheck) {
                                System.out.println("Nothing do it ...");
                                Utility.hideProgressDialogue();

                                break;
                            } else {
                                if (mPostionFinal == 0) {
                                    System.out.println("mPostionFinal==000 " + mPostionFinal);
                                    wb = new HSSFWorkbook();
                                    Log.e("PairdDeviceName2", pairedDeviceList.get(selectedIndex).getDeviceName());
                                    sheet1 = wb.createSheet("Dongle_" + pairedDeviceList.get(selectedIndex).getDeviceName() + "_" + Calendar.getInstance().getTimeInMillis() + ".xls");
                                    row = sheet1.createRow(0);

                                    for (int k = 0; k < mMonthHeaderList.size(); k++) {
                                        String[] mStringSplitStart = mMonthHeaderList.get(k).split("-");
                                        sheet1.setColumnWidth(k, (10 * 200));
                                        cell = row.createCell(k);

                                        cell.setCellValue(mStringSplitStart[0]);
                                        System.out.println("HEADER+++>>> " + mStringSplitStart[0]);

                                    }

                                    try {
                                        row = sheet1.createRow(mPostionFinal + 1);
                                        for (int j = 0; j < mMonthHeaderList.size(); j++) {
                                            String[] mStringSplitStart = mMonthHeaderList.get(j).split("-");
                                            int mmIntt = 1;
                                            mmIntt = Integer.parseInt(mStringSplitStart[1]);

                                            try {
                                                if (mmIntt == 1) {
                                                    sheet1.setColumnWidth(j, (10 * 200));
                                                    if (j > 4) {
                                                        fFrequency = Float.intBitsToFloat(mTotalTime[j]) / ((float) mmIntt);
                                                    } else {
                                                        fFrequency = mTotalTime[j];
                                                    }
                                                    cell = row.createCell(j);
                                                    cell.setCellValue("" + fFrequency);

                                                    System.out.println("fFrequency===>>>vk1==>>" + fFrequency);
                                                } else {
                                                    sheet1.setColumnWidth(j, (10 * 200));
                                                    fFrequency = mTotalTime[j];

                                                    if (j > 4) {
                                                        fFrequency = Float.intBitsToFloat(mTotalTime[j]) / ((float) mmIntt);
                                                    } else {
                                                        fFrequency = mTotalTime[j];
                                                    }
                                                    cell = row.createCell(j);
                                                    cell.setCellValue("" + fFrequency);

                                                    System.out.println("mmValue===>>>vk1==>>" + fFrequency);
                                                }
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }

                                        }

                                        mPostionFinal = mPostionFinal + 1;
                                    } catch (NumberFormatException e) {
                                        System.out.println("printStackTrace++ ==>> ");
                                        e.printStackTrace();
                                        Utility.hideProgressDialogue();
                                    }
                                } else {


                                    System.out.println("mPostionFinal >= " + mPostionFinal);

                                    row = sheet1.createRow(mPostionFinal + 1);


                                    try {
                                        for (int j = 0; j < mMonthHeaderList.size(); j++) {
                                            String[] mStringSplitStart = mMonthHeaderList.get(j).split("-");
                                            int mmIntt = 1;
                                            mmIntt = Integer.parseInt(mStringSplitStart[1]);

                                            try {
                                                if (mmIntt == 1) {
                                                    sheet1.setColumnWidth(j, (10 * 200));
                                                    if (j > 4) {
                                                        fFrequency = Float.intBitsToFloat(mTotalTime[j]) / ((float) mmIntt);
                                                    } else {
                                                        fFrequency = mTotalTime[j];
                                                    }
                                                    cell = row.createCell(j);
                                                    cell.setCellValue("" + fFrequency);

                                                    System.out.println("fFrequency===>>>vkkkk1==>>" + fFrequency);
                                                } else {
                                                    sheet1.setColumnWidth(j, (10 * 200));
                                                    if (j > 4) {
                                                        fFrequency = Float.intBitsToFloat(mTotalTime[j]) / ((float) mmIntt);
                                                    } else {
                                                        fFrequency = mTotalTime[j];
                                                    }
                                                    cell = row.createCell(j);

                                                    cell.setCellValue("" + fFrequency);


                                                    System.out.println("mmValue===>>>vkppp1==>>" + fFrequency);
                                                }
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                Utility.hideProgressDialogue();
                                            }

                                        }
                                    } catch (NumberFormatException e) {
                                        System.out.println("printStackTrace++ ==>> ");
                                        e.printStackTrace();

                                    }
                                }


                                System.out.println("vikas--n==>4");

                                if (sheet1 == null) {
                                    Log.e("PairdDeviceName3", pairedDeviceList.get(selectedIndex).getDeviceName());
                                    wb = new HSSFWorkbook();
                                    sheet1 = wb.createSheet("Dongle_" + pairedDeviceList.get(selectedIndex).getDeviceName() + "_" + Calendar.getInstance().getTimeInMillis() + ".xls");
                                }
                                try {
                                    FileOutputStream os = new FileOutputStream(dirName);
                                    wb.write(os);
                                    os.close();
                                    Log.w("FileUtils", "Writing file" + dirName);

                                } catch (IOException e) {
                                    Log.w("FileUtils", "Error writing " + dirName, e);
                                } catch (Exception e) {
                                    Log.w("FileUtils", "Failed to save file", e);

                                }
                                mPostionFinal++;
                            }
                        }
                    }
                } else {
                    Utility.hideProgressDialogue();
                    Utility.ShowToast("Please Try Again!", MainActivity.this);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("vikas--8==>8");
                // baseRequest.hideLoader();
                Utility.hideProgressDialogue();

            }
            return false;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(Boolean result) //after the doInBackground, it checks if everything went fine
        {
            Utility.hideProgressDialogue();
            super.onPostExecute(result);

        }
    }


    /*-------------------------------------------------------------Retrieve Dongle 5 Year Data-----------------------------------------------------------------------------*/
    @SuppressLint("StaticFieldLeak")
    private class BluetoothCommunicationGet5YearDongleLength extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            alertDialog.dismiss();
            Utility.showProgressDialogue(MainActivity.this);
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected Boolean doInBackground(String... requests) //while the progress dialog is shown, the connection is done in background
        {

            try {
                if (bluetoothSocket != null) {
                    if (!bluetoothSocket.isConnected()) {
                        connectToBluetoothSocket();

                    }
                } else {
                    connectToBluetoothSocket();
                }

                if (bluetoothSocket.isConnected()) {
                    byte[] STARTRequest = requests[0].getBytes(StandardCharsets.US_ASCII);


                    try {
                        bluetoothSocket.getOutputStream().write(STARTRequest);
                        sleep(1000);
                        iStream = bluetoothSocket.getInputStream();
                    } catch (InterruptedException e1) {
                        //   baseRequest.hideLoader();
                        e1.printStackTrace();
                    }


                    SS = "";

                    while (iStream.available() > 0) {
                        SS += (char) iStream.read();
                    }
                    if (!SS.trim().isEmpty()) {
                        String SSS = SS.replace(",", "VIKASGOTHI");

                        String[] mS = SSS.split("VIKASGOTHI");

                        if (mS.length > 0) {

                            for (int i = 0; i < mS.length; i++) {


                                if (!mS[i].trim().equalsIgnoreCase("null")) {
                                    if (i == 0) {
                                        //mLengthCount = Integer.parseInt(mS[i]);
                                        mLengthCount = Integer.valueOf(mS[i]);
                                    } else {
                                        mMonthHeaderList.add(mS[i]);
                                    }
                                } else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            new BluetoothCommunicationGet5YearDongleLength().execute(":CLENGTH#", ":CLENGTH#", "OKAY");
                                        }
                                    });

                                }

                            }

                        }
                    }


                }
            } catch (Exception e) {
                Log.e("Exception5Year====>", e.getMessage());
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
            if (mMonthHeaderList.size() > 0) {
                new BluetoothCommunicationForCDataEXTActivity().execute(":CDATA#", ":CDATA#", "START");
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Utility.hideProgressDialogue();
                        new BluetoothCommunicationGet5YearDongleLength().execute(":CLENGTH#", ":CLENGTH#", "OKAY");
                    }
                });
            }
            super.onPostExecute(result);

        }
    }

    @SuppressLint("StaticFieldLeak")
    private class BluetoothCommunicationForCDataEXTActivity extends AsyncTask<String, Void, Boolean>  // UI thread
    {
        private int bytesRead;

        @Override
        protected void onPreExecute() {
            mBoolflag = false;
            //  baseRequest.showLoader();
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected Boolean doInBackground(String... requests) //while the progress dialog is shown, the connection is done in background
        {
            try {
                if (!bluetoothSocket.isConnected())
                    bluetoothSocket.connect();//start connection

                if (bluetoothSocket.isConnected()) {
                    byte[] STARTRequest = requests[0].getBytes(StandardCharsets.US_ASCII);

                    try {
                        bluetoothSocket.getOutputStream().write(STARTRequest);
                        sleep(400);
                        iStream = bluetoothSocket.getInputStream();
                    } catch (InterruptedException e1) {

                        e1.printStackTrace();
                    }

                    for (int i = 0; i < 12; i++) {
                        try {
                            bytesRead = iStream.read();
                        } catch (IOException e) {
                            // baseRequest.hideLoader();
                            e.printStackTrace();
                        }
                    }
                    int[] bytesReaded;

                    while (true) {
                        bytesReaded = new int[mLengthCount];
                        for (int i = 0; i < mLengthCount; i++) {

                            int mCharOne = iStream.read();
                            int mCharTwo = iStream.read();

                            try {
                                System.out.println("vikas--3==>i" + i + " =" + String.valueOf((char) mCharOne) + String.valueOf((char) mCharTwo));
                                if ("TX".equals(String.valueOf((char) mCharOne) + String.valueOf((char) mCharTwo))) {
                                    mBoolflag = true;

                                    Utility.hideProgressDialogue();
                                    Message msg = new Message();
                                    msg.obj = "Data Extraction Completed!";
                                    mHandler.sendMessage(msg);
                                    break;
                                } else {
                                    if (mCharOne == 0 || mCharTwo == 0) {
                                        bytesReaded[i] = Integer.parseInt("" + mCharOne + mCharTwo, 16);// iStream.read();
                                    } else {
                                        bytesReaded[i] = Integer.parseInt("" + (char) mCharOne + (char) mCharTwo, 16);// iStream.read();
                                    }

                                }
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                                Utility.hideProgressDialogue();
                            }
                        }
                        int mDay = 0;
                        int mMonth = 0;
                        int mYear = 0;
                        float fTotalEnergy = 0;


                        if (!mBoolflag) {

                            kk++;
                            System.out.println("\nDay_vikas_kk++ ==>> " + kk);

                            mDay = bytesReaded[0];
                            mMonth = bytesReaded[1];
                            mYear = bytesReaded[2];
                            int a = 0;
                            mTotalTime = new int[10];
                            mTotalTime[0] = mDay;
                            mTotalTime[1] = mMonth;
                            mTotalTime[2] = mYear;

                            int i = 3;
                            for (int k = 3; k < mLengthCount - 3; k += 4) {

                                mTotalTime = Arrays.copyOf(mTotalTime, i + 1);
                                mTotalTime[i] = bytesReaded[k];
                                mTotalTime[i] |= bytesReaded[k + 1] << 8;
                                mTotalTime[i] |= bytesReaded[k + 2] << 16;
                                mTotalTime[i] |= bytesReaded[k + 3] << 24;
                                System.out.println(Float.intBitsToFloat(mTotalTime[i]));
                                i++;
                            }

                            mvDay = mDay;
                            mvMonth = mMonth;
                            mvYear = mYear;

                        } else {
                            Utility.hideProgressDialogue();
                            if (sheet1 == null) {
                                Log.e("PairdDeviceName", pairedDeviceList.get(selectedIndex).getDeviceName());
                                wb = new HSSFWorkbook();
                                sheet1 = wb.createSheet("Dongle5Year_" + pairedDeviceList.get(selectedIndex).getDeviceName() + "_" + Calendar.getInstance().getTimeInMillis() + ".xls");

                            }
                            try {
                                FileOutputStream os = new FileOutputStream(dirName);
                                wb.write(os);
                                os.close();
                                Log.w("FileUtils", "Writing file" + dirName);

                            } catch (IOException e) {
                                Log.w("FileUtils", "Error writing " + dirName, e);
                            } catch (Exception e) {
                                Log.w("FileUtils", "Failed to save file", e);

                            }

                            break;
                        }

                        if (((mDay == 255) && (mMonth == 255) && (mYear == 255)) || ((mDay == 0) || (mMonth == 0) || (mYear == 0))) {
                            if (mPostionFinal == 0) {
                                if (sheet1 == null) {
                                    Log.e("PairdDeviceName", pairedDeviceList.get(selectedIndex).getDeviceName());
                                    wb = new HSSFWorkbook();
                                    sheet1 = wb.createSheet("Dongle5Year_" + pairedDeviceList.get(selectedIndex).getDeviceName() + "_" + Calendar.getInstance().getTimeInMillis() + ".xls");
                                }
                                try {
                                    FileOutputStream os = new FileOutputStream(dirName);
                                    wb.write(os);
                                    os.close();
                                    Log.w("FileUtils", "Writing file" + dirName);

                                } catch (IOException e) {
                                    Log.w("FileUtils", "Error writing " + dirName, e);
                                } catch (Exception e) {
                                    Log.w("FileUtils", "Failed to save file", e);

                                }
                            }
                            //  break;
                        } else {
                            if (mPostionFinal == 0) {

                                wb = new HSSFWorkbook();
                                //New Sheet
                                sheet1 = wb.createSheet("Dongle5Year_" + pairedDeviceList.get(selectedIndex).getDeviceName() + "_" + Calendar.getInstance().getTimeInMillis() + ".xls");
                                row = sheet1.createRow(0);

                                for (int k = 0; k < mMonthHeaderList.size(); k++) {

                                    String[] mStringSplitStart = mMonthHeaderList.get(k).split("-");
                                    sheet1.setColumnWidth(k, (10 * 200));
                                    cell = row.createCell(k);
                                    cell.setCellValue(mStringSplitStart[0]);

                                }
                                row = sheet1.createRow(mPostionFinal + 1);

                                sheet1.setColumnWidth(0, (10 * 200));
                                cell = row.createCell(0);
                                cell.setCellValue("" + mDay);


                                sheet1.setColumnWidth(1, (10 * 200));
                                cell = row.createCell(1);
                                cell.setCellValue("" + mMonth);


                                cell = row.createCell(2);
                                sheet1.setColumnWidth(2, (10 * 200));
                                cell.setCellValue("" + mYear);


                                try {
                                    for (int j = 3; j < mMonthHeaderList.size(); j++) {

                                        String[] mStringSplitStart = mMonthHeaderList.get(j).split("-");
                                        int mmIntt = 1;
                                        try {
                                            mmIntt = Integer.parseInt(mStringSplitStart[1]);
                                        } catch (NumberFormatException e) {
                                            e.printStackTrace();
                                        }

                                        sheet1.setColumnWidth(j, (10 * 200));
                                        fTotalEnergy = (Float.intBitsToFloat(mTotalTime[j]) / mmIntt);
                                        float fffff = 0;
                                        if (mMonthHeaderList.size() - 1 == j) {
                                            fffff = (float) (fTotalEnergy * 1.67);
                                        } else {
                                            fffff = fTotalEnergy;
                                        }
                                        DecimalFormat df = new DecimalFormat("#");
                                        df.setMaximumFractionDigits(10);
                                        cell = row.createCell(j);

                                        cell.setCellValue(df.format(fffff));

                                    }
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }


                            } else {
                                row = sheet1.createRow(mPostionFinal + 1);

                                sheet1.setColumnWidth(0, (10 * 200));
                                cell = row.createCell(0);
                                cell.setCellValue("" + mDay);


                                sheet1.setColumnWidth(1, (10 * 200));
                                cell = row.createCell(1);
                                cell.setCellValue("" + mMonth);


                                sheet1.setColumnWidth(2, (10 * 200));
                                cell = row.createCell(2);
                                cell.setCellValue("" + mYear);


                                try {
                                    for (int j = 3; j < mMonthHeaderList.size(); j++) {

                                        String[] mStringSplitStart = mMonthHeaderList.get(j).split("-");
                                        int mmIntt = 1;
                                        try {
                                            mmIntt = Integer.parseInt(mStringSplitStart[1]);
                                        } catch (NumberFormatException e) {
                                            e.printStackTrace();
                                        }
                                        sheet1.setColumnWidth(j, (10 * 200));

                                        fTotalEnergy = (Float.intBitsToFloat(mTotalTime[j]) / mmIntt);
                                        float fffff = 0;
                                        if (mMonthHeaderList.size() - 1 == j) {
                                            fffff = (float) (fTotalEnergy * 1.67);
                                        } else {
                                            fffff = fTotalEnergy;
                                        }
                                        DecimalFormat df = new DecimalFormat("#");
                                        df.setMaximumFractionDigits(10);
                                        cell = row.createCell(j);

                                        cell.setCellValue(df.format(fffff));

                                    }
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }

                            }
                            mPostionFinal++;
                        }
                    }

                }
            } catch (Exception e) {

                e.printStackTrace();
                Utility.hideProgressDialogue();
            }
            return false;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

        }
    }

    /*-------------------------------------------------------------Upload Excel Sheet-----------------------------------------------------------------------------*/

    public void uploadFile() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setMessage("Loading...");
        progressDialog.show();
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        VolleyMultipartRequest volleyMultipartRequest = new VolleyMultipartRequest(
                Request.Method.POST,
                "https://solar10.shaktisolarrms.com/RMSAppTest1/ExcelUploadNew",
                new Response.Listener<NetworkResponse>() {
                    @Override
                    public void onResponse(NetworkResponse response) {
                        //Handle response
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }


                        try {
                            String json = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                            JSONObject obj = new JSONObject(json);
                            if (obj.getString("status").equals("true")) {
                                fileNametxt.setText("");
                            }
                            Utility.ShowToast(obj.getString("message"), getApplicationContext());
                        } catch (UnsupportedEncodingException | JSONException e) {
                            throw new RuntimeException(e);
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //Handle error
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Log.e("error====>", error.getMessage().toString());
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                String deviceNo = fileNametxt.getText().toString().trim();
                deviceNo.replace(".xls", "");
                params.put("type", type);
                params.put("DeviceNO", deviceNo);
                params.put("columnCount", String.valueOf(selectedFile.length()));
                //Params
                return params;
            }

            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                params.put("excel", new DataPart(fileNametxt.getText().toString().trim(), Utility.getFileData(selectedFile)));
                return params;
            }
        };

        //I used this because it was sending the file twice to the server
        volleyMultipartRequest.setRetryPolicy(
                new DefaultRetryPolicy(0, -1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(volleyMultipartRequest);
    }

    /*-------------------------------------------------------------Extra Methods-----------------------------------------------------------------------------*/

    private void connectToBluetoothSocket() {
        try {
            bluetoothSocket = null;
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(pairedDeviceList.get(selectedIndex).getDeviceAddress());//connects to the device's address and checks if it's available

            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(my_UUID);//create a RFCOMM (SPP) connection
            bluetoothAdapter.cancelDiscovery();
            if (!bluetoothSocket.isConnected())
                bluetoothSocket.connect();


        } catch (Exception e) {
            Utility.hideProgressDialogue();
            runOnUiThread(() -> Utility.ShowToast(getResources().getString(R.string.pairedDevice), getApplicationContext()));
            e.printStackTrace();
        }

    }

    public String getMediaFilePath(String type, String name) {

        File root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "DataLogger");

        File directory = new File(root.getAbsolutePath() + type); //it is my root directory

        try {
            if (!directory.exists()) {
                directory.mkdirs();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create a media file name
        return directory.getPath() + File.separator + name;
    }

    public static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
            return true;
        }

        return false;
    }

    public static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    private void DataExtractPopup() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dataextrationdialog, null);
        final androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this, R.style.MyDialogTheme);

        builder.setView(layout);
        builder.setCancelable(true);
        alertDialog = builder.create();

        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        alertDialog.getWindow().setGravity(Gravity.CENTER);
        alertDialog.show();

        TextView devicedata = layout.findViewById(R.id.devicedata);
        TextView dongale = layout.findViewById(R.id.dongale);
        TextView dongale5year = layout.findViewById(R.id.dongalefiveyear);
        TextView cancel = layout.findViewById(R.id.cancel);

        devicedata.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dirName = getMediaFilePath("Data Extract", "Device" + pairedDeviceList.get(selectedIndex).getDeviceName() + "_" + Calendar.getInstance().getTimeInMillis() + ".xls");

                if (!dirName.isEmpty()) {
                    if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
                        Log.e("Failed", "Storage not available or read only");
                    } else {
                        kk = 0;
                        mmCount = 0;
                        mPostionFinal = 0;
                        mBoolflag = false;
                        mCheckCLICKDayORMonth = 1;
                        if (mMonthHeaderList.size() > 0)
                            mMonthHeaderList.clear();
                        new BluetoothCommunicationGetDeviceYearlyData().execute(":MLENGTH#", ":MLENGTH#", "OKAY");


                    }
                }
            }
        });


        dongale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                selectMonthdialog();

            }
        });

        dongale5year.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                kk = 0;
                mmCount = 0;
                mBoolflag = false;
                mPostionFinal = 0;
                mCheckCLICKDayORMonth = 0;
                if (mMonthHeaderList.size() > 0)
                    mMonthHeaderList.clear();
                dirName = getMediaFilePath("Data Extract", "Dongle5Year_" + pairedDeviceList.get(selectedIndex).getDeviceName() + "_" + Calendar.getInstance().getTimeInMillis() + ".xls");


                new BluetoothCommunicationGet5YearDongleLength().execute(":CLENGTH#", ":CLENGTH#", "OKAY");

            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
    }

    private void selectMonthdialog() {

        LayoutInflater inflater = (LayoutInflater) getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dataextrationmonth, null);
        final androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this, R.style.MyDialogTheme);

        builder.setView(layout);
        builder.setCancelable(true);
        alertDialogMonth = builder.create();

        alertDialogMonth.setCanceledOnTouchOutside(true);
        alertDialogMonth.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        alertDialogMonth.getWindow().setGravity(Gravity.CENTER);
        alertDialogMonth.show();

        TextView month1 = layout.findViewById(R.id.month1);
        TextView month2 = layout.findViewById(R.id.month2);
        TextView month3 = layout.findViewById(R.id.month3);
        TextView month4 = layout.findViewById(R.id.month4);
        TextView month5 = layout.findViewById(R.id.month5);
        TextView month6 = layout.findViewById(R.id.month6);
        TextView month7 = layout.findViewById(R.id.month7);
        TextView month8 = layout.findViewById(R.id.month8);
        TextView month9 = layout.findViewById(R.id.month9);
        TextView month10 = layout.findViewById(R.id.month10);
        TextView month11 = layout.findViewById(R.id.month11);
        TextView month12 = layout.findViewById(R.id.month12);

        alertDialog.dismiss();

        month1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                monthValue = "01";
                dongaleDataExtrationMonthly();

            }
        });

        month2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                monthValue = "02";
                dongaleDataExtrationMonthly();
            }
        });
        month3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                monthValue = "03";
                dongaleDataExtrationMonthly();
            }
        });
        month4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                monthValue = "04";
                dongaleDataExtrationMonthly();
            }
        });
        month5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                monthValue = "05";
                dongaleDataExtrationMonthly();
            }
        });
        month6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                monthValue = "06";
                dongaleDataExtrationMonthly();
            }
        });
        month7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                monthValue = "07";
                dongaleDataExtrationMonthly();
            }
        });
        month8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                monthValue = "08";
                dongaleDataExtrationMonthly();
            }
        });
        month9.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                monthValue = "09";
                dongaleDataExtrationMonthly();
            }
        });
        month10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                monthValue = "10";
                dongaleDataExtrationMonthly();
            }
        });
        month11.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                monthValue = "11";
                dongaleDataExtrationMonthly();
            }
        });
        month12.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                monthValue = "12";
                dongaleDataExtrationMonthly();
            }
        });

    }

    private void dongaleDataExtrationMonthly() {
        dirName = getMediaFilePath("Data Extract", "Dongle_" + monthValue + "_" + pairedDeviceList.get(selectedIndex).getDeviceName() + "_" + Calendar.getInstance().getTimeInMillis() + ".xls");
        if (!dirName.isEmpty()) {
            if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
                Log.e("Failed", "Storage not available or read only");

            } else {
                kk = 0;
                mmCount = 0;
                mPostionFinal = 0;
                mBoolflag = false;
                mCheckCLICKDayORMonth = 1;
                if (mMonthHeaderList.size() > 0)
                    mMonthHeaderList.clear();

                       // new BluetoothCommunicationGetDongleYearlyData().execute(":YLENGTH#", ":YLENGTH#", "OKAY");

                        new BluetoothCommunicationGetDongleYearlyData().execute(":CLENGTH#", ":CDATA#", "START");
                    }
                }
            }
        });


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ATTACHMENT_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                Uri mImageCaptureUri = data.getData();
                String[] fileName = FileUtils.getPath(this, data.getData()).split("/");
                String finalFileName = fileName[fileName.length - 1];
                filePath = FileUtils.getPath(this, data.getData());

                // open the user-picked file for reading:
                InputStream in = getContentResolver().openInputStream(mImageCaptureUri);
                // now read the content:
                reader = new BufferedReader(new InputStreamReader(in));
                String line;
                StringBuilder builder = new StringBuilder();

                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                if (finalFileName.contains(".xls")) {
                    selectedFile = new File(filePath);
                    fileNametxt.setText(finalFileName);
                    if (finalFileName.contains("Dongle_") || finalFileName.contains("Dongle")) {
                        type = "DongleMonth";
                    } else {
                        type = "Month";
                    }

                } else {

                    Utility.ShowToast("Please Select file from Data Logger Folder this file is not valid", getApplicationContext());
                }

            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


}