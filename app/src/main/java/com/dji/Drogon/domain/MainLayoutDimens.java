package com.dji.Drogon.domain;

public class MainLayoutDimens {

  private static MainLayoutDimens dimensInstance = new MainLayoutDimens();

  private MainLayoutDimens() {}

  private static int width = 0;
  private static int height = 0;

  public static MainLayoutDimens getInstance() {
    return dimensInstance;
  }

  public static void setDimens(int w, int h) {
    width = w;
    height = h;
  }

  public static int getWidth() {
    return width;
  }

  public static int getHeight() {
    return height;
  }
}
