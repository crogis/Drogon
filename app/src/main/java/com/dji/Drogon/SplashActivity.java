package com.dji.Drogon;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final Context _c = this;
    new Handler().postDelayed(new Runnable() {
      @Override
      public void run() {
        Intent intent = new Intent(_c, MainActivity.class);
        startActivity(intent);
        finish();
      }
    }, 2000);
  }
}