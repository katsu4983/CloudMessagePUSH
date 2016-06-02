package com.example.saito.cloudmessagepush;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.app.Activity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends Activity {

    // GCM関連
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    // デベロッパーコンソールで取得したプロジェクト番号
    private static final String SENDER_ID = "946520138020";

    private GoogleCloudMessaging gcm;
    private String registrationId = "";
    private static Context context;
    private AsyncTask<Void, Void, String> registerTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gcmRegister();
    }

    /**
     * GCMのレジストレーションIDを取得
     */
    private void gcmRegister() {
        context = getApplicationContext();

        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(context);
            registrationId = getRegistrationId();
        } else {
            Log.d("MainActivity", "端末にGooglePlayServiceAPKがありません");
        }

        if (registrationId.equals("")) {
            registerTask = new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    try {
                        registrationId = gcm.register(SENDER_ID);
                        sendRegistrationIdToAppServer(registrationId);
                        Log.d("MainActivity", registrationId);
                        storeRegistrationId(registrationId);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(String resultPostExecute) {
                    registerTask = null;
                }
            };
            registerTask.execute(null, null, null);
        }
    }

    /**
     * 端末のGooglePlayServiceAPKの有無のチェック
     */
    private boolean checkPlayServices() {
        final int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * 端末に保存されているレジストレーションIDの取得
     */
    private String getRegistrationId() {
        final SharedPreferences prefs = getGCMPreferences();
        registrationId = prefs.getString(PROPERTY_REG_ID, "");
        try {
            if (registrationId.equals("")) {
                return "";
            }
        } catch (Exception e) {
            return "";
        }
        final int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        final int currentVersion = getAppVersion();
        if (registeredVersion != currentVersion) {
            return "";
        }
        return registrationId;
    }

    /**
     * アプリケーションのバージョン情報を取得する
     */
    private static int getAppVersion() {
        try {
            final PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("パッケージが見つかりません:" + e);
        }
    }

    /**
     * アプリのプリファレンスを取得する
     */
    private SharedPreferences getGCMPreferences() {
        return getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
    }

    /**
     * レジストレーションIDの端末保存
     */
    private void storeRegistrationId(String regId) {
        final SharedPreferences prefs = getGCMPreferences();
        final int appVersion = getAppVersion();
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.apply();
    }

    /**
     * 自サーバへレジストリキーを通知する
     */
    public static boolean sendRegistrationIdToAppServer(String regId) {
        String serverUrl = "http://221.186.150.142:10080/cgi-bin/register.php";
        Map<String, String> params = new HashMap<String, String>();
        Log.d("MainActivity", regId);
        params.put("regId", regId);
        try {
            post(serverUrl, params);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void post(String endpoint, Map<String, String> params)
            throws IOException {
        URL url;
        try {
            url = new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("invalid url: " + endpoint);
        }
        StringBuilder bodyBuilder = new StringBuilder();
        Iterator<Map.Entry<String, String>> iterator = params.entrySet().iterator();
        // POSTするパラメータ
        while (iterator.hasNext()) {
            Map.Entry<String, String> param = iterator.next();
            bodyBuilder.append(param.getKey()).append('=')
                    .append(param.getValue());
            if (iterator.hasNext()) {
                bodyBuilder.append('&');
            }
        }
        String body = bodyBuilder.toString();
        byte[] bytes = body.getBytes();
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setFixedLengthStreamingMode(bytes.length);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded;charset=UTF-8");
            // ポスト送信
            OutputStream out = conn.getOutputStream();
            out.write(bytes);
            out.close();
            // サーバーレスポンス受信
            int status = conn.getResponseCode();
            if (status != 200) {
                throw new IOException("Post failed with error code " + status);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();
    }

    @Override
    protected void onDestroy() {
        if (registerTask != null) {
            registerTask.cancel(true);
        }
        gcm.close();
        super.onDestroy();
    }
}
