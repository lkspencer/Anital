/*
package com.lkspencer.anital;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.DigitalClock;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;


public class Anital extends Activity implements SensorEventListener {

  //Variables
  private Calendar mTime;
  private static final String ACTION_KEEP_WATCH_FACE_AWAKE = "intent.action.keep.watchface.awake";
  private static final String ACTION_KEEP_WATCH_FACE_ASLEEP = "intent.action.keep.watchface.asleep";
  private static final String ACTION_KEEP_WATCH_FACE_SLEEP = "intent.action.keep.watchface.sleep";
  private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
    {}
    @Override public void onReceive(Context context, Intent intent) {
      if (Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
        final String timeZone = intent.getStringExtra("time-zone");
        createTime(timeZone);
      }

      if (!ACTION_KEEP_WATCH_FACE_AWAKE.equals(intent.getAction())) {
        updateDate();
      }
    }
  };
  private SensorManager mSensorManager;
  private ImageView light;
  private class AnitalSettings {
    public int SensorSpeed;
    public boolean ShowDigitalTime;
    public boolean ShowDate;
    public boolean ShowBatteryPercentage;
    public boolean ShowDayOfWeek;
  }
  private AnitalSettings settings = new AnitalSettings();



  //Event Handlers
  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_anital);
    updateDate();
  }

  @Override protected void onResume() {
    super.onResume();
    LoadSettings();
    registerReceiver();
    showBackground();
    if (settings.ShowBatteryPercentage) {
      showPercentage();
    } else {
      hidePercentage();
    }
    if (settings.ShowDigitalTime) {
      showDigitalClock();
    } else {
      hideDigitalClock();
    }
    if (settings.ShowDayOfWeek) {
      showDayOfWeek();
    } else {
      hideDayOfWeek();
    }
    if (settings.ShowDate) {
      showDate();
    } else {
      hideDate();
    }
    if (settings.ShowBatteryPercentage) {
      showBatteryPercentage();
    } else {
      hideBatteryPercentage();
    }
    light = (ImageView)findViewById(R.id.light);
    mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    if (accelerometer != null) {
      mSensorManager.registerListener(this, accelerometer, settings.SensorSpeed);
    }
  }

  @Override protected void onPause() {
    unregisterReceiver();
    hideBackground();
    hidePercentage();
    if (mSensorManager != null) {
      mSensorManager.unregisterListener(this);
    }
    super.onPause();
  }

  @Override public final void onAccuracyChanged(Sensor sensor, int accuracy) { }

  @Override public final void onSensorChanged(SensorEvent event) {
    if (light != null && event != null) {
      light.setRotation(event.values[1] * 10.0f);
    }
  }



  //Methods
  private void LoadSettings() {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    settings.SensorSpeed = preferences.getInt("Anital_SensorSpeed", SensorManager.SENSOR_DELAY_UI);
    settings.ShowDigitalTime = preferences.getBoolean("Anital_ShowDigitalTime", true);
    settings.ShowDate = preferences.getBoolean("Anital_ShowDate", true);
    settings.ShowBatteryPercentage = preferences.getBoolean("Anital_ShowBatteryPercentage", true);
    settings.ShowDayOfWeek = preferences.getBoolean("Anital_ShowDayOfWeek", false);
    if (settings.ShowDayOfWeek) {
      settings.ShowDigitalTime = false;
    }
  }

  private void createTime(String timeZone) {
    if (timeZone != null) {
      mTime = Calendar.getInstance(TimeZone.getTimeZone(timeZone));
    } else {
      mTime = Calendar.getInstance();
    }
    updateDate();
  }

  private void registerReceiver() {
    final IntentFilter filter = new IntentFilter();

    filter.addAction(Intent.ACTION_TIME_TICK);
    filter.addAction(Intent.ACTION_TIME_CHANGED);
    filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
    filter.addAction(ACTION_KEEP_WATCH_FACE_AWAKE);
    filter.addAction(ACTION_KEEP_WATCH_FACE_ASLEEP);
    filter.addAction(ACTION_KEEP_WATCH_FACE_SLEEP);

    registerReceiver(mIntentReceiver, filter);
  }

  private void unregisterReceiver() {
    this.unregisterReceiver(mIntentReceiver);
  }

  private void updateDate() {
    mTime = new GregorianCalendar();
    SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy");
    sdf.setCalendar(mTime);
    TextView date = (TextView)findViewById(R.id.date);
    date.setText(sdf.format(mTime.getTime()));

    sdf = new SimpleDateFormat("EEEE");
    sdf.setCalendar(mTime);
    TextView day_of_week = (TextView)findViewById(R.id.day_of_week);
    day_of_week.setText(sdf.format(mTime.getTime()));
  }

  private void hideBackground() {
    ImageView background = (ImageView)findViewById(R.id.background);
    background.setVisibility(View.INVISIBLE);
  }

  private void showBackground() {
    ImageView background = (ImageView)findViewById(R.id.background);
    background.setVisibility(View.VISIBLE);
  }

  private void hidePercentage() {
    TextView watchBattery = (TextView)findViewById(R.id.watchBattery);
    watchBattery.setVisibility(View.INVISIBLE);
  }

  private void showPercentage() {
    updateWatchBatteryPercentage();
    TextView watchBattery = (TextView)findViewById(R.id.watchBattery);
    watchBattery.setVisibility(View.VISIBLE);
  }

  private void hideDigitalClock() {
    DigitalClock digital_clock = (DigitalClock)findViewById(R.id.digital_clock);
    digital_clock.setVisibility(View.INVISIBLE);
  }

  private void showDigitalClock() {
    DigitalClock digital_clock = (DigitalClock)findViewById(R.id.digital_clock);
    digital_clock.setVisibility(View.VISIBLE);
  }

  private void hideDayOfWeek() {
    TextView day_of_week = (TextView)findViewById(R.id.day_of_week);
    day_of_week.setVisibility(View.INVISIBLE);
  }

  private void showDayOfWeek() {
    TextView day_of_week = (TextView)findViewById(R.id.day_of_week);
    day_of_week.setVisibility(View.VISIBLE);
  }

  private void hideDate() {
    TextView date = (TextView)findViewById(R.id.date);
    date.setVisibility(View.INVISIBLE);
  }

  private void showDate() {
    TextView date = (TextView)findViewById(R.id.date);
    date.setVisibility(View.VISIBLE);
  }

  private void hideBatteryPercentage() {
    TextView watchBattery = (TextView)findViewById(R.id.watchBattery);
    watchBattery.setVisibility(View.INVISIBLE);
  }

  private void showBatteryPercentage() {
    TextView watchBattery = (TextView)findViewById(R.id.watchBattery);
    watchBattery.setVisibility(View.VISIBLE);
  }

  private void updateWatchBatteryPercentage() {
    TextView watchBattery = (TextView)findViewById(R.id.watchBattery);
    int watchPercentage = getWatchPercentage();
    watchBattery.setText(watchPercentage + "%");
    if (watchPercentage > 95) {
      watchBattery.setTextColor(Color.WHITE);
    } else if (watchPercentage > 60) {
      watchBattery.setTextColor(Color.GREEN);
    } else if (watchPercentage > 30) {
      watchBattery.setTextColor(Color.YELLOW);
    } else if (watchPercentage > 15) {
      watchBattery.setTextColor(Color.RED);
    }
  }

  private int getWatchPercentage() {
    IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    Intent batteryStatus = this.registerReceiver(null, ifilter);
    if (batteryStatus == null) return 0;

    int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
    int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

    return (int)((level / (float)scale) * 100);
  }

}
*/