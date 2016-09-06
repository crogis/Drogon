package com.dji.Drogon;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.dji.Drogon.anim.FillScreenAnimation;
import com.dji.Drogon.views.CustomLayoutParams;
import com.dji.Drogon.fragment.CameraFragment;
import com.dji.Drogon.fragment.MapFragment;

public class MainActivity extends AppCompatActivity {

  private FrameLayout mainLayout, subLayout, borderLayout;
  private RelativeLayout parentLayout, settingsLayout;
  private ImageButton settingsImageBtn;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
      WindowManager.LayoutParams.FLAG_FULLSCREEN);
    setContentView(R.layout.activity_main);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
      ActivityCompat.requestPermissions(this,getPermissions(), 1);

    initializeToolbar();
    initializeViews();

    Fragment cameraFragment = new CameraFragment();
    Fragment mapFragment = new MapFragment();
    addFragmentToMain(cameraFragment);
    addFragmentToSub(mapFragment);
  }

  private void initializeToolbar() {
    Toolbar toolbar = find(R.id.toolbar);
    // Sets the Toolbar to act as the ActionBar for this Activity window.
    // Make sure the toolbar exists in the activity and is not null
    setSupportActionBar(toolbar);
    ActionBar actionBar = getSupportActionBar();
    if(isNotNull(actionBar)) {
      actionBar.setDisplayShowTitleEnabled(false);
    }
  }

  private void initializeViews() {
    parentLayout = find(R.id.parent_fragment_layout);
    mainLayout = find(R.id.main_fragment);
    subLayout = find(R.id.sub_fragment);
    borderLayout = find(R.id.border_layout);
    settingsLayout = find(R.id.settings_layout);
    settingsImageBtn = find(R.id.settings_image_button);

    setListeners();
  }

  private void setListeners() {
    borderLayout.setOnClickListener((view) -> {
      onFragmentChange();
    });

    settingsImageBtn.setOnClickListener((view) -> {
      createSettingsDialog();
    });
  }

  public void onFragmentChange() {
    CustomLayoutParams cp = new CustomLayoutParams(subLayout);
    FillScreenAnimation fillScreenAnimation =
            new FillScreenAnimation(
                    subLayout,
                    mainLayout.getHeight(),
                    cp.height,
                    mainLayout.getWidth(),
                    cp.width);
    fillScreenAnimation.setAnimationListener(new Animation.AnimationListener() {
      @Override
      public void onAnimationStart(Animation animation) {}

      @Override
      public void onAnimationEnd(Animation animation) {
        switchViews(cp);
        borderLayout.setEnabled(true);
      }

      @Override
      public void onAnimationRepeat(Animation animation) {}
    });
    fillScreenAnimation.setDuration(500);
    subLayout.startAnimation(fillScreenAnimation);
    borderLayout.setEnabled(false);
  }

  private void switchViews(CustomLayoutParams cp) {
    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(cp.width, cp.height);
    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
    params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
    mainLayout.setLayoutParams(params);
    mainLayout.setPadding(cp.paddingLeft, cp.paddingTop, cp.paddingRight, cp.paddingBottom);
    mainLayout.requestLayout();
    parentLayout.bringChildToFront(mainLayout);
    borderLayout.requestLayout();

    FrameLayout temp = subLayout;
    subLayout = mainLayout;
    mainLayout = temp;
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
      d.dismiss();
    });
    d.show();
  }

  private void addFragmentToMain(Fragment main) {
    addFragment(R.id.main_fragment, main);
  }

  private void addFragmentToSub(Fragment sub) {
    addFragment(R.id.sub_fragment, sub);
  }

  private void addFragment(int id, Fragment f) {
    getSupportFragmentManager().beginTransaction().add(id, f).commit();
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
