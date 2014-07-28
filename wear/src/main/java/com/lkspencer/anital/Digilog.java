package com.lkspencer.anital;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class Digilog extends Activity implements SensorEventListener {
  private Calendar mTime;
  private static final String ACTION_KEEP_WATCH_FACE_AWAKE = "intent.action.keep.watchface.awake";
  private static final String ACTION_KEEP_WATCH_FACE_ASLEEP = "intent.action.keep.watchface.asleep";
  private static final String ACTION_KEEP_WATCH_FACE_SLEEP = "intent.action.keep.watchface.sleep";
  private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
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



  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_anital);
    updateDate();
    //Setting values to prevent lint tool from throwing a fit;
    m_total = 0;
    m_i = 0;
  }

  @Override protected void onResume() {
    super.onResume();
    showBackground();
    registerReceiver();
    showPercentage();
    light = (ImageView)findViewById(R.id.light);
    mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    if (accelerometer != null){
      mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
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

  /*
  NOTE: These have been pulled out to prevent memory allocation each time. The
        frequency at which this event handler is called is the primary reason
        for pulling anything out that I can to improve performance.
   */
  private float m_total = 0.0f;
  private int m_i = 0;
  private int m_length = 3;
  private float[] values = new float[m_length];
  @Override public final void onSensorChanged(SensorEvent event) {
    System.arraycopy(values, 0, values, 1, m_length - 1);
    values[0] = event.values[1];

    if (light != null) {
      m_total = 0.0f;
      for (m_i = 0; m_i < m_length; m_i++) {
        m_total += values[m_i];
      }
      light.setRotation((m_total * 3.0f) + values[0]);
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
