package com.dji.Drogon.anim;

//nagpapalit ng MapFragment and CameraFragment

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.RelativeLayout;

public class FillScreenAnimation extends Animation {
  final int targetHeight, targetWidth;
  View view;
  int startHeight, startWidth;

  public FillScreenAnimation(View view, int targetHeight, int startHeight, int targetWidth, int startWidth) {
    this.view = view;
    this.targetHeight = targetHeight;
    this.startHeight = startHeight;

    this.targetWidth = targetWidth;
    this.startWidth = startWidth;
  }

  @Override
  protected void applyTransformation(float interpolatedTime, Transformation t) {
    int newHeight = (int) (startHeight+(targetHeight - startHeight) * interpolatedTime);
    int newWidth = (int) (startWidth+(targetWidth - startWidth) * interpolatedTime);
    view.getLayoutParams().height = newHeight;
    view.getLayoutParams().width = newWidth;
    view.setPadding(0,0,0,0);
    view.requestLayout();
  }

  @Override
  public void initialize(int width, int height, int parentWidth, int parentHeight) {
    super.initialize(width, height, parentWidth, parentHeight);
  }

  @Override
  public boolean willChangeBounds() {
    return true;
  }
}