package com.dji.Drogon.db.domain;

import android.provider.BaseColumns;

public final class MissionPicture {

  private MissionPicture() {}

  public static class MissionPictureEntry implements BaseColumns {

    public static final String TABLE_NAME = "mission_picture_entry";

    public static final String COLUMN_NAME_ID = "id";
    public static final String COLUMN_NAME_MISSION_ID = "mission_id";
    public static final String COLUMN_NAME_DATE_TIME = "date_time";
    public static final String COLUMN_NAME_LAT = "lat";
    public static final String COLUMN_NAME_LNG = "lng";
  }
}
