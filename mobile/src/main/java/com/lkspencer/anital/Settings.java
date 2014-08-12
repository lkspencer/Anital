package com.lkspencer.anital;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;


public class Settings extends ActionBarActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {

  private static final String TAG = "com.lkspencer.anital";
  private NavigationDrawerFragment mNavigationDrawerFragment;
  private CharSequence mTitle;
  private GoogleApiClient mGoogleApiClient = null;



  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_settings);

    mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
    mTitle = getTitle();

    // Set up the drawer.
    mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));

    mGoogleApiClient = new GoogleApiClient.Builder(this)
            .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
              @Override public void onConnected(Bundle connectionHint) {
                Log.d(TAG, "onConnected: " + connectionHint);
                // Now you can use the data layer API
              }

              @Override public void onConnectionSuspended(int cause) {
                Log.d(TAG, "onConnectionSuspended: " + cause);
              }
            })
            .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
              {}
              @Override public void onConnectionFailed(ConnectionResult result) {
                Log.d(TAG, "onConnectionFailed: " + result);
              }
            })
            .addApi(Wearable.API)
            .build();

    List<Node> nodes = getNodes();
    for (Node node : nodes) {
      MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), "this is a test", null).await();
      if (!result.getStatus().isSuccess()) {
        Log.e(TAG, "ERROR: failed to send Message: " + result.getStatus());
      }
    }
  }

  @Override public void onNavigationDrawerItemSelected(int position) {
    // update the main content by replacing fragments
    FragmentManager fragmentManager = getSupportFragmentManager();
    fragmentManager.beginTransaction()
            .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
            .commit();
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    if (!mNavigationDrawerFragment.isDrawerOpen()) {
      // Only show items in the action bar relevant to this screen
      // if the drawer is not showing. Otherwise, let the drawer
      // decide what to show in the action bar.
      getMenuInflater().inflate(R.menu.settings, menu);
      restoreActionBar();
      return true;
    }
    return super.onCreateOptionsMenu(menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    return id == R.id.action_settings || super.onOptionsItemSelected(item);
  }



  public void onSectionAttached(int number) {
    switch (number) {
      case 1:
        mTitle = getString(R.string.title_section1);
        break;
    }
  }

  public void restoreActionBar() {
    ActionBar actionBar = getSupportActionBar();
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    actionBar.setDisplayShowTitleEnabled(true);
    actionBar.setTitle(mTitle);
  }

  private List<Node> getNodes() {
    List<Node> nodes = new ArrayList<Node>();
    NodeApi.GetConnectedNodesResult rawNodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
    for (Node node : rawNodes.getNodes()) {
      nodes.add(node);
    }
    return nodes;
  }



  public static class PlaceholderFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";



    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override public void onAttach(Activity activity) {
      super.onAttach(activity);
      ((Settings) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
    }



    public static PlaceholderFragment newInstance(int sectionNumber) {
      PlaceholderFragment fragment = new PlaceholderFragment();
      Bundle args = new Bundle();
      args.putInt(ARG_SECTION_NUMBER, sectionNumber);
      fragment.setArguments(args);
      return fragment;
    }

    public PlaceholderFragment() {
    }

  }

}
