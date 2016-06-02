package com.example.saito.cloudmessagepush;
/**
 * Copyright(c) 2016- KDDI R&D Lab.
 * at Network Security Group
 * Google Cloud Messeging Service Sample
 *
 * Created by saito on 2016/05/26.
 */
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

public class GcmBroadcastReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // 受け取ったインテントの処理をGcmIntentServiceで行う
        final ComponentName comp = new ComponentName(context.getPackageName(),
                GcmIntentService.class.getName());

        // サービスを起動して、処理中スリープを制御する
        startWakefulService(context, (intent.setComponent(comp)));
        setResultCode(Activity.RESULT_OK);
    }
}
