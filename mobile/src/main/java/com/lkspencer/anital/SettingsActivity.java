package com.lkspencer.anital;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.util.Log;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

  /**
   * Determines whether to always show the simplified settings UI, where
   * settings are presented in a single list. When false, settings are shown
   * as a master/detail two-pane view on tablets. When true, a single pane is
   * shown on tablets.
   */
  private static final boolean ALWAYS_SIMPLE_PREFS = false;
  private static final String TAG = "com.lkspencer.anital";
  private GoogleApiClient mGoogleApiClient = null;



  @Override protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    mGoogleApiClient = new GoogleApiClient.Builder(this)
            .addApi(Wearable.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build();
    mGoogleApiClient.connect();

    setupSimplePreferencesScreen();
  }

  /** {@inheritDoc} */
  @Override public boolean onIsMultiPane() {
    return isXLargeTablet(this) && !isSimplePreferences(this);
  }

  @Override public void onConnected(Bundle bundle) {
    Log.d(TAG, "onConnected");
  }

  @Override public void onConnectionSuspended(int i) {
    Log.d(TAG, "onConnectionSuspended");
  }

  @Override public void onConnectionFailed(ConnectionResult connectionResult) {
    Log.e(TAG, "Failed to connect to Google API Client");
  }

  @Override protected void onResume() {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    preferences.registerOnSharedPreferenceChangeListener(this);
    super.onResume();
  }
  @Override protected void onPause() {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    preferences.unregisterOnSharedPreferenceChangeListener(this);
    mGoogleApiClient.disconnect();
    super.onPause();
  }

  @Override public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
      new Thread(new Runnable() {
        {}
        @Override public void run() {
          NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
          for (Node node : nodes.getNodes()) {
            SendSettingsToWear(node);
          }
        }
      }).start();
    }
  }



  /**
   * Shows the simplified settings UI if the device configuration if the
   * device configuration dictates that a simplified, single-pane UI should be
   * shown.
   */
  private void setupSimplePreferencesScreen() {
    if (!isSimplePreferences(this)) {
      return;
    }

    // In the simplified UI, fragments are not used at all and we instead
    // use the older PreferenceActivity APIs.

    PreferenceCategory fakeHeader;
    // Add 'general' preferences.
    addPreferencesFromResource(R.xml.pref_general);

    /*
    // Add 'notifications' preferences, and a corresponding header.
    fakeHeader = new PreferenceCategory(this);
    fakeHeader.setTitle(R.string.pref_header_notifications);
    getPreferenceScreen().addPreference(fakeHeader);
    addPreferencesFromResource(R.xml.pref_notification);

    // Add 'data and sync' preferences, and a corresponding header.
    fakeHeader = new PreferenceCategory(this);
    fakeHeader.setTitle(R.string.pref_header_data_sync);
    getPreferenceScreen().addPreference(fakeHeader);
    addPreferencesFromResource(R.xml.pref_data_sync);
    //*/

    // Add anital preferences, and a corresponding header.
    fakeHeader = new PreferenceCategory(this);
    fakeHeader.setTitle(R.string.pref_header_anital);
    getPreferenceScreen().addPreference(fakeHeader);
    addPreferencesFromResource(R.xml.pref_anital);

    // Bind the summaries of EditText/List/Dialog/Ringtone preferences to
    // their values. When their values change, their summaries are updated
    // to reflect the new value, per the Android Design guidelines.
    /*
    bindPreferenceSummaryToValue(findPreference("example_text"));
    bindPreferenceSummaryToValue(findPreference("example_list"));
    bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
    bindPreferenceSummaryToValue(findPreference("sync_frequency"));
    //*/
    bindPreferenceSummaryToValue(findPreference("animate_frequency"));
  }

  /**
   * Helper method to determine if the device has an extra-large screen. For
   * example, 10" tablets are extra-large.
   */
  private static boolean isXLargeTablet(Context context) {
    return (context.getResources().getConfiguration().screenLayout
            & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
  }

  /**
   * Determines whether the simplified settings UI should be shown. This is
   * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
   * doesn't have newer APIs like {@link PreferenceFragment}, or the device
   * doesn't have an extra-large screen. In these cases, a single-pane
   * "simplified" settings UI should be shown.
   */
  private static boolean isSimplePreferences(Context context) {
    return ALWAYS_SIMPLE_PREFS
            || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
            || !isXLargeTablet(context);
  }

  /** {@inheritDoc} */
  @Override
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public void onBuildHeaders(List<Header> target) {
    if (!isSimplePreferences(this)) {
      loadHeadersFromResource(R.xml.pref_headers, target);
    }
  }

  /**
   * A preference value change listener that updates the preference's summary
   * to reflect its new value.
   */
  private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
    {}
    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
      String stringValue = value.toString();

      if (preference instanceof ListPreference) {
        // For list preferences, look up the correct display value in
        // the preference's 'entries' list.
        ListPreference listPreference = (ListPreference) preference;
        int index = listPreference.findIndexOfValue(stringValue);

        // Set the summary to reflect the new value.
        preference.setSummary(
                index >= 0
                        ? listPreference.getEntries()[index]
                        : null);

      } else if (preference instanceof RingtonePreference) {
        // For ringtone preferences, look up the correct display value
        // using RingtoneManager.
        if (TextUtils.isEmpty(stringValue)) {
          // Empty values correspond to 'silent' (no ringtone).
          preference.setSummary(R.string.pref_ringtone_silent);

        } else {
          Ringtone ringtone = RingtoneManager.getRingtone(
                  preference.getContext(), Uri.parse(stringValue));

          if (ringtone == null) {
            // Clear the summary if there was a lookup error.
            preference.setSummary(null);
          } else {
            // Set the summary to reflect the new ringtone display
            // name.
            String name = ringtone.getTitle(preference.getContext());
            preference.setSummary(name);
          }
        }

      } else {
        // For all other preferences, set the summary to the value's
        // simple string representation.
        preference.setSummary(stringValue);
      }
      return true;
    }
  };

  /**
   * Binds a preference's summary to its value. More specifically, when the
   * preference's value is changed, its summary (line of text below the
   * preference title) is updated to reflect the value. The summary is also
   * immediately updated upon calling this method. The exact display format is
   * dependent on the type of preference.
   *
   * @see #sBindPreferenceSummaryToValueListener
   */
  private static void bindPreferenceSummaryToValue(Preference preference) {
    // Set the listener to watch for value changes.
    preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

    // Trigger the listener immediately with the preference's
    // current value.
    sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
            PreferenceManager
                    .getDefaultSharedPreferences(preference.getContext())
                    .getString(preference.getKey(), ""));
  }

  private void SendSettingsToWear(Node node) {
    byte[] data;
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this);
    String Anital_SensorSpeed = preferences.getString("animate_frequency", String.valueOf(SensorManager.SENSOR_DELAY_UI));
    data = ByteBuffer.allocate(4)
            .putInt(Integer.parseInt(Anital_SensorSpeed))
            .array();
    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), "Anital_SensorSpeed", data).await();
    LogSendMessageResult(node, result);

    boolean show_digital = preferences.getBoolean("show_digital", true);
    data = ByteBuffer.allocate(4)
            .putInt(show_digital ? 1 : 0)
            .array();
    result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), "show_digital", data).await();
    LogSendMessageResult(node, result);

    boolean show_day_of_week = preferences.getBoolean("show_day_of_week", true);
    data = ByteBuffer.allocate(4)
            .putInt(show_day_of_week ? 1 : 0)
            .array();
    result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), "show_day_of_week", data).await();
    LogSendMessageResult(node, result);

    boolean show_date = preferences.getBoolean("show_date", true);
    data = ByteBuffer.allocate(4)
            .putInt(show_date ? 1 : 0)
            .array();
    result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), "show_date", data).await();
    LogSendMessageResult(node, result);

    boolean show_battery = preferences.getBoolean("show_battery", true);
    data = ByteBuffer.allocate(4)
            .putInt(show_battery ? 1 : 0)
            .array();
    result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), "show_battery", data).await();
    LogSendMessageResult(node, result);
  }

  private void LogSendMessageResult(Node node, MessageApi.SendMessageResult result) {
    if (!result.getStatus().isSuccess()) {
      Log.e(TAG, "error");
    } else {
      Log.i(TAG, "success!! sent to: " + node.getDisplayName());
    }
  }


  /**
   * This fragment shows general preferences only. It is used when the
   * activity is showing a two-pane settings UI.
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public static class GeneralPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.pref_general);

      // Bind the summaries of EditText/List/Dialog/Ringtone preferences
      // to their values. When their values change, their summaries are
      // updated to reflect the new value, per the Android Design
      // guidelines.
      bindPreferenceSummaryToValue(findPreference("example_text"));
      bindPreferenceSummaryToValue(findPreference("example_list"));
    }
  }

  /**
   * This fragment shows notification preferences only. It is used when the
   * activity is showing a two-pane settings UI.
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public static class NotificationPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.pref_notification);

      // Bind the summaries of EditText/List/Dialog/Ringtone preferences
      // to their values. When their values change, their summaries are
      // updated to reflect the new value, per the Android Design
      // guidelines.
      bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
    }
  }

  /**
   * This fragment shows data and sync preferences only. It is used when the
   * activity is showing a two-pane settings UI.
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public static class DataSyncPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.pref_data_sync);

      // Bind the summaries of EditText/List/Dialog/Ringtone preferences
      // to their values. When their values change, their summaries are
      // updated to reflect the new value, per the Android Design
      // guidelines.
      bindPreferenceSummaryToValue(findPreference("sync_frequency"));
    }
  }

}
