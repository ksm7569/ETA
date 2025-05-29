package com.example.eta.adapter;

import android.content.Context;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eta.R;
import com.example.eta.model.AlarmItem;
import com.example.eta.receiver.AlarmReceiver;

import java.util.ArrayList;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder> {

    private final ArrayList<AlarmItem> alarmList;
    private final Context context;

    public AlarmAdapter(Context context, ArrayList<AlarmItem> alarmList) {
        this.context = context;
        this.alarmList = alarmList;
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alarm, parent, false);
        return new AlarmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        AlarmItem item = alarmList.get(position);

        holder.timeText.setText(item.getTimeText());  // getter 사용
        holder.timeText.setTextColor(context.getResources().getColor(R.color.text_primary));

        holder.cancelBtn.setBackgroundColor(context.getResources().getColor(R.color.button_secondary));
        holder.cancelBtn.setTextColor(context.getResources().getColor(R.color.text_primary));

        holder.cancelBtn.setOnClickListener(v -> {
            // 알람 취소
            Intent intent = new Intent(context, AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, item.getRequestCode(), intent, PendingIntent.FLAG_IMMUTABLE);  // getter 사용

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
            }

            // 리스트에서 제거
            alarmList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, alarmList.size());
        });
    }


    @Override
    public int getItemCount() {
        return alarmList.size();
    }

    public static class AlarmViewHolder extends RecyclerView.ViewHolder {
        TextView timeText;
        Button cancelBtn;

        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);
            timeText = itemView.findViewById(R.id.text_alarm_time);
            cancelBtn = itemView.findViewById(R.id.btn_cancel_alarm);
        }
    }
}
