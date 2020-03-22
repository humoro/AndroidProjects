package com.example.mycalendar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.Calendar;

public class CreateSchedule extends AppCompatActivity implements View.OnClickListener {

    private final static int MSG_NETOUTOFWORK = 0x001;
    private final static int MSG_CREATESCHEDULEFAULT = 0x010;
    private final static int MSG_CREATESCHEDULESUCC = 0x100;

    private ImageButton completeButton;
    private ImageButton timeSelectorButton;
    private UserAccount user;
    private EditText themeText;
    private EditText contentText;
    private TextView timeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle receive = getIntent().getExtras();
        if (receive != null) user = (UserAccount) receive.getSerializable(StringUtils.BundleAccountKey); // 获取对应的账户
        setContentView(R.layout.activity_create_schedule);
        ActivityStackUtils.addActivity(this);
        initView();
    }

    private void initView() {
        // 根据id找到每个View
        completeButton = findViewById(R.id.create_schedule_complete_button);
        timeSelectorButton = findViewById(R.id.create_schedule_select_time);
        timeText = findViewById(R.id.create_schedule_time_text);
        themeText = findViewById(R.id.create_schedule_new_schedule_theme);
        contentText = findViewById(R.id.create_schedule_new_schedule_content);
        //初始化每个控件的样式
        completeButton.setImageResource(R.mipmap.ic_complete_icon);
        timeSelectorButton.setImageResource(R.mipmap.ic_time_icon);
        completeButton.setBackgroundColor(Color.TRANSPARENT);
        timeSelectorButton.setBackgroundColor(Color.TRANSPARENT);
        refreshTimeText(Calendar.getInstance());
        completeButton.setOnClickListener(this);
        timeSelectorButton.setOnClickListener(this);
    }

    private void refreshTimeText(Calendar calendar) { // 刷新时间显示字符串
        timeText.setText(JTimeUtils.getDateString(calendar));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityStackUtils.removeActivity(this);
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_NETOUTOFWORK:
                {
                    ToastNetWorkOutOfTIme();
                }
                break;
                case MSG_CREATESCHEDULEFAULT:
                {
                    ToastCreateScheduleFault();
                }
                break;
                case MSG_CREATESCHEDULESUCC:
                {
                    finishThis();
                }
                break;
            }
        }
    };

    private void ToastNetWorkOutOfTIme() {
        Toast.makeText(this, "网络连接超时", Toast.LENGTH_LONG).show();
    }

    private void ToastCreateScheduleFault() {
        Toast.makeText(this, "创建日程失败", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.create_schedule_select_time: // 日期选择按钮
            {
                Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int date = calendar.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog dialog = new DatePickerDialog(this, setDateCallBack, year, month, date);
                dialog.show();
            }
                break;
            case R.id.create_schedule_complete_button: //创建完成通过获得的内容创建一个日程对象加入数据库并且提醒主数据库更新页面
            {
                String theme = themeText.getText().toString();
                String content = contentText.getText().toString();
                String time = timeText.getText().toString();
                Schedule schedule = new Schedule(user.getUserName(), theme, content, time);
                String username = user.getUserName();
                String id = schedule.getId();
                new Thread(() -> {
                    try{
                        HttpClient httpclient= new DefaultHttpClient();
                        HttpPost httpPost = new HttpPost("http://10.0.2.2:8080/calendarWeb_war_exploded/ScheduleCreate");//服务器地址，指向查询用户是否信息的servlet
                        ArrayList<NameValuePair> params= new ArrayList<>();//将id装入list
                        params.add(new BasicNameValuePair(StringUtils.HttpScheduleIdKey, id));
                        params.add(new BasicNameValuePair(StringUtils.HttpUserNameKey, username));
                        params.add(new BasicNameValuePair(StringUtils.HttpScheduleThemeKey, theme));
                        params.add(new BasicNameValuePair(StringUtils.HttpScheduleContentKey, content));
                        params.add(new BasicNameValuePair(StringUtils.HttpScheduleDateKey, time));
                        final UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "utf-8");
                        httpPost.setEntity(entity);
                        HttpResponse response = httpclient.execute(httpPost);
                        Message msg = new Message();
                        if (response.getStatusLine().getStatusCode() == 200) {
                            int ans = Integer.parseInt(EntityUtils.toString(response.getEntity()));
                            if (ans > 0) {
                                msg.what = MSG_CREATESCHEDULESUCC;
                            } else {
                                msg.what = MSG_CREATESCHEDULEFAULT;
                            }
                        } else {
                            msg.what = MSG_NETOUTOFWORK;
                        }
                        handler.sendMessage(msg);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
//                    DataBaseUtils.insertSchedule(schedule);
//                    Message msg = new Message();
//                    msg.what = 0x0;
//                    handler.sendMessage(msg);
                }).start();
            }
                break;
        }
    }

    private void finishThis() {
        this.finish();
    }

    private DatePickerDialog.OnDateSetListener setDateCallBack = (view, year, monthOfYear, dayOfMonth) -> {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, monthOfYear, dayOfMonth);
        refreshTimeText(calendar);
    };
}
