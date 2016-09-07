package com.dji.Drogon;

import android.Manifest;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ConnectionActivity extends Activity implements View.OnClickListener {

  private TextView mTextConnectionStatus;
  private TextView mTextProduct;
  private Button mBtnOpen;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
      ActivityCompat.requestPermissions(this,getPermissions(), 1);

    setContentView(R.layout.activity_connection);
    initUI();
  }

  private void initUI() {
    mTextConnectionStatus = (TextView) findViewById(R.id.text_connection_status);
    mTextProduct = (TextView) findViewById(R.id.text_product_info);
    mBtnOpen = (Button) findViewById(R.id.btn_open);
    mBtnOpen.setOnClickListener(this);
    mBtnOpen.setEnabled(false);
  }
  @Override
  public void onClick(View v) {
    switch (v.getId()) {
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