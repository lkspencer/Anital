package com.lkspencer.anital;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Created by Kirk on 4/17/2015.
 *
 */
public class Anital extends CanvasWatchFaceService {

  private Engine engine;

  @Override public Engine onCreateEngine() {
    this.engine = new Engine();
    return this.engine;
  }

  public Engine getEngine() {
    return this.engine;
  }

  public class Engine extends CanvasWatchFaceService.Engine implements SensorEventListener {
    /* a time object */
    GregorianCalendar mTime;

    /* device features */
    boolean mLowBitAmbient;
    boolean mBurnInProtection;
    boolean mRegisteredTimeZoneReceiver;
    private class AnitalSettings {
      public int SensorSpeed;
      public boolean ShowDigitalTime;
      public boolean ShowDate;
      public boolean ShowBatteryPercentage;
      public boolean ShowDayOfWeek;
    }
    private AnitalSettings settings = new AnitalSettings();

    /* graphic objects */
    Bitmap mBackgroundBitmap;
    Bitmap mBackgroundScaledBitmap;
    Bitmap mLightBitmap;
    Matrix matrix;
    Paint mHourPaint;
    Paint mMinutePaint;
    Paint mSecondPaint;
    Paint mBoxPaint;
    Paint mBackgroundPaint;
    DateFormat timeFormat;

    /* handler to update the time once a second in interactive mode */
    private Handler mUpdateTimeHandler = new UpdateTimeHandler(Anital.this);

    /* receiver to update the time zone */
    final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
      {} @Override public void onReceive(Context context, Intent intent) {
        mTime.clear();
        mTime.setTimeZone(TimeZone.getTimeZone(intent.getStringExtra("time-zone")));
        mTime.setTimeInMillis(System.currentTimeMillis());
      }
    };



    @Override public void onCreate(SurfaceHolder holder) {
      super.onCreate(holder);

      LoadSettings();

      timeFormat = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);

      SensorManager mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
      Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
      if (accelerometer != null) {
        mSensorManager.registerListener(this, accelerometer, settings.SensorSpeed);
      }

      /* configure the system UI */
      setWatchFaceStyle(new WatchFaceStyle.Builder(Anital.this)
          .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
          .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
          .setShowSystemUiTime(false)
          .build());

      /* load the background image */
      Resources resources = Anital.this.getResources();
      Drawable backgroundDrawable = resources.getDrawable(R.drawable.background, getTheme());
      mBackgroundBitmap = backgroundDrawable != null ? ((BitmapDrawable) backgroundDrawable).getBitmap() : null;

      /* load the background image */
      Drawable lightDrawable = resources.getDrawable(R.drawable.light, getTheme());
      mLightBitmap = lightDrawable != null ? ((BitmapDrawable) lightDrawable).getBitmap() : null;
      matrix = new Matrix();

      /* create graphic styles */
      mHourPaint = new Paint();
      mHourPaint.setARGB(255, 200, 200, 200);
      mHourPaint.setStrokeWidth(5.0f);
      mHourPaint.setAntiAlias(true);
      mHourPaint.setStrokeCap(Paint.Cap.ROUND);

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

      /* allocate an object to hold the time */
      mTime = new GregorianCalendar();
    }

    @Override public final void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override public final void onSensorChanged(SensorEvent event) {
      if (mLightBitmap != null && !isInAmbientMode()) {
        matrix.setRotate(event.values[1] * 10.0f, mLightBitmap.getWidth() / 2, mLightBitmap.getHeight() / 2);
      }
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

      if (mLowBitAmbient) {
        boolean antiAlias = !inAmbientMode;
        mHourPaint.setAntiAlias(antiAlias);
        mMinutePaint.setAntiAlias(antiAlias);
        mSecondPaint.setAntiAlias(antiAlias);
        //mTickPaint.setAntiAlias(antiAlias);
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
        // Draw the background, scaled to fit.
        if (mBackgroundScaledBitmap == null
            || mBackgroundScaledBitmap.getWidth() != width
            || mBackgroundScaledBitmap.getHeight() != height) {
          mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap, width, height, true /* filter */);
        }

        canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);
        matrix.postTranslate((-mLightBitmap.getWidth() / 2 + centerX), (-mLightBitmap.getHeight() / 2 + centerY));
        canvas.drawBitmap(mLightBitmap, matrix, null);
        drawHourLine(1, centerX, centerY, canvas);
        drawHourLine(2, centerX, centerY, canvas);
        drawHourLine(3, centerX, centerY, canvas);
        drawHourLine(4, centerX, centerY, canvas);
        drawHourLine(5, centerX, centerY, canvas);
        drawHourLine(6, centerX, centerY, canvas);
        drawHourLine(7, centerX, centerY, canvas);
        drawHourLine(8, centerX, centerY, canvas);
        drawHourLine(9, centerX, centerY, canvas);
        drawHourLine(10, centerX, centerY, canvas);
        drawHourLine(11, centerX, centerY, canvas);
        drawHourLine(12, centerX, centerY, canvas);
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
        /*
        mSecondPaint.setTextSize(20);
        mSecondPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(String.valueOf(seconds), centerX + secX + 10, centerY + secY + 10, mSecondPaint);
        //*/
        canvas.drawRect(0, 0, width, 45, mBoxPaint);
      }

      mHourPaint.setTextSize(30);
      mHourPaint.setFakeBoldText(true);
      mHourPaint.setTextAlign(Paint.Align.CENTER);
      timeFormat.setCalendar(mTime);
      canvas.drawText(timeFormat.format(mTime.getTime()), width / 2, 35, mHourPaint);

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
        /*
        mTime.clear(TimeZone.getDefault().getID());
        mTime.setToNow();
        */
      } else {
        unregisterReceiver();
      }

      // Whether the timer should be running depends on whether we're visible and
      // whether we're in ambient mode), so we may need to start or stop the timer
      updateTimer();
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

    private void drawHourLine(int hour, float centerX, float centerY, Canvas canvas) {
      float hrRot = (hour / 6f ) * (float) Math.PI;
      float startX = (float) Math.sin(hrRot) * (centerX - 45);
      float startY = (float) -Math.cos(hrRot) * (centerX - 45);
      float endX = (float) Math.sin(hrRot) * (centerX - 20);
      float endY = (float) -Math.cos(hrRot) * (centerX - 20);
      canvas.drawLine(centerX + startX, centerY + startY, centerX + endX, centerY + endY, mHourPaint);
    }

  }

}
