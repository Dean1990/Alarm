package com.deanlib.alarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;

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
    long[] mSequence;//存一个序列，单位是秒
    int mPosition;
    long lastNum;//上一次触发闹钟的循环数

    static CompositeDisposable mDisposable = new CompositeDisposable();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent!=null) {
            mPosition = 0;
            mSequence = intent.getLongArrayExtra("sequence");
            if (mSequence!=null) {

                String context = "Alarm Service";
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        AlarmService.this, -1
                        , new Intent(AlarmService.this, MainActivity.class)
                        , PendingIntent.FLAG_CANCEL_CURRENT);
                Notification notification = createNotification(AlarmService.this,
                        mServiceNotifiyId, CHANNEL_ID, CHANNEL_SERVICE,
                        R.mipmap.ic_launcher, R.mipmap.ic_launcher, "Alarm working",
                        getApplicationContext().getString(R.string.app_name), context, context, pendingIntent);

                startForeground(mServiceNotifiyId, notification);

                mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

                Disposable disposable = Observable.interval(1, TimeUnit.SECONDS).subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long num) throws Exception {
                        if (mSequence != null && mPosition < mSequence.length) {
                            long time = mSequence[mPosition];
                            if ((num - lastNum) == time) {
                                mVibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
                                mPosition++;
                                lastNum = num;
                                if (mPosition >= mSequence.length) {
                                    mPosition = 0;
                                }
                            }
                        }
                    }
                });
                mDisposable.add(disposable);
            }
        }
        return super.onStartCommand(intent, flags, startId);
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
        super.onDestroy();
        mDisposable.clear();
        stopForeground(true);
    }
}
