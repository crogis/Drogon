package com.dji.Drogon.views;

import android.view.View;

public class CustomLayoutParams {
  final public int width, height, paddingLeft, paddingRight, paddingTop, paddingBottom;

  public CustomLayoutParams(View v) {
    height = v.getHeight();
    width = v.getWidth();
    paddingLeft = v.getPaddingLeft();
    paddingRight = v.getPaddingRight();
    paddingTop = v.getPaddingTop();
    paddingBottom = v.getPaddingBottom();
  }
}
