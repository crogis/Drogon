package com.dji.Drogon;

import android.Manifest;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ConnectionActivity extends Activity {

  @BindView(R.id.text_connection_status) TextView mTextConnectionStatus;
  @BindView(R.id.text_product_info) TextView mTextProduct;
  @BindView(R.id.btn_open) Button mBtnOpen;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_connection);

    ButterKnife.bind(this);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
      ActivityCompat.requestPermissions(this,getPermissions(), 1);

    mBtnOpen.setEnabled(false);
  }

  @OnClick(R.id.btn_open) void onButtonOpen(View view) {
    switch (view.getId()) {
      case R.id.btn_open: {
        break;
      }
      default:
        break;
    }
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