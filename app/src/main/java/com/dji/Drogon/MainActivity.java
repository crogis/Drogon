package com.dji.Drogon;

import android.Manifest;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.FrameLayout;

import com.dji.Drogon.fragment.CameraFragment;
import com.dji.Drogon.fragment.MapFragment;

public class MainActivity extends FragmentActivity {

  private FrameLayout mainLayout, subLayout;

  private final String CAMERA_FRAGMENT_TAG = "fragment_camera";
  private final String MAP_FRAGMENT_TAG = "fragment_map";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
      ActivityCompat.requestPermissions(this,getPermissions(), 1);

    mainLayout = find(R.id.main_fragment);
    subLayout = find(R.id.sub_fragment);

    mainLayout.setOnClickListener((view) -> {
      System.out.println("CLICKING MAIN LAYOUT");
    });

    subLayout.setOnClickListener((view) -> {
      System.out.println("CLICKING SUB LAYOUT");
      onFragmentChange();
    });

    addFragmentToMain(newCameraFragment());
    addFragmentToSub(newMapFragment());
  }

  public void onFragmentChange() {
    FragmentManager fragmentManager = getSupportFragmentManager();

    Fragment f1;
    if (fragmentManager.findFragmentById(R.id.main_fragment) instanceof CameraFragment) {
      f1 = newMapFragment();
      Bundle b = new Bundle();
      b.putBoolean("isMain", true);
      f1.setArguments(b);
    } else {
      f1 = newCameraFragment();
    }

    Fragment f2;
    if (fragmentManager.findFragmentById(R.id.sub_fragment) instanceof MapFragment) {
      f2 = newCameraFragment();
    } else {
      f2 = newMapFragment();
      Bundle b = new Bundle();
      b.putBoolean("isMain", false);
      f2.setArguments(b);
    }

    FragmentTransaction mfragmentTransaction = fragmentManager.beginTransaction();
    mfragmentTransaction.setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit);
    mfragmentTransaction.replace(R.id.main_fragment, f1);
    mfragmentTransaction.commit();

    FragmentTransaction mfragmentTransaction2 = fragmentManager.beginTransaction();
    mfragmentTransaction2.replace(R.id.sub_fragment, f2);
    mfragmentTransaction2.commit();
  }

  private CameraFragment newCameraFragment() {
    return new CameraFragment();
  }

  private MapFragment newMapFragment() {
    return new MapFragment();
  }

  private void addFragmentToMain(Fragment main) {
    addFragment(R.id.main_fragment, main);
  }

  private void addFragmentToSub(Fragment sub) {
    addFragment(R.id.sub_fragment, sub);
  }

  private void addFragment(int id, Fragment f) {
    //so wrong
    String tag = "";
    if(f instanceof MapFragment) tag = MAP_FRAGMENT_TAG;
    else tag = CAMERA_FRAGMENT_TAG;
    getSupportFragmentManager().beginTransaction().add(id, f, tag).commit();
  }

  public <T> boolean isNotNull(T i) {
    return i != null;
  }

  public <T> boolean isNull(T i) {
    return i == null;
  }

  @SuppressWarnings("unchecked")
  public <T extends View> T find(int id) {
    return (T)findViewById(id);
  }

  private String[] getPermissions() {
    return new String[]{
      Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
      Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
      Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
      Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
      Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
      Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
      Manifest.permission.READ_PHONE_STATE
    };
  }
}
