package com.lkspencer.anital;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

public class UpdateTimeHandler extends Handler {
  static final int MSG_UPDATE_TIME = 0;
  static final int INTERACTIVE_UPDATE_RATE_MS = 50;
  WeakReference<Anital.Engine> eWeakReference;

  public UpdateTimeHandler(Anital.Engine e) {
    this.eWeakReference = new WeakReference<>(e);
  }

  @Override public void handleMessage(Message message) {
    switch (message.what) {
      case MSG_UPDATE_TIME:
        eWeakReference.get().invalidate();
        if (eWeakReference.get().shouldTimerBeRunning()) {
          this.sendEmptyMessageDelayed(MSG_UPDATE_TIME, INTERACTIVE_UPDATE_RATE_MS);
        }
        break;
    }
  }
}
