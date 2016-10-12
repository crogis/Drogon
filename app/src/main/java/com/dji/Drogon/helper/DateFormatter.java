package com.dji.Drogon.helper;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class DateFormatter {
  public static SimpleDateFormat getReadableDateFormat() {
    return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
  }

  public static SimpleDateFormat getCompressedDateFormat() {
    return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
  }
}