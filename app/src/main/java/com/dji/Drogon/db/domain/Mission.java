package com.dji.Drogon.db.domain;

import android.provider.BaseColumns;

public final class Mission {
  private Mission() {}

  /*
  CREATE TABLE `mission table 1` (
  `Mission ID` int(11) NOT NULL,
  `Date` date NOT NULL,
  `Time` time NOT NULL,
  `Flight Duration` time NOT NULL,
  `Number of Pictures Taken` int(11) NOT NULL,
  `Home Point` text NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
   */

  public static class MissionEntry implements BaseColumns {
    public static final String TABLE_NAME = "mission_entry";

    public static final String COLUMN_NAME_MISSION_ID = "mission_id";
    public static final String COLUMN_NAME_DATE_TIME = "date_time";
//    public static final String COLUMN_NAME_TIME = "time";
    public static final String COLUMN_NAME_FLIGHT_DURATION = "flight_duration";
    public static final String COLUMN_NAME_NUM_PICS_TAKEN = "num_pics_taken";
    public static final String COLUMN_NAME_HOME_LAT = "home_lat";
    public static final String COLUMN_NAME_HOME_LNG = "home_lng";
  }
}