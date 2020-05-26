package com.deanlib.alarm;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.alibaba.fastjson.JSON;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.PowerManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.WindowManager;
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
    TextView tvSettingsBattery;
    TextView tvTimes;
    SwitchMaterial swKeepScreen;

    List<Long> mSeqList;
    SharedPreferences mSharedPreferences;
    String mBeforeText;

    int mCachePosition = -1;//设置了正在进行的 list 的 position
    int mBlinkCount; //闪烁计数
    SpannableString mSequenceSS;

    public static final String SPLIT_CHAR = "-";
    public static final String ARROW = "➜";
    public static final String FILL_ARROW = " " + ARROW + " ";

    public static final String KEY_SEQUENCE = "sequence";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getWindow().setNavigationBarColor(getResources().getColor(R.color.colorPrimary));

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
        mSharedPreferences = getSharedPreferences("config", Context.MODE_PRIVATE);

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
        tvSettingsBattery = findViewById(R.id.tvSettingsBattery);
        tvTimes = findViewById(R.id.tvTimes);
        swKeepScreen = findViewById(R.id.swKeepScreen);


        //init
        if (isAlarmWorking()) {
            btnOpen.setText(R.string.close);
        } else {
            btnOpen.setText(R.string.open);
        }

        btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSeqList == null && mSeqList.isEmpty()) {
                    Toast.makeText(getApplicationContext(), R.string.series_empty, Toast.LENGTH_SHORT).show();
                    return;
                }
                btnOpen.setEnabled(false);
                if (!isAlarmWorking()) {

                    Intent intent = new Intent(MainActivity.this, AlarmService.class);

                    intent.putExtra(KEY_SEQUENCE, packageSequence());

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent);
                    } else {
                        startService(intent);
                    }
                    btnOpen.setText(R.string.close);

                } else {
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
                        etSequence.setSelection(mBeforeText.length() - 1);
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
                if (layoutEdit.getVisibility() == View.VISIBLE) {
                    //保存
                    Sequence sequence = packageSequence();
                    if (sequence != null) {
                        mSharedPreferences.edit().putString(KEY_SEQUENCE, JSON.toJSONString(sequence)).apply();
                    }
                    layoutEdit.setVisibility(View.GONE);
                    btnEdit.setText(R.string.edit);
                    btnOpen.setEnabled(true);

                } else {
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
                imgLoop.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                String text = etSequence.getText().toString();
                if (!TextUtils.isEmpty(text)) {
                    tvSequence.setText(convertShowStauts(text, isChecked));
                }
            }
        });

        cbRing.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                imgRing.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

        cbVibration.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                imgVibration.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

        String sequenceStr = mSharedPreferences.getString(KEY_SEQUENCE, "");
        if (!TextUtils.isEmpty(sequenceStr)) {
            Sequence sequence = JSON.parseObject(sequenceStr, Sequence.class);
            if (sequence != null) {
                cbLoop.setChecked(sequence.isLoop());
                cbRing.setChecked(sequence.isRing());
                cbVibration.setChecked(sequence.isVibration());
                tvSequence.setText(convertShowStauts(sequence.getData()));
            }
        } else {
            cbLoop.setChecked(true);
            cbRing.setChecked(true);
            cbVibration.setChecked(true);
            tvSequence.setText(convertShowStauts("30-5"));
        }

        tvSettingsBattery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this).setTitle(R.string.statement)
                        .setMessage(R.string.statement_ignore_battery)
                        .setPositiveButton(R.string.to_settings, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                ignoreBatteryOptimization();
                            }
                        })
                        .setNegativeButton(R.string.cancel, null).show();

            }
        });

        swKeepScreen.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }else {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!hasIgnoredBatteryOptimization()){
            tvSettingsBattery.setVisibility(View.VISIBLE);
        }else {
            tvSettingsBattery.setVisibility(View.GONE);
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
    public void onDownCountMessage(DownCount downCount) {
        if (isAppInForeground(this) && tvDownCount != null) {
            tvDownCount.setText(downCount.getNum() + "");
            tvTimes.setText(downCount.getLoopTimes()+"");

            if (mCachePosition != downCount.getPosition()){
                //序列里 工作阶段变更了
                mCachePosition = downCount.getPosition();
                mSequenceSS = getMarkWorkingSequenceSS(mCachePosition);
                tvSequence.setText(mSequenceSS);
                mBlinkCount = 0;
            }else {
                mBlinkCount++;
                if (mBlinkCount%2 == 0){
                    if (mSequenceSS == null){
                        mSequenceSS = getMarkWorkingSequenceSS(mCachePosition);
                    }
                    tvSequence.setText(mSequenceSS);
                }else {
                    tvSequence.setText(tvSequence.getText().toString());
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAlarmStatusMessage(AlarmStatus alarmStatus) {
        if (alarmStatus.isWorking) {
            btnOpen.setText(R.string.close);
            Toast.makeText(getApplicationContext(), R.string.alarm_open, Toast.LENGTH_SHORT).show();
        } else {
            btnOpen.setText(R.string.open);
            Toast.makeText(getApplicationContext(), R.string.alarm_close, Toast.LENGTH_SHORT).show();
        }
        btnOpen.setEnabled(true);
    }

    private SpannableString getMarkWorkingSequenceSS(int position){
        String text = tvSequence.getText().toString();
        SpannableString ss = new SpannableString(text);
        if (!TextUtils.isEmpty(text) && position >= 0) {
            String[] split = text.split(FILL_ARROW);
            if (split.length > position) {
                int start = 0;
                for (int i = 0;i<split.length;i++){
                    if (i >= position){
                        break;
                    }
                    start = start + split[i].length() + FILL_ARROW.length();
                }
                int end = Math.min(start + split[position].length() + FILL_ARROW.length(), text.length());
                ss.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorPrimary)),
                        start, end, SpannableString.SPAN_INCLUSIVE_EXCLUSIVE);
            }
        }
        return ss;
    }

    private Sequence packageSequence() {
        Sequence sequence = null;
        if (mSeqList != null && mSeqList.size() > 0) {
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

    private boolean isAlarmWorking() {
        return isServiceRunning(MainActivity.this, "com.deanlib.alarm.AlarmService");
    }

    private String convertEditStatus() {
        StringBuffer edit = new StringBuffer();
        if (mSeqList != null) {
            for (Long l : mSeqList) {
                edit.append(l + SPLIT_CHAR);
            }
            if (edit.length() > 0) {
                edit.deleteCharAt(edit.length() - 1);
            }
        }

        return edit.toString();
    }

    private String convertShowStauts(long[] arr) {
        StringBuffer show = new StringBuffer();
        if (arr != null && arr.length > 0) {
            mSeqList.clear();
            for (long l : arr) {
                if (l > 0) {
                    mSeqList.add(l);
                    show.append(l + "s" + FILL_ARROW);
                }
            }

            if (!cbLoop.isChecked()) {
                show.delete(show.length() - FILL_ARROW.length(), show.length());
            }

        }
        return show.toString();
    }

    private String convertShowStauts(String edit) {
        return convertShowStauts(edit, cbLoop.isChecked());
    }

    private String convertShowStauts(String edit, boolean isLoop) {
        StringBuffer show = new StringBuffer();
        if (!TextUtils.isEmpty(edit)) {
            mSeqList.clear();
            String[] split = edit.split(SPLIT_CHAR);
            for (String s : split) {
                if (!TextUtils.isEmpty(s) && TextUtils.isDigitsOnly(s)) {
                    mSeqList.add(Long.valueOf(s));
                    show.append(s + "s" + FILL_ARROW);
                }
            }

            if (split.length > 0 && !isLoop) {
                show.delete(show.length() - FILL_ARROW.length(), show.length());
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

    /**
     * 判断 是否拿到忽略
     *
     * @return
     */
    private boolean hasIgnoredBatteryOptimization() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        return powerManager.isIgnoringBatteryOptimizations(this.getPackageName());
    }

    /**
     * 忽略电池优化
     */
    private void ignoreBatteryOptimization() {
        try {
            if (!hasIgnoredBatteryOptimization()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.settings_ignore_battery_optimizations_fail, Toast.LENGTH_LONG).show();
        }

    }
}