package com.dji.Drogon;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.Products.DJIAircraft;

public class ConnectionActivity extends Activity {

  private static final String TAG = ConnectionActivity.class.getName();

  @BindView(R.id.connection_status_text_view) TextView connectionStatusTextView;
  @BindView(R.id.product_info_text_view) TextView productInfoTextView;
  @BindView(R.id.open_button) Button openButton;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_connection);

    ButterKnife.bind(this);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
      ActivityCompat.requestPermissions(this,getPermissions(), 1);

    openButton.setEnabled(true);

    IntentFilter filter = new IntentFilter();
    filter.addAction(DrogonApplication.FLAG_CONNECTION_CHANGE);
    registerReceiver(mReceiver, filter);
  }

  protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      refreshSDKRelativeUI();
    }
  };

  private void refreshSDKRelativeUI() {
    DJIBaseProduct mProduct = DrogonApplication.getProductInstance();
    String textProduct = "", textConnectionStatus = "";

    System.out.println("IS PRODUCT NULL " + isNotNull(mProduct));
    boolean isProductConnected = isNotNull(mProduct) && mProduct.isConnected();
    if(isProductConnected) {
      String str = mProduct instanceof DJIAircraft ? "DJIAircraft" : "DJIHandheld";
      textConnectionStatus = "Status " + str + " connected";
      textProduct = isNotNull(mProduct.getModel()) ? mProduct.getModel().getDisplayName() : getString(R.string.product_information);
    } else {
      textProduct = getString(R.string.product_information);
      textConnectionStatus = getString(R.string.connection_loose);
    }
    Log.v(TAG, "refreshSDK: " + isProductConnected);
    openButton.setEnabled(isProductConnected);
    productInfoTextView.setText(textProduct);
    connectionStatusTextView.setText(textConnectionStatus);
  }


  @OnClick(R.id.open_button) void onButtonOpen(View view) {
    Intent intent = new Intent(this, MainActivity.class);
    startActivity(intent);
  }

  public <T> boolean isNotNull(T i) {
    return i != null;
  }

  public <T> boolean isNull(T i) {
    return i == null;
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