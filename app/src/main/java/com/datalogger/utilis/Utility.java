package com.datalogger.utilis;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import com.datalogger.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Utility {

    public static Context context;
    private static String PREFERENCE = "DealLizard";
    public static ProgressDialog progressDialog;

    public static boolean isConnectingToInternet(Context context) {
        ConnectivityManager connectivity =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
                for (NetworkInfo anInfo : info)
                    if (anInfo.getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }

        }
        return false;
    }

    public static boolean isBluetoothEnabled() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return mBluetoothAdapter.isEnabled();

    }

    public static void ShowToast(String text, Context context) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    public static void showProgressDialogue(Context context) {
        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(context.getResources().getString(R.string.loading));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    public static void hideProgressDialogue() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    public static File commonDocumentDirPath(String FolderName) {
       File dir = null;
        if (Build.VERSION.SDK_INT >= 30) {
            dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + FolderName);
            System.out.println("dir_ in  = " + dir);
        } else {
            dir = new File(Environment.getExternalStorageDirectory() + "/" + FolderName);
            System.out.println("dir_ out  = " + dir);
        }


        if (!dir.exists()) {
            boolean success = false;
            try {
                success = dir.createNewFile();

            if (!success) {
                dir = null;
            }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return dir;
    }

    public static String getRealPathFromUri(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static byte[] getFileData(File selectedFile) {
        int size = (int) selectedFile.length();
        byte[] bytes = new byte[size];
        byte[] tmpBuff = new byte[size];

        try (FileInputStream inputStream = new FileInputStream(selectedFile)) {
            int read = inputStream.read(bytes, 0, size);
            if (read < size) {
                int remain = size - read;
                while (remain > 0) {
                    read = inputStream.read(tmpBuff, 0, remain);
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
                    remain -= read;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bytes;
    }

    // for username string preferences
    public static void setSharedPreference(Context mContext, String name,
                                           String value) {
        context = mContext;
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE, 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString(name, value);
        editor.commit();
    }

    public static String getSharedPreferences(Context context, String name) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE, 0);
        return settings.getString(name, "");
    }

    public static void removeFromSharedPreference(Context context,String name){
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE, 0);
        settings.edit().remove(name).apply();
    }

    public static void clearSharedPreference(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE, 0);
        settings.edit().clear().apply();
    }


}
