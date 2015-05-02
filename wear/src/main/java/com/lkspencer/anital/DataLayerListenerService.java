package com.lkspencer.anital;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.nio.ByteBuffer;

/**
 * Created by Kirk on 8/12/2014.
 * Listener Service
 */
public class DataLayerListenerService extends WearableListenerService {

  @Override public void onMessageReceived(MessageEvent messageEvent) {
    if (messageEvent == null) return;

    String path = messageEvent.getPath();
    if ("Anital_SensorSpeed".equalsIgnoreCase(path)) {
      ByteBuffer wrapped = ByteBuffer.wrap(messageEvent.getData());
      int Anital_SensorSpeed = wrapped.getInt();

      SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
      preferences.edit().putInt("Anital_SensorSpeed", Anital_SensorSpeed).apply();
    } else if ("show_digital".equalsIgnoreCase(path)) {
      ByteBuffer wrapped = ByteBuffer.wrap(messageEvent.getData());
      boolean show_digital = wrapped.getInt() != 0;

      SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
      preferences.edit().putBoolean("Anital_ShowDigitalTime", show_digital).apply();
    } else if ("show_day_of_week".equalsIgnoreCase(path)) {
      ByteBuffer wrapped = ByteBuffer.wrap(messageEvent.getData());
      boolean show_day_of_week = wrapped.getInt() != 0;

      SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
      preferences.edit().putBoolean("Anital_ShowDayOfWeek", show_day_of_week).apply();
    } else if ("show_date".equalsIgnoreCase(path)) {
      ByteBuffer wrapped = ByteBuffer.wrap(messageEvent.getData());
      boolean show_date = wrapped.getInt() != 0;

      SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
      preferences.edit().putBoolean("Anital_ShowDate", show_date).apply();
    } else if ("show_battery".equalsIgnoreCase(path)) {
      ByteBuffer wrapped = ByteBuffer.wrap(messageEvent.getData());
      boolean show_battery = wrapped.getInt() != 0;

      SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
      preferences.edit().putBoolean("Anital_ShowBatteryPercentage", show_battery).apply();
    } else if ("Anital_PhonePercent".equalsIgnoreCase(path)) {
      ByteBuffer wrapped = ByteBuffer.wrap(messageEvent.getData());
      int Anital_PhonePercent = wrapped.getInt();
      Intent i = new Intent();
      i.setAction("Anital_PhonePercent");
      i.putExtra("percent", Anital_PhonePercent);
      this.sendBroadcast(i);
    }
    super.onMessageReceived(messageEvent);
  }

}
