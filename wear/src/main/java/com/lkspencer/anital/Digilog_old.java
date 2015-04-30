/*
package com.lkspencer.anital;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.DigitalClock;

public class Digilog extends Activity {
  private static final String ACTION_KEEP_WATCH_FACE_AWAKE = "intent.action.keep.watchface.awake";
  private static final String ACTION_KEEP_WATCH_FACE_ASLEEP = "intent.action.keep.watchface.asleep";
  private static final String ACTION_KEEP_WATCH_FACE_SLEEP = "intent.action.keep.watchface.sleep";
  private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
    @Override public void onReceive(Context context, Intent intent) {
      //if (Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
      //}
    }
  };
  private DigitalClock digital;



  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_digilog);
    digital = (DigitalClock)findViewById(R.id.digital);
  }

  @Override protected void onResume() {
    super.onResume();
    showBackground();
    registerReceiver();
  }

  @Override protected void onPause() {
    unregisterReceiver();
    hideBackground();
    super.onPause();
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

  private void hideBackground() {
    digital.setVisibility(View.GONE);
  }

  private void showBackground() {
    digital.setVisibility(View.VISIBLE);
  }

}
*/