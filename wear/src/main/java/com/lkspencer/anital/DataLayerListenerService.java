package com.lkspencer.anital;

import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.widget.Toast;

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
    }
    super.onMessageReceived(messageEvent);
  }

}
