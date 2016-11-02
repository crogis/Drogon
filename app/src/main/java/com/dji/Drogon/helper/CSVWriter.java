package com.dji.Drogon.helper;

import android.text.TextUtils;

import com.dji.Drogon.domain.Picture;
import com.dji.Drogon.domain.ReadableDBMission;

import java.text.SimpleDateFormat;
import java.util.List;

public class CSVWriter {

  private static final String COMMA_SEPARATOR = ",";
  private static SimpleDateFormat dateFormat = DateFormatter.getReadableDateFormat();

  //int missionId, Date dateTime, int flightDuration, int numPicsTaken, double homeLat, double homeLng
  public static String generateFromDBMission(ReadableDBMission mission) {
    String missionId = String.valueOf(mission.getMissionId());
    String dateTime = dateFormat.format(mission.getDateTime());
    String flightDuration = String.valueOf(mission.getFlightDuration());
    String altitude = String.valueOf(mission.getAltitude());
    String homeLat = String.valueOf(mission.getHomeLat());
    String homeLng = String.valueOf(mission.getHomeLng());

    List<Picture> pictures = mission.getPictures();

    String s = "";

    for(Picture p: pictures) {
      String id = String.valueOf(pictures.indexOf(p));
      String now = dateFormat.format(p.getDateTime());
      String lat = String.valueOf(p.getLat());
      String lng = String.valueOf(p.getLng());
      String[] arr = new String[]{id, now, lat, lng, altitude, flightDuration};
      s = s + TextUtils.join(COMMA_SEPARATOR, arr) + "\n";
    }
//    String[] arr = new String[]{missionId, dateTime, flightDuration, numPicsTaken, homeLat, homeLng};

//    return TextUtils.join(COMMA_SEPARATOR, arr);
    return s;
  }
}
