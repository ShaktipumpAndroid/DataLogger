package com.datalogger.utilis;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.widget.Toast;

import com.datalogger.R;

import java.util.UUID;

public class Utility {

    public static UUID my_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public static ProgressDialog progressDialog;
    public static boolean isBluetoothEnabled() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return mBluetoothAdapter.isEnabled();

    }

    public static void ShowToast(String text, Context context) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }


    public static void showProgressDialogue(Context context){
        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(context.getResources().getString(R.string.loading));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    public void hideProgressDialogue(){
        if(progressDialog!=null && progressDialog.isShowing()){
            progressDialog.dismiss();
        }
    }

}
