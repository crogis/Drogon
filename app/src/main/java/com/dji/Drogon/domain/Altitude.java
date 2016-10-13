package com.dji.Drogon.domain;

public class Altitude {

  float mAltitude;
  double mWidth, mHeight;

  private static float LEVEL_10_MALTITUDE = 10.0f;
  private static double LEVEL_10_MWIDTH = 13.33;
  private static double LEVEL_10_MHEIGHT = 10.00;

  private static float LEVEL_15_MALTITUDE = 15.0f;
  private static double LEVEL_15_MWIDTH = 18.00;
  private static double LEVEL_15_MHEIGHT = 13.50;

  private static float LEVEL_20_MALTITUDE = 20.0f;
  private static double LEVEL_20_MWIDTH = 26.00;
  private static double LEVEL_20_MHEIGHT = 19.50;

  public static Altitude LEVEL_10 = new Altitude(LEVEL_10_MALTITUDE, LEVEL_10_MWIDTH,LEVEL_10_MHEIGHT);
  public static Altitude LEVEL_15 = new Altitude(LEVEL_15_MALTITUDE, LEVEL_15_MWIDTH,LEVEL_15_MHEIGHT);
  public static Altitude LEVEL_20 = new Altitude(LEVEL_20_MALTITUDE, LEVEL_20_MWIDTH,LEVEL_20_MHEIGHT);

  private Altitude(float altitude, double width, double height) {
    mAltitude = altitude;
    mWidth = width;
    mHeight = height;
  }

  public float getAltitude() {
    return mAltitude;
  }

  public double getCaptureDistance() {
    return mHeight;
  }

  public double getWidthInKM() {
    return mToKm(mWidth);
  }

  public double getHeightInKM() {
    return mToKm(mHeight);
  }

  private double mToKm(double m) {
    return m / 1000;
  }
}
