package com.lkspencer.anital;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Created by Kirk on 4/17/2015.
 *
 */
public class Anital extends CanvasWatchFaceService {

  @Override public Engine onCreateEngine() {
    return new Engine();
  }

  public class Engine extends CanvasWatchFaceService.Engine implements SensorEventListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    /* a time object */
    GregorianCalendar mTime;

    /* device features */
    boolean mLowBitAmbient;
    boolean mBurnInProtection;
    boolean mRegisteredTimeZoneReceiver;
    class AnitalSettings {
      public int SensorSpeed;
      public boolean ShowDigitalTime;
      public boolean ShowDate;
      public boolean ShowBatteryPercentage;
      public boolean ShowDayOfWeek;
    }
    AnitalSettings settings = new AnitalSettings();
    GoogleApiClient mGoogleApiClient = null;
    boolean mIsRound = false;

    /* values */
    float gravity;
    int batteryPercentage;
    int phoneBatteryPercentage;
    String googleApiAction;
    int mPhoneWidth = 0;
    int mLightBitmapWidth = 0;
    int mLightBitmapHeight = 0;

    /* graphic objects */
    Bitmap mBackgroundBitmap;
    Bitmap mBackgroundScaledBitmap;
    Bitmap mLightBitmap;
    Bitmap mTickBackground;
    Bitmap mLogo;
    Bitmap mWatch;
    Bitmap mPhone;
    Matrix matrix;
    Paint mHourPaint;
    Paint mHourDividerPaint;
    Paint mMinutePaint;
    Paint mSecondPaint;
    Paint mBoxPaint;
    Paint mBackgroundPaint;
    Paint mDigital;
    Paint mWatchPaint;
    Paint mPhonePaint;
    DateFormat timeFormat;

    /* handler to update the time once a second in interactive mode */
    private Handler mUpdateTimeHandler = new UpdateTimeHandler(this);

