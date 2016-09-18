package com.dji.Drogon.event;

public class FragmentChange {
  private boolean isCameraFragmentMain;

  public FragmentChange(boolean isCameraFragmentMain) {
    this.isCameraFragmentMain = isCameraFragmentMain;
  }

  public boolean getIsCameraFragmentMain() {
    return isCameraFragmentMain;
  }
}
