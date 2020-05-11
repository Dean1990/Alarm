package com.deanlib.alarm;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import com.alibaba.fastjson.JSON;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    TextView tvSequence;
    EditText etSequence;
//    TextView tvSequenceDesc;
    Button btnEdit;
    Button btnOpen;
    TextView tvDownCount;
//    Button btnClose;
    View layoutEdit;
    CheckBox cbLoop;
    CheckBox cbRing;
    CheckBox cbVibration;
    ImageView imgLoop;
    ImageView imgRing;
    ImageView imgVibration;

    List<Long> mSeqList;
    SharedPreferences mSharedPreferences;
    String mBeforeText;

    public static final String SPLIT_CHAR = "-";
    public static final String ARROW = "➜";

    public static final String KEY_SEQUENCE = "sequence";

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

        mSeqList = new ArrayList<>();
        mSharedPreferences = getSharedPreferences("config",Context.MODE_PRIVATE);

        etSequence = findViewById(R.id.etSequence);
        tvSequence = findViewById(R.id.tvSequence);
//        tvSequenceDesc = findViewById(R.id.tvSequenceDesc);
        btnEdit = findViewById(R.id.btnEdit);
        btnOpen = findViewById(R.id.btnOpen);
        tvDownCount = findViewById(R.id.tvDownCount);
//        btnClose = findViewById(R.id.btnClose);
        layoutEdit = findViewById(R.id.layoutEdit);
        cbLoop = findViewById(R.id.cbLoop);
        cbRing = findViewById(R.id.cbRing);
        cbVibration = findViewById(R.id.cbVibration);
        imgLoop = findViewById(R.id.imgLoop);
        imgRing = findViewById(R.id.imgRing);
        imgVibration = findViewById(R.id.imgVibration);


        //init
        if (isAlarmWorking()){
            btnOpen.setText(R.string.close);
        }else {
            btnOpen.setText(R.string.open);
        }

        btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSeqList == null && mSeqList.isEmpty()){
                    Toast.makeText(getApplicationContext(),R.string.series_empty,Toast.LENGTH_SHORT).show();
                    return;
                }
                btnOpen.setEnabled(false);
                if (!isAlarmWorking()) {

                    Intent intent = new Intent(MainActivity.this, AlarmService.class);

                    intent.putExtra(KEY_SEQUENCE,packageSequence());

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent);
                    } else {
                        startService(intent);
                    }
                    btnOpen.setText(R.string.close);

                }else {
                    Intent i = new Intent(MainActivity.this, AlarmService.class);
                    stopService(i);
                    btnOpen.setText(R.string.open);
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
                if (layoutEdit.getVisibility() == View.VISIBLE){
                    //保存
                    Sequence sequence = packageSequence();
                    if (sequence!=null) {
                        mSharedPreferences.edit().putString(KEY_SEQUENCE, JSON.toJSONString(sequence)).apply();
                    }
                    layoutEdit.setVisibility(View.GONE);
                    btnEdit.setText(R.string.edit);
                    btnOpen.setEnabled(true);

                }else {
                    //编辑
                    layoutEdit.setVisibility(View.VISIBLE);
                    btnEdit.setText(R.string.save);
                    etSequence.setText(convertEditStatus());
                    btnOpen.setEnabled(false);
                }
            }
        });


        cbLoop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                imgLoop.setVisibility(isChecked?View.VISIBLE:View.GONE);
            }
        });

        cbRing.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                imgRing.setVisibility(isChecked?View.VISIBLE:View.GONE);
            }
        });

        cbVibration.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                imgVibration.setVisibility(isChecked?View.VISIBLE:View.GONE);
            }
        });

        String sequenceStr = mSharedPreferences.getString(KEY_SEQUENCE,"");
        if (!TextUtils.isEmpty(sequenceStr)) {
            Sequence sequence = JSON.parseObject(sequenceStr, Sequence.class);
            if (sequence!=null) {
                tvSequence.setText(convertShowStauts(sequence.getData()));
                cbLoop.setChecked(sequence.isLoop());
                cbRing.setChecked(sequence.isRing());
                cbVibration.setChecked(sequence.isVibration());
            }
        }else {
            tvSequence.setText(convertShowStauts("30-5"));
            cbLoop.setChecked(true);
            cbRing.setChecked(true);
            cbVibration.setChecked(true);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDownCountMessage(DownCount downCount){
        if (isAppInForeground(this) && tvDownCount!=null){
            tvDownCount.setText(downCount.getNum()+"");
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAlarmStatusMessage(AlarmStatus alarmStatus){
        if (alarmStatus.isWorking){
            btnOpen.setText(R.string.close);
            Toast.makeText(getApplicationContext(),R.string.alarm_open,Toast.LENGTH_SHORT).show();
        }else {
            btnOpen.setText(R.string.open);
            Toast.makeText(getApplicationContext(),R.string.alarm_close,Toast.LENGTH_SHORT).show();
        }
        btnOpen.setEnabled(true);
    }

    private Sequence packageSequence(){
        Sequence sequence = null;
        if (mSeqList!=null && mSeqList.size()>0) {
            long[] arr = new long[mSeqList.size()];
            for (int i = 0; i < mSeqList.size(); i++) {
                arr[i] = mSeqList.get(i);
            }
            sequence = new Sequence();
            sequence.setData(arr);
            sequence.setLoop(cbLoop.isChecked());
            sequence.setRing(cbRing.isChecked());
            sequence.setVibration(cbVibration.isChecked());
        }
        return sequence;
    }

    private boolean isAlarmWorking(){
        return isServiceRunning(MainActivity.this, "com.deanlib.alarm.AlarmService");
    }

    private String convertEditStatus(){
        StringBuffer edit = new StringBuffer();
        if (mSeqList!=null){
            for (Long l : mSeqList){
                edit.append(l + SPLIT_CHAR);
            }
            if (edit.length()>0) {
                edit.deleteCharAt(edit.length() - 1);
            }
        }

        return edit.toString();
    }

    private String convertShowStauts(long[] arr){
        StringBuffer show = new StringBuffer();
        if (arr!=null && arr.length>0){
            mSeqList.clear();
            for (long l : arr){
                if (l>0){
                    mSeqList.add(l);
                    show.append(l + "s " + ARROW + " ");
                }
            }

        }
        return show.toString();
    }

    private String convertShowStauts(String edit){
        StringBuffer show = new StringBuffer();
        if (!TextUtils.isEmpty(edit)){
            mSeqList.clear();
            String[] split = edit.split(SPLIT_CHAR);
            for (String s : split){
                if (!TextUtils.isEmpty(s) && TextUtils.isDigitsOnly(s)){
                    mSeqList.add(Long.valueOf(s));
                    show.append(s + "s " + ARROW + " ");
                }
            }

        }
        return show.toString();
    }

/*    @Override
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
    }*/

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
