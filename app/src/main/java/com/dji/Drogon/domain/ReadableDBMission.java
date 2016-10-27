package com.dji.Drogon.domain;

import java.util.Date;
import java.util.List;

public class ReadableDBMission {

  private int missionId, flightDuration, altitude;
  private Date dateTime;
  private double homeLat, homeLng;
  private List<Picture> pictures;

  public ReadableDBMission(int missionId, Date dateTime, int flightDuration, int altitude, double homeLat, double homeLng, List<Picture> pictures) {
    this.missionId = missionId;
    this.dateTime = dateTime;
    this.flightDuration = flightDuration;
    this.altitude = altitude;
    this.homeLat = homeLat;
    this.homeLng = homeLng;
    this.pictures = pictures;
  }

  public int getMissionId() {
    return missionId;
  }

  public Date getDateTime() {
    return dateTime;
  }

  public int getFlightDuration() {
    return flightDuration;
  }

  public int getAltitude() {
    return altitude;
  }

  public double getHomeLat() {
    return homeLat;
  }

  public double getHomeLng() {
    return homeLng;
  }

  public List<Picture> getPictures() {
    return pictures;
  }
}
