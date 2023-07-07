package com.datalogger.utilis;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import com.datalogger.R;

import java.io.File;
import java.io.IOException;

public class Utility {


    public static ProgressDialog progressDialog;

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

}
