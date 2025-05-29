package com.example.eta.activity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eta.R;
import com.example.eta.adapter.AlarmAdapter;
import com.example.eta.model.AlarmItem;
import com.example.eta.receiver.AlarmReceiver;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class AlarmManageActivity extends AppCompatActivity {

    private int hour, minute;
    private RecyclerView recyclerView;
    private AlarmAdapter alarmAdapter;
    private ArrayList<AlarmItem> alarmList;
    private Button setAlarmBtn;
    private TextView textCurrentTime;
    private String nickname;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_manage);

        // Intent에서 사용자 정보 받기
        nickname = getIntent().getStringExtra("nickname");
        userId = getIntent().getStringExtra("userId");

        // 액션바 설정
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("알람 관리");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initViews();
        setupRecyclerView();
        setupClickListeners();
        updateCurrentTime();
    }

    private void initViews() {
        setAlarmBtn = findViewById(R.id.btn_set_alarm);
        textCurrentTime = findViewById(R.id.text_current_time);
        recyclerView = findViewById(R.id.recycler_alarm_list);

        // 다크 테마 적용
        setAlarmBtn.setBackgroundColor(getResources().getColor(R.color.button_primary));
        setAlarmBtn.setTextColor(getResources().getColor(R.color.text_primary));
        textCurrentTime.setTextColor(getResources().getColor(R.color.text_primary));
        recyclerView.setBackgroundColor(getResources().getColor(R.color.background_color));
    }

    private void setupRecyclerView() {
        alarmList = new ArrayList<>();
        alarmAdapter = new AlarmAdapter(this, alarmList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(alarmAdapter);
    }

    private void setupClickListeners() {
        setAlarmBtn.setOnClickListener(v -> showTimePickerDialog());
    }

    private void updateCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        textCurrentTime.setText("현재 시간: " + sdf.format(Calendar.getInstance().getTime()));
    }

    private void showTimePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        hour = calendar.get(Calendar.HOUR_OF_DAY);
        minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePicker = new TimePickerDialog(
                this,
                android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
                (TimePicker view, int hourOfDay, int minute) -> {
                    this.hour = hourOfDay;
                    this.minute = minute;
                    setAlarm(hourOfDay, minute);
                },
                hour, minute, true
        );
        timePicker.show();
    }

    private void setAlarm(int hourOfDay, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // 시간이 이미 지났으면 다음 날로 설정
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        int requestCode = (int) System.currentTimeMillis();
        Intent intent = new Intent(getApplicationContext(), AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                getApplicationContext(), requestCode, intent, PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            // Android 12 이상에서는 정확한 알람 권한 요청 필요
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Intent permissionIntent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    permissionIntent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(permissionIntent);
                    return;
                }
            }

            // Doze 모드에서도 작동하는 알람 설정
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );

            // 알람 시간 표시
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String timeText = sdf.format(calendar.getTime());

            // 리스트에 추가
            alarmList.add(new AlarmItem(timeText, requestCode));
            alarmAdapter.notifyItemInserted(alarmList.size() - 1);

            Toast.makeText(this, "알람이 " + timeText + "에 설정되었습니다", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
