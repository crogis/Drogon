package com.dji.Drogon.helper;

import android.text.TextUtils;

import com.dji.Drogon.domain.DBMission;

import java.text.SimpleDateFormat;

public class CSVWriter {

  private static final String COMMA_SEPARATOR = ",";
  private static SimpleDateFormat dateFormat = DateFormatter.getReadableDateFormat();

  //int missionId, Date dateTime, int flightDuration, int numPicsTaken, double homeLat, double homeLng
  public static String generateFromDBMission(DBMission mission) {
    String missionId = String.valueOf(mission.getMissionId());
    String dateTime = dateFormat.format(mission.getDateTime());
    String flightDuration = String.valueOf(mission.getFlightDuration());
    String numPicsTaken = String.valueOf(mission.getNumPicsTaken());
    String homeLat = String.valueOf(mission.getHomeLat());
    String homeLng = String.valueOf(mission.getHomeLng());

    String[] arr = new String[]{missionId, dateTime, flightDuration, numPicsTaken, homeLat, homeLng};

    return TextUtils.join(COMMA_SEPARATOR, arr);
  }
}
