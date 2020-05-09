package com.deanlib.alarm;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    TextView tvSequence;
    EditText etSequence;
    TextView tvSequenceDesc;
    Button btnEdit;
    Button btnOpen;
    Button btnClose;

    List<Long> mSequence;
    SharedPreferences mSharedPreferences;
    String mBeforeText;

    public static final String SPLIT_CHAR = "-";
    public static final String ARROW = "➜";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        fab.setVisibility(View.GONE);

        mSequence = new ArrayList<>();
        mSharedPreferences = getSharedPreferences("config",Context.MODE_PRIVATE);

        etSequence = findViewById(R.id.etSequence);
        tvSequence = findViewById(R.id.tvSequence);
        tvSequenceDesc = findViewById(R.id.tvSequenceDesc);
        btnEdit = findViewById(R.id.btnEdit);
        btnOpen = findViewById(R.id.btnOpen);
        btnClose = findViewById(R.id.btnClose);

        String sequenceStr = mSharedPreferences.getString("sequence", "30-5");
        tvSequence.setText(convertShowStauts(sequenceStr));

        btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSequence == null && mSequence.isEmpty()){
                    Toast.makeText(getApplicationContext(),R.string.sequence_empty,Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!isServiceRunning(MainActivity.this, "com.deanlib.alarm.AlarmService")) {

                    Intent intent = new Intent(MainActivity.this, AlarmService.class);
                    long[] arr = new long[mSequence.size()];
                    for (int i = 0;i<mSequence.size();i++){
                        arr[i] = mSequence.get(i);
                    }
                    intent.putExtra("sequence",arr);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent);
                    } else {
                        startService(intent);
                    }

                }else {
                    Toast.makeText(getApplicationContext(), R.string.alarm_working, Toast.LENGTH_SHORT).show();
                }
            }
        });
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isServiceRunning(MainActivity.this, "com.deanlib.alarm.AlarmService")){
                    Intent i = new Intent(MainActivity.this, AlarmService.class);
                    stopService(i);
                }
            }
        });

        etSequence.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED);
        etSequence.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        etSequence.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //mBeforeText不要使用 CharSequence 类型，不是一个值类型
                // 做为一个对象，数据会被修改
                mBeforeText = s.toString();

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (!TextUtils.isEmpty(s)) {
                    if (Pattern.matches("^[\\d-]+$", s)) {
                        tvSequence.setText(convertShowStauts(s.toString()));
                    } else {
                        etSequence.setText(mBeforeText);
                        etSequence.setSelection(mBeforeText.length()-1);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (etSequence.getVisibility() == View.VISIBLE){
                    //保存
                    String text = etSequence.getText().toString();
                    if (!TextUtils.isEmpty(text)) {
                        mSharedPreferences.edit().putString("sequence", text).apply();
                    }
                    etSequence.setVisibility(View.GONE);
                    tvSequenceDesc.setVisibility(View.GONE);
                    btnEdit.setText(R.string.edit);
                    btnOpen.setEnabled(true);

                }else {
                    //编辑
                    etSequence.setVisibility(View.VISIBLE);
                    tvSequenceDesc.setVisibility(View.VISIBLE);
                    btnEdit.setText(R.string.save);
                    etSequence.setText(convertEditStatus());
                    btnOpen.setEnabled(false);
                }
            }
        });
    }

    private String convertEditStatus(){
        StringBuffer edit = new StringBuffer();
        if (mSequence!=null){
            for (Long l : mSequence){
                edit.append(l + SPLIT_CHAR);
            }
            if (edit.length()>0) {
                edit.deleteCharAt(edit.length() - 1);
            }
        }

        return edit.toString();
    }

    private String convertShowStauts(String edit){
        StringBuffer show = new StringBuffer();
        if (!TextUtils.isEmpty(edit)){
            mSequence.clear();
            String[] split = edit.split(SPLIT_CHAR);
            for (String s : split){
                if (!TextUtils.isEmpty(s) && TextUtils.isDigitsOnly(s)){
                    mSequence.add(Long.valueOf(s));
                    show.append(s + "s " + ARROW + " ");
                }
            }

        }
        return show.toString();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * 判断应用是否运行在前端
     *
     * @param context
     * @return
     */
    public static boolean isAppInForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(context.getPackageName())) {
                return appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
            }
        }
        return false;
    }

    /**
     * 判断服务是否在运行
     *
     * @param mContext
     * @param className
     * @return
     */
    public static boolean isServiceRunning(Context mContext, String className) {
        boolean isRunning = false;
        ActivityManager activityManager = (ActivityManager)
                mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList = activityManager.getRunningServices(30);

        if (!(serviceList.size() > 0)) {
            return false;
        }

        for (int i = 0; i < serviceList.size(); i++) {
            if (serviceList.get(i).service.getClassName().equals(className) == true) {
                isRunning = true;
                break;
            }
        }
        return isRunning;
    }
}
