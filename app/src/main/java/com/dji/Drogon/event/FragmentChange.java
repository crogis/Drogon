package com.dji.Drogon.event;

public class FragmentChange {
  private boolean isMapFragmentMain;

  public FragmentChange(boolean isMapFragmentMain) {
    this.isMapFragmentMain = isMapFragmentMain;
  }

  public boolean getIsMapFragmentMain() {
    return isMapFragmentMain;
  }
}