    /* receiver to update the time zone */
    final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
      {} @Override public void onReceive(Context context, Intent intent) {
        mTime.clear();
        mTime.setTimeZone(TimeZone.getTimeZone(intent.getStringExtra("time-zone")));
        mTime.setTimeInMillis(System.currentTimeMillis());
      }
    };

    /* receiver to update battery percentage */
    final BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
      {} @Override public void onReceive(Context context, Intent batteryStatus) {
        if (batteryStatus == null
            || !Intent.ACTION_BATTERY_CHANGED.equalsIgnoreCase(batteryStatus.getAction())) return;

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        batteryPercentage = (int)((level / (float)scale) * 100);
      }
    };

    /* receiver to update battery percentage */
    final BroadcastReceiver mPhoneBatteryReceiver = new BroadcastReceiver() {
      {} @Override public void onReceive(Context context, Intent batteryStatus) {
        if (batteryStatus == null || !"Anital_PhonePercent".equalsIgnoreCase(batteryStatus.getAction())) return;
        int percent = batteryStatus.getIntExtra("percent", -1);
        if (percent != -1) {
          phoneBatteryPercentage = percent;
        }
      }
    };



    /* Data Google API Client */
    @Override public void onConnected(Bundle bundle) {
      new Thread(new Runnable() {
        {} @Override public void run() {
          NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
          for(Node node : nodes.getNodes()) {
            /*MessageApi.SendMessageResult result = */Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), googleApiAction, new byte[0]).await();
          }
          mGoogleApiClient.disconnect();
        }
      }).start();
    }

    @Override public void onConnectionSuspended(int i) { }

    @Override public void onConnectionFailed(ConnectionResult connectionResult) { }



    /* Sensor Event Listener */
    @Override public final void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override public final void onSensorChanged(SensorEvent event) {
      gravity = event.values[1];
    }



    /* CanvasWatchFaceService.Engine */
    @Override public void onCreate(SurfaceHolder holder) {
      super.onCreate(holder);

      LoadSettings();

      timeFormat = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);

      SensorManager mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
      Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
      if (accelerometer != null) {
        mSensorManager.registerListener(this, accelerometer, settings.SensorSpeed);
      }

      IntentFilter intentWatchFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
      Anital.this.registerReceiver(mBatteryReceiver, intentWatchFilter);
      IntentFilter intentPhoneFilter = new IntentFilter("Anital_PhonePercent");
      Anital.this.registerReceiver(mPhoneBatteryReceiver, intentPhoneFilter);


      /* configure the system UI */
      setWatchFaceStyle(new WatchFaceStyle.Builder(Anital.this)
          .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
          .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
          .setStatusBarGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL)
          .setHotwordIndicatorGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL)
          .setViewProtection(WatchFaceStyle.PROTECT_STATUS_BAR | WatchFaceStyle.PROTECT_HOTWORD_INDICATOR)
          .setShowSystemUiTime(false)
          .build());

      /* load the background image */
      Resources resources = Anital.this.getResources();
      Drawable backgroundDrawable = resources.getDrawable(R.drawable.background, getTheme());
      mBackgroundBitmap = backgroundDrawable != null ? ((BitmapDrawable) backgroundDrawable).getBitmap() : null;

      Drawable logoDrawable = resources.getDrawable(R.drawable.logo_12, getTheme());
      mLogo = logoDrawable != null ? ((BitmapDrawable) logoDrawable).getBitmap() : null;

      /* load the background image */
      Drawable lightDrawable = resources.getDrawable(R.drawable.light, getTheme());
      mLightBitmap = lightDrawable != null ? ((BitmapDrawable) lightDrawable).getBitmap() : null;
      if (mLightBitmap != null) {
        mLightBitmapWidth = mLightBitmap.getWidth();
        mLightBitmapHeight = mLightBitmap.getHeight();
      }
      matrix = new Matrix();

      Drawable watchDrawable = resources.getDrawable(R.drawable.wear_icon, getTheme());
      mWatch = watchDrawable != null ? ((BitmapDrawable) watchDrawable).getBitmap() : null;

      Drawable phoneDrawable = resources.getDrawable(R.drawable.phone_icon, getTheme());
      mPhone = phoneDrawable != null ? ((BitmapDrawable) phoneDrawable).getBitmap() : null;
      if (mPhone != null) {
        mPhoneWidth = mPhone.getWidth();
      }

      /* create graphic styles */
      mHourPaint = new Paint();
      mHourPaint.setARGB(255, 200, 200, 200);
      mHourPaint.setStrokeWidth(5.0f);
      mHourPaint.setAntiAlias(true);
      mHourPaint.setStrokeCap(Paint.Cap.ROUND);

      mHourDividerPaint = new Paint();
      mHourDividerPaint.setARGB(255, 200, 200, 200);
      mHourDividerPaint.setStrokeWidth(1.0f);
      mHourDividerPaint.setAntiAlias(true);


      mMinutePaint = new Paint();
      mMinutePaint.setARGB(255, 200, 200, 200);
      mMinutePaint.setStrokeWidth(3.f);
      mMinutePaint.setAntiAlias(true);
      mMinutePaint.setStrokeCap(Paint.Cap.ROUND);

      mSecondPaint = new Paint();
      mSecondPaint.setARGB(255, 255, 0, 0);
      mSecondPaint.setStrokeWidth(2.f);
      mSecondPaint.setAntiAlias(true);
      mSecondPaint.setStrokeCap(Paint.Cap.ROUND);

      mBoxPaint = new Paint();
      mBoxPaint.setARGB(68, 0, 0, 0);

      mBackgroundPaint = new Paint();
      mBackgroundPaint.setARGB(255, 0, 0, 0);

      mDigital = new Paint();
      mDigital.setShadowLayer(5f, 0f, 0f, Color.BLACK);
      mDigital.setTextSize(30);
      mDigital.setFakeBoldText(true);
      mDigital.setTextAlign(Paint.Align.CENTER);
      mDigital.setARGB(255, 255, 255, 255);

      mWatchPaint = new Paint();
      mWatchPaint.setTextSize(16);
      mWatchPaint.setTextAlign(Paint.Align.LEFT);
      mWatchPaint.setARGB(255, 255, 255, 255);

      mPhonePaint = new Paint();
      mPhonePaint.setTextSize(16);
      mPhonePaint.setTextAlign(Paint.Align.RIGHT);
      mPhonePaint.setARGB(255, 255, 255, 255);

      /* allocate an object to hold the time */
      mTime = new GregorianCalendar();

      mGoogleApiClient = new GoogleApiClient.Builder(Anital.this)
          .addApi(Wearable.API)
          .addConnectionCallbacks(this)
          .addOnConnectionFailedListener(this)
          .build();
      googleApiAction = "Anital_PhonePercent";
      mGoogleApiClient.connect();
    }

    @Override public void onDestroy() {
      Anital.this.unregisterReceiver(mBatteryReceiver);
      Anital.this.unregisterReceiver(mPhoneBatteryReceiver);
      if (mGoogleApiClient != null) {
        googleApiAction = "Anital_Stop";
        mGoogleApiClient.connect();
      }
      super.onDestroy();
    }

    @Override public void onPropertiesChanged(Bundle properties) {
      super.onPropertiesChanged(properties);
      mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
      mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
    }

    @Override public void onTimeTick() {
      super.onTimeTick();

      invalidate();
    }

    @Override public void onAmbientModeChanged(boolean inAmbientMode) {
      super.onAmbientModeChanged(inAmbientMode);

      googleApiAction = "Anital_PhonePercent";
      mGoogleApiClient.connect();
      if (mLowBitAmbient) {
        boolean antiAlias = !inAmbientMode;
        mHourPaint.setAntiAlias(antiAlias);
        mMinutePaint.setAntiAlias(antiAlias);
      }
      invalidate();
      updateTimer();
    }

    @Override public void onDraw(Canvas canvas, Rect bounds) {
      mTime.clear();
      mTime.setTimeInMillis(System.currentTimeMillis());

      int width = bounds.width();
      int height = bounds.height();
      float centerX = width / 2f;
      float centerY = height / 2f;

      if (!isInAmbientMode()) {
        if (mBackgroundScaledBitmap == null
            || mBackgroundScaledBitmap.getWidth() != width
            || mBackgroundScaledBitmap.getHeight() != height) {
          mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap, width, height, true /* filter */);
        }

        canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);
        matrix.setRotate(gravity * 10f, mLightBitmapWidth / 2, mLightBitmapHeight / 2);
        matrix.postTranslate((-mLightBitmapWidth / 2 + centerX), (-mLightBitmapHeight / 2 + centerY));
        canvas.drawBitmap(mLightBitmap, matrix, null);

        if (mTickBackground == null) {
          setupMainBackground(canvas, centerX, centerY, width);
        }
        canvas.drawBitmap(mTickBackground, 0, 0, null);

        if (mIsRound) {
          canvas.drawText(batteryPercentage + "%", 85, centerY + 6, mWatchPaint);
          canvas.drawText(phoneBatteryPercentage + "%", width - 80, centerY + 6, mPhonePaint);
        } else {
          canvas.drawText(batteryPercentage + "%", 40, 30, mWatchPaint);
          canvas.drawText(phoneBatteryPercentage + "%", width - 40, 30, mPhonePaint);
        }
      } else {
        canvas.drawRect(0,0,width,height, mBackgroundPaint);
      }

      // Compute rotations and lengths for the clock hands.
      int seconds = mTime.get(GregorianCalendar.SECOND);
      int minutes = mTime.get(GregorianCalendar.MINUTE);
      int hours = mTime.get(GregorianCalendar.HOUR);
      float minRot = ((minutes + (seconds / 60f)) / 30f) * (float) Math.PI;
      float hrRot = ((hours + (minutes / 60f)) / 6f ) * (float) Math.PI;

      float minLength = centerX - 40;
      float hrLength = centerX - 80;

      // Draw the minute and hour hands.
      float hrX = (float) Math.sin(hrRot) * hrLength;
      float hrY = (float) -Math.cos(hrRot) * hrLength;
      canvas.drawLine(centerX, centerY, centerX + hrX, centerY + hrY, mHourPaint);
      float minX = (float) Math.sin(minRot) * minLength;
      float minY = (float) -Math.cos(minRot) * minLength;
      canvas.drawLine(centerX, centerY, centerX + minX, centerY + minY, mMinutePaint);

      // Only draw the second hand in interactive mode.
      if (!isInAmbientMode()) {
        float secLength = centerX - 20;
        int milliseconds = mTime.get(GregorianCalendar.MILLISECOND);
        float secRot = ((seconds + (milliseconds / 1000f)) / 30f) * (float)Math.PI;
        float secX = (float) Math.sin(secRot) * secLength;
        float secY = (float) -Math.cos(secRot) * secLength;
        canvas.drawLine(centerX, centerY, centerX + secX, centerY + secY, mSecondPaint);
      }

      timeFormat.setCalendar(mTime);
      canvas.drawText(timeFormat.format(mTime.getTime()), centerX, (centerY / 2) + 20, mDigital);

    }

    @Override public void onVisibilityChanged(boolean visible) {
      super.onVisibilityChanged(visible);
      /* the watch face became visible or invisible */

      if (visible) {
        registerReceiver();

        // Update time zone in case it changed while we weren't visible.
        mTime.clear();
        mTime.setTimeZone(TimeZone.getDefault());
        mTime.setTimeInMillis(System.currentTimeMillis());
      } else {
        unregisterReceiver();
      }

      updateTimer();
    }

    @Override public void onApplyWindowInsets(WindowInsets insets) {
      super.onApplyWindowInsets(insets);
      mIsRound = insets.isRound();
      //mChinSize = insets.getSystemWindowInsetBottom();
    }



    private void updateTimer() {
      mUpdateTimeHandler.removeMessages(UpdateTimeHandler.MSG_UPDATE_TIME);
      if (shouldTimerBeRunning()) {
        mUpdateTimeHandler.sendEmptyMessage(UpdateTimeHandler.MSG_UPDATE_TIME);
      }
    }

    public boolean shouldTimerBeRunning() {
      return isVisible() && !isInAmbientMode();
    }

    private void LoadSettings() {
      SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(Anital.this);
      settings.SensorSpeed = preferences.getInt("Anital_SensorSpeed", SensorManager.SENSOR_DELAY_FASTEST);
      settings.ShowDigitalTime = preferences.getBoolean("Anital_ShowDigitalTime", true);
      settings.ShowDate = preferences.getBoolean("Anital_ShowDate", true);
      settings.ShowBatteryPercentage = preferences.getBoolean("Anital_ShowBatteryPercentage", true);
      settings.ShowDayOfWeek = preferences.getBoolean("Anital_ShowDayOfWeek", false);
      if (settings.ShowDayOfWeek) {
        settings.ShowDigitalTime = false;
      }
    }

    private void registerReceiver() {
      if (mRegisteredTimeZoneReceiver) {
        return;
      }
      mRegisteredTimeZoneReceiver = true;
      IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
      Anital.this.registerReceiver(mTimeZoneReceiver, filter);
    }

    private void unregisterReceiver() {
      if (!mRegisteredTimeZoneReceiver) {
        return;
      }
      mRegisteredTimeZoneReceiver = false;
      Anital.this.unregisterReceiver(mTimeZoneReceiver);
    }

    private void setupMainBackground(Canvas canvas, float centerX, float centerY, int width) {
      mTickBackground = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
      Canvas blankCanvas = new Canvas(mTickBackground);
      drawHourLine(1f, 25, centerX, centerY, blankCanvas, mHourPaint);
      drawHourLine(1.2f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(1.4f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(1.6f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(1.8f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);

      drawHourLine(2f, 25, centerX, centerY, blankCanvas, mHourPaint);
      drawHourLine(2.2f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(2.4f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(2.6f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(2.8f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);

      drawHourLine(3f, 25, centerX, centerY, blankCanvas, mHourPaint);
      drawHourLine(3.2f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(3.4f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(3.6f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(3.8f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);

      drawHourLine(4f, 25, centerX, centerY, blankCanvas, mHourPaint);
      drawHourLine(4.2f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(4.4f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(4.6f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(4.8f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);

      drawHourLine(5f, 25, centerX, centerY, blankCanvas, mHourPaint);
      drawHourLine(5.4f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(5.6f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(5.8f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(5.2f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);

      drawHourLine(6f, 25, centerX, centerY, blankCanvas, mHourPaint);
      drawHourLine(6.2f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(6.4f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(6.6f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(6.8f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);

      drawHourLine(7f, 25, centerX, centerY, blankCanvas, mHourPaint);
      drawHourLine(7.2f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(7.4f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(7.6f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(7.8f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);

      drawHourLine(8f, 25, centerX, centerY, blankCanvas, mHourPaint);
      drawHourLine(8.2f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(8.4f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(8.6f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(8.8f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);

      drawHourLine(9f, 25, centerX, centerY, blankCanvas, mHourPaint);
      drawHourLine(9.2f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(9.4f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(9.6f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(9.8f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);

      drawHourLine(10f, 25, centerX, centerY, blankCanvas, mHourPaint);
      drawHourLine(10.2f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(10.4f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(10.6f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(10.8f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);

      drawHourLine(11f, 25, centerX, centerY, blankCanvas, mHourPaint);
      drawHourLine(11.2f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(11.4f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(11.6f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(11.8f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);

      blankCanvas.drawBitmap(mLogo, centerX - 23, 16, null);
      drawHourLine(12.2f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(12.4f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(12.6f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);
      drawHourLine(12.8f, 10, centerX, centerY, blankCanvas, mHourDividerPaint);

      if (mIsRound) {
        blankCanvas.drawBitmap(mWatch, 55, centerY - 15, null);
        blankCanvas.drawBitmap(mPhone, width - 55 - mPhoneWidth, centerY - 15, null);
      } else {
        blankCanvas.drawBitmap(mWatch, 10, 10, null);
        blankCanvas.drawBitmap(mPhone, width - 10 - mPhoneWidth, 8, null);
      }
    }

    private void drawHourLine(float hour, int length, float centerX, float centerY, Canvas canvas, Paint paint) {
      float hrRot = (hour / 6f ) * (float) Math.PI;
      float startX = (float) Math.sin(hrRot) * (centerX - 20 - length);
      float startY = (float) -Math.cos(hrRot) * (centerX - 20 - length);
      float endX = (float) Math.sin(hrRot) * (centerX - 20);
      float endY = (float) -Math.cos(hrRot) * (centerX - 20);
      canvas.drawLine(centerX + startX, centerY + startY, centerX + endX, centerY + endY, paint);
    }

  }

}
