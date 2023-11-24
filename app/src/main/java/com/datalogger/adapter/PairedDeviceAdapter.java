package com.datalogger.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.datalogger.R;
import com.datalogger.model.PairDeviceModel;

import java.util.List;

public class PairedDeviceAdapter extends RecyclerView.Adapter<PairedDeviceAdapter.ViewHolder> {
    Context mContext;

    private final List<PairDeviceModel> PairDeviceList;
    private static deviceSelectionListener deviceListener;


    public PairedDeviceAdapter(Context context, List<PairDeviceModel> listData) {
        PairDeviceList = listData;
        mContext = context;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem = layoutInflater.inflate(R.layout.device_list_item, parent, false);
        return new ViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        final PairDeviceModel pairDeviceModel = PairDeviceList.get(position);
             holder.setData(pairDeviceModel,position);

    }


    @Override
    public int getItemCount() {
        return PairDeviceList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView deviceName, deviceAddress;
        LinearLayout deviceCard;

        public ViewHolder(View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.deviceName);
            deviceAddress = itemView.findViewById(R.id.deviceAddress);
            deviceCard = itemView.findViewById(R.id.deviceCard);
        }

        public void setData(PairDeviceModel pairDeviceModel, int position) {
            deviceName.setText(pairDeviceModel.getDeviceName());
            deviceAddress.setText(pairDeviceModel.getDeviceAddress());

            deviceCard.setOnClickListener(v -> {
              //  Utility.ShowToast("MethodClick",itemView.getContext());
                deviceListener.DeviceSelectionListener(pairDeviceModel,position);
            });
        }
    }

    public interface deviceSelectionListener {
        void DeviceSelectionListener(PairDeviceModel pairDeviceModel, int position);
    }

    public void deviceSelection(deviceSelectionListener pairDevice) {
        try {
            deviceListener = pairDevice;
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
    }
}
