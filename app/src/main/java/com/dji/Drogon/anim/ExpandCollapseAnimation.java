//Drop down alerts
package com.dji.Drogon.anim;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * Class for handling collapse and expand animations.
 * @author Esben Gaarsmand
 * http://stackoverflow.com/questions/9248930/android-animate-drop-down-up-view-proper
 */
public class ExpandCollapseAnimation extends Animation {
  private View parentView, textView;
  private int mEndHeight;
  private int mType;

  /**
   * Initializes expand collapse animation, has two types, collapse (1) and expand (0).
   * @param parentView The view to animate
   * @param duration
   * @param type The type of animation: 0 will expand from gone and 0 size to visible and layout size defined in xml.
   * 1 will collapse view and set to gone
   */
  public ExpandCollapseAnimation(View parentView, View textView, int duration, int type) {
    setDuration(duration);
    this.textView = textView;
    this.parentView = parentView;
    mEndHeight = parentView.getLayoutParams().height;
    mType = type;
    if(mType == 0) {
      parentView.getLayoutParams().height = 0;
      parentView.setVisibility(View.VISIBLE);
    }
  }

  @Override
  protected void applyTransformation(float interpolatedTime, Transformation t) {
    super.applyTransformation(interpolatedTime, t);
    if (interpolatedTime < 1.0f) {
      float alpha = interpolatedTime;
      int height = (int) (mEndHeight * interpolatedTime);
      if(mType == 1) {
        alpha = 1.0f - interpolatedTime;
        height = mEndHeight - (int) (mEndHeight * interpolatedTime);
      }
      setAlphaHeight(alpha, height);
    } else {
      float alpha = 1f;
      int height = mEndHeight;

      if(mType == 1) {
        alpha = 0f;
        height = 0;
        parentView.setVisibility(View.GONE);
      }

      setAlphaHeight(alpha, height);
      parentView.getLayoutParams().height = mEndHeight;
    }
  }

  private void setAlphaHeight(float alpha, int height) {
    setTextViewAlpha(alpha);
    setParentAlphaHeight(alpha, height);
  }

  private void setTextViewAlpha(float alpha) {
    textView.setAlpha(alpha);
    textView.requestLayout();
  }

  private void setParentAlphaHeight(float alpha, int height) {
    parentView.setAlpha(alpha);
    parentView.getLayoutParams().height = height;
    parentView.requestLayout();
  }
}
