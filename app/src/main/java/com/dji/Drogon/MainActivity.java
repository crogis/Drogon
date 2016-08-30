package com.dji.Drogon;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.dji.Drogon.fragment.CameraFragment;
import com.dji.Drogon.fragment.MapFragment;

public class MainActivity extends AppCompatActivity {

  private FrameLayout mainLayout, subLayout;
  private RelativeLayout settingsLayout;
  private ImageButton settingsImageBtn;

  private final String CAMERA_FRAGMENT_TAG = "fragment_camera";
  private final String MAP_FRAGMENT_TAG = "fragment_map";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);

    setContentView(R.layout.activity_main);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
      ActivityCompat.requestPermissions(this,getPermissions(), 1);

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    // Sets the Toolbar to act as the ActionBar for this Activity window.
    // Make sure the toolbar exists in the activity and is not null
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayShowTitleEnabled(false);

    mainLayout = find(R.id.main_fragment);
    subLayout = find(R.id.sub_fragment);
    settingsLayout = find(R.id.settings_layout);
    settingsImageBtn = find(R.id.settings_image_button);

    mainLayout.setOnClickListener((view) -> {
      System.out.println("CLICKING MAIN LAYOUT");
    });

    subLayout.setOnClickListener((view) -> {
      System.out.println("CLICKING SUB LAYOUT");
      onFragmentChange();
    });

    settingsImageBtn.setOnClickListener((view) -> {
      System.out.println("CLICK SETTINGS BUTTON");
      createSettingsDialog();
    });

    addFragmentToMain(newCameraFragment());
    addFragmentToSub(newMapFragment());
  }

  public void onFragmentChange() {
    FragmentManager fragmentManager = getSupportFragmentManager();

    Fragment f1;
    if (fragmentManager.findFragmentById(R.id.main_fragment) instanceof CameraFragment) {
      settingsLayout.setVisibility(View.INVISIBLE);
      f1 = newMapFragment();
      Bundle b = new Bundle();
      b.putBoolean("isMain", true);
      f1.setArguments(b);
    } else {
      settingsLayout.setVisibility(View.VISIBLE);
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
//    mfragmentTransaction.setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit);
    mfragmentTransaction.replace(R.id.main_fragment, f1);
    mfragmentTransaction.commit();

    FragmentTransaction mfragmentTransaction2 = fragmentManager.beginTransaction();
    mfragmentTransaction2.replace(R.id.sub_fragment, f2);
    mfragmentTransaction2.commit();
  }

  private void createSettingsDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    // Get the layout inflater
    LayoutInflater inflater = getLayoutInflater();
    View v = inflater.inflate(R.layout.dialog_settings, null);
    // Inflate and set the layout for the dialog
    // Pass null as the parent view because its going in the
    // dialog layout

    builder.setCancelable(false);
    builder.setView(v);
    AlertDialog d = builder.create();
    ImageButton btn = (ImageButton)v.findViewById(R.id.close_btn);
    btn.setOnClickListener((l) -> {
      System.out.println("CLOSE!!!!");
      d.dismiss();
    });
    d.show();
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
