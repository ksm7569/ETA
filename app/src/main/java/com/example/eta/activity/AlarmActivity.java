package com.example.eta.activity;

import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

import com.example.eta.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AlarmActivity extends AppCompatActivity {

    private Ringtone ringtone;
    private TextView textAlarmTime;
    private TextView textUserInfo;
    private Button buttonDismiss;
    private Button buttonSnooze;

    // ChatActivity에서 전달받을 데이터
    private String nickname;
    private String userId;
    private String chatRoomId;
    private String roomName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        // ChatActivity에서 전달받은 데이터 처리
        getIntentData();

        // 잠금화면 위로 알람 띄우기
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            );
        }

        // 액션바 설정
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("알람 - " + (roomName != null ? roomName : "ETA"));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initViews();
        startAlarmSound();
        setupClickListeners();
    }

    private void getIntentData() {
        // ChatActivity에서 전달받은 데이터
        nickname = getIntent().getStringExtra("nickname");
        userId = getIntent().getStringExtra("userId");
        chatRoomId = getIntent().getStringExtra("chatRoomId");
        roomName = getIntent().getStringExtra("roomName");

        // 로그로 확인
        android.util.Log.d("AlarmActivity", "받은 데이터 - 닉네임: " + nickname + ", 방: " + roomName);
    }

    private void initViews() {
        textAlarmTime = findViewById(R.id.text_alarm_time);
        textUserInfo = findViewById(R.id.text_user_info);
        buttonDismiss = findViewById(R.id.button_dismiss);
        buttonSnooze = findViewById(R.id.button_snooze);

        // 다크 테마 적용
        textAlarmTime.setTextColor(getResources().getColor(R.color.text_primary));
        buttonDismiss.setBackgroundColor(getResources().getColor(R.color.button_primary));
        buttonDismiss.setTextColor(getResources().getColor(R.color.text_primary));
        buttonSnooze.setBackgroundColor(getResources().getColor(R.color.button_secondary));
        buttonSnooze.setTextColor(getResources().getColor(R.color.text_primary));

        // 현재 시간 표시
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        textAlarmTime.setText(sdf.format(new Date()));

        // 사용자 정보 표시 (이모지 제거)
        if (textUserInfo != null && nickname != null) {
            textUserInfo.setText(nickname + "님의 알람");
            textUserInfo.setTextColor(getResources().getColor(R.color.text_secondary));
        }
    }

    private void setupClickListeners() {
        buttonDismiss.setOnClickListener(v -> {
            stopAlarmSound();

            // 알람 종료 메시지 (이모지 제거)
            if (nickname != null) {
                Toast.makeText(this, nickname + "님, 알람을 종료합니다", Toast.LENGTH_SHORT).show();
            }

            finish();
        });

        buttonSnooze.setOnClickListener(v -> {
            stopAlarmSound();
            Toast.makeText(this, "5분 후 다시 알람이 울립니다", Toast.LENGTH_SHORT).show();
            // TODO: 5분 후 스누즈 알람 설정
            finish();
        });
    }

    private void startAlarmSound() {
        // 기본 알람 소리 울리기
        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        ringtone = RingtoneManager.getRingtone(this, alarmUri);
        if (ringtone != null) {
            ringtone.play();
        }
    }

    private void stopAlarmSound() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlarmSound();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            stopAlarmSound();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
