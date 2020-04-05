package com.example.mytrackingapp.utility;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.example.mytrackingapp.R;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.example.mytrackingapp.utility.Constants.ACCOUNT_ID;
import static com.example.mytrackingapp.utility.Constants.APP_NAME;
import static com.example.mytrackingapp.utility.Constants.NOTIFICATION_CHANNEL_ID;
import static com.example.mytrackingapp.utility.Constants.USER_ID;

public class Utils {

    public static void toastIfInDevEnv(Context ctx, CharSequence charSequence){
        try {

            if(/*TODO check if in debug &&*/ true)
                Toast.makeText(ctx,charSequence, Toast.LENGTH_SHORT).show();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static boolean isMockSettingsON(Context context) {
        // returns true if mock location enabled, false if not enabled.
        return !Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ALLOW_MOCK_LOCATION).equals("0");
    }

    public static int getBatteryPercentage(Context context){

        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, iFilter);

        if (batteryStatus!=null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            return level;
        }

        return -1;
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static boolean areLocationServicesEnabled(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){

            int locationMode = 0;
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        }
        else{
            String locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    public static String getDateTimeFormattedString(DateTime dateTime, String newFormat){
        DateTimeFormatter formatter = DateTimeFormat.forPattern(newFormat).withLocale(Locale.US);
        return formatter.print(dateTime);
    }


    public static String getFormattedDateForMs(long timeInMs, String dateFormat) {

        DateTime dateTime = new DateTime(timeInMs);
        return getDateTimeFormattedString(dateTime, dateFormat);
    }

    private long getDiffBetweenTimes(DateTime fromTime, DateTime toTime){
        return toTime.getMillis() - fromTime.getMillis();
    }

    public static float calculateDisplacement(Location location1, Location locationTo) {

        float[] results = new float[1];
        Location.distanceBetween(location1.getLatitude(),
                location1.getLongitude(),
                locationTo.getLatitude(),
                locationTo.getLongitude(),
                results);

        return results[0];

    }

    public static float calculateSpeed(DateTime fromTime, DateTime toTime, float distance) {

        if (fromTime != null){
            long deltaTime = toTime.getMillis() - fromTime.getMillis();
            return distance * 60 * 60 / deltaTime;

        }else
            return 0;
    }

    public static String getFormattedDateToSend(String date, String oldFormatStr, String newFormatStr) {

        if(date!=null) {
            date = date.trim();

            if (!date.equals("")) {
                try {
                    SimpleDateFormat oldFormat = new SimpleDateFormat(oldFormatStr, Locale.US);
                    Date oldDate = oldFormat.parse(date);

                    SimpleDateFormat newFormat = new SimpleDateFormat(newFormatStr, Locale.US);

                    return newFormat.format(oldDate);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }


    public static JSONObject prepareJsonObject(String strJson) {

        JSONObject json;
        try {

            if(strJson == null)
                Log.d(APP_NAME, "strJSON is null!");

            json = new JSONObject(strJson);

        } catch (Exception e) {
            e.printStackTrace();
            json = new JSONObject();
        }
        return json;

    }

    public static JSONObject newJSONObject(Object ... keyValues){
        JSONObject jsonObject = new JSONObject();
        for(int i=0; i<keyValues.length; i+=2){
            put(jsonObject, (String)keyValues[i], keyValues[i+1]);
        }
        return jsonObject;
    }

    public static JSONObject newJsonObject(String key, Object value){
        JSONObject jsonObject = new JSONObject();
        put(jsonObject, key, value);
        return jsonObject;

    }

    public static void put(JSONObject json, String key, Object data) {
        try {
            json.put(key, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static JSONObject prepareCurrentAccountDetailsDTOJson(Context context, String accountId) {
        JSONObject accountDetailsDTOJson;
        accountDetailsDTOJson = new JSONObject();
        Utils.put(accountDetailsDTOJson, ACCOUNT_ID, accountId);
        Utils.put(accountDetailsDTOJson, USER_ID, "USER_ID");
        return accountDetailsDTOJson;
    }

    public static void dismissCustomNotification(Context context, int notiId){
        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(notiId);
        }
    }

    public static void showCustomNotification(Context context, String msg, int notiId, Intent openAppIntent) {

        NotificationCompat.Builder notification = getCustomNotification(context, null, msg, openAppIntent,
                R.drawable.splash_launcher);

        NotificationManager ntfManager = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
//		notification.setStyle(new NotificationCompat.BigTextStyle().bigText(msg));
        if (ntfManager != null) {
            ntfManager.notify(notiId, notification.build());
        }

    }

    public static NotificationCompat.Builder getCustomNotification(Context context, String contentTitle, String contentText,
                                                                   Intent openAppIntent, int drawableId) {

        if (contentTitle == null)
            contentTitle = "Tracker";


        Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        PendingIntent openAppPendingIntent = PendingIntent.getBroadcast(context, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Bitmap circularIcon = getCircleBitmap(BitmapFactory.decodeResource(context.getResources(),
                drawableId));


        return new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.h_36x36)
                .setLargeIcon(circularIcon)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setSound(notificationSound)
                .setAutoCancel(true)
                .setContentIntent(openAppPendingIntent);
    }

    public static Bitmap getCircleBitmap(Bitmap bitmap) {
        final Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        //int sqLen = bitmap.getWidth() <= bitmap.getHeight() ? bitmap.getWidth() : bitmap.getHeight();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawOval(rectF, paint);
        //canvas.drawRoundRect(rectF, bitmap.getWidth(), bitmap.getHeight(), paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        //bitmap.recycle();

        return output;
    }
}
