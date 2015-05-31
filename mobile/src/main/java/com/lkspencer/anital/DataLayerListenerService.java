package com.lkspencer.anital;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.nio.ByteBuffer;

public class DataLayerListenerService extends WearableListenerService {

  int batteryPercentage = 0;
  boolean mIsRegistered = false;
  String googleApiAction;
  GoogleApiClient mGoogleApiClient = null;

  /* receiver to update battery percentage */
  BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
    {} @Override public void onReceive(Context context, Intent batteryStatus) {
      if (batteryStatus == null || !Intent.ACTION_BATTERY_CHANGED.equalsIgnoreCase(batteryStatus.getAction())) return;

      int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
      int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

      batteryPercentage = (int)((level / (float)scale) * 100);
      mGoogleApiClient = new GoogleApiClient.Builder(context)
          .addApi(Wearable.API)
          .addConnectionCallbacks(connectionCallbacks)
          .build();
      googleApiAction = "Anital_PhonePercent";
      mGoogleApiClient.connect();
    }
  };



  /* callback for GoogleApiClient connections */
  GoogleApiClient.ConnectionCallbacks connectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
    @Override public void onConnected(Bundle bundle) {
      new Thread(new Runnable() {
        {} @Override public void run() {
          NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
          for(Node node : nodes.getNodes()) {
            byte[] data = ByteBuffer.allocate(4)
                .putInt(batteryPercentage)
                .array();
            /*MessageApi.SendMessageResult result = */Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), googleApiAction, data).await();
          }
          mGoogleApiClient.disconnect();
          if (mIsRegistered) {
            mIsRegistered = false;
            DataLayerListenerService.this.unregisterReceiver(mBatteryReceiver);
          }
        }
      }).start();
    }

    @Override public void onConnectionSuspended(int i) { }
  };



  /* WearableListenerService */
  @Override public void onMessageReceived(MessageEvent messageEvent) {
    if (messageEvent == null) return;

    String path = messageEvent.getPath();
    if ("Anital_PhonePercent".equalsIgnoreCase(path)) {
      IntentFilter intentWatchFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
      mIsRegistered = true;
      registerReceiver(mBatteryReceiver, intentWatchFilter);
    }
    super.onMessageReceived(messageEvent);
  }

}
