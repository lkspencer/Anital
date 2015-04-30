package com.lkspencer.anital;

import android.os.Handler;
import android.os.Message;

public class UpdateTimeHandler extends Handler {
  static final int MSG_UPDATE_TIME = 0;
  static final int INTERACTIVE_UPDATE_RATE_MS = 50;

  public UpdateTimeHandler(Anital a) {
    this.a = a;
  }

  private Anital a;

  @Override public void handleMessage(Message message) {
    switch (message.what) {
      case MSG_UPDATE_TIME:
        Anital.Engine e = a.getEngine();
        if (e != null) {
          e.invalidate();
          if (e.shouldTimerBeRunning()) {
            this.sendEmptyMessageDelayed(MSG_UPDATE_TIME, INTERACTIVE_UPDATE_RATE_MS);
          }
        }
        break;
    }
  }
}
