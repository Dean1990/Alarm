package com.deanlib.alarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * @auther Dean
 * @create 2020/5/9
 */
public class AlarmService extends Service {

    int mServiceNotifiyId = 101010;
    public static String CHANNEL_ID = "com.deanlib.alarm";
    public static String CHANNEL_SERVICE = "Service";

    Vibrator mVibrator;
    SoundPool mSoundPool;
    int mVoiceId;
    PowerManager.WakeLock mWakeLock;

    Sequence mSequence;
    int mPosition;
    long lastNum;//上一次触发闹钟的循环数
    long loopTimes;//循环次数

    static CompositeDisposable mDisposable = new CompositeDisposable();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, AlarmService.class.getName());
        mWakeLock.acquire();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent!=null) {
            mPosition = 0;
            mSequence = intent.getParcelableExtra(MainActivity.KEY_SEQUENCE);
            if (mSequence!=null) {

                String context = "Alarm Service";
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        AlarmService.this, -1
                        , new Intent(AlarmService.this, MainActivity.class)
                        , PendingIntent.FLAG_CANCEL_CURRENT);
                Notification notification = createNotification(AlarmService.this,
                        mServiceNotifiyId, CHANNEL_ID, CHANNEL_SERVICE,
                        R.mipmap.logo, R.mipmap.logo, "Alarm working",
                        getApplicationContext().getString(R.string.app_name), context, context, pendingIntent);

                startForeground(mServiceNotifiyId, notification);

                //init
                mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                SoundPool.Builder poolBuilder = new SoundPool.Builder();
                poolBuilder.setMaxStreams(1);
                AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
                attrBuilder.setLegacyStreamType(AudioManager.STREAM_ALARM);
                poolBuilder.setAudioAttributes(attrBuilder.build());
                mSoundPool = poolBuilder.build();
                mVoiceId = mSoundPool.load(this, R.raw.di, 1);
                loopTimes = 0;

                Disposable disposable = Observable.interval(1, TimeUnit.SECONDS).subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long num) throws Exception {
                        if (mSequence != null && mSequence.getData()!=null &&  mPosition < mSequence.getData().length) {
                            long time = mSequence.getData()[mPosition];
                            long temp = num - lastNum;
                            EventBus.getDefault().post(new DownCount(time - temp, loopTimes, mPosition));
                            if (temp == time) {
                                effect();
                                mPosition++;
                                lastNum = num;
                                if (mPosition >= mSequence.getData().length) {
                                    loopTimes++;
                                    if (mSequence.isLoop()) {
                                        mPosition = 0;
                                    }else {
                                        //close
                                        onDestroy();
                                    }
                                }
                            }
                        }
                    }
                });
                mDisposable.add(disposable);
                EventBus.getDefault().post(new AlarmStatus(true));
            }
        }
//        return super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }


    private void effect(){
        if (mSequence!=null) {
            if (mSequence.isVibration()) {
                mVibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
            }
            if (mSequence.isRing()) {
                mSoundPool.play(mVoiceId, 1, 1, 1, 0, 1);
            }
        }
    }


    public static Notification createNotification(
            Context context, int notifyId, String channelId, String channelName,
            int largeIconRid, int smallIconRid, String ticker, String contentTitle, String contentText,
            String bigText, PendingIntent pendingIntent
    ){
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, channelId);
        notificationBuilder.setSmallIcon(smallIconRid);
        //builder.setSmallIcon(android.os.Build.VERSION.SDK_INT>20?R.drawable.ic_launcher_round:R.drawable.ic_launcher);
        //builder.setColor(context.getResources().getColor(R.color.icon_blue));
        notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), largeIconRid));
        notificationBuilder.setAutoCancel(true);
//            notificationBuilder.setDefaults(Notification.DEFAULT_ALL);
        notificationBuilder.setTicker(ticker);
        notificationBuilder.setContentTitle(contentTitle);
        notificationBuilder.setContentText(contentText);
//            notificationBuilder.setProgress(0, 0, true);
        notificationBuilder.setWhen(System.currentTimeMillis());
        if (!TextUtils.isEmpty(bigText)) {
            notificationBuilder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(bigText));
        }
        if (pendingIntent!=null) {
            notificationBuilder.setContentIntent(pendingIntent);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName,
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);
            notificationBuilder.setChannelId(channelId);
        } else {
            notificationBuilder.setSound(null);
        }

        return notificationBuilder.build();
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().post(new AlarmStatus(false));
        mDisposable.clear();
        stopForeground(true);
        if (mWakeLock!=null){
            mWakeLock.release();
            mWakeLock = null;
        }
        super.onDestroy();
    }
}
