package com.dji.Drogon.domain;

import java.util.Date;

public class DBMission {

  private int missionId, flightDuration, numPicsTaken;
  private Date dateTime;
  private double homeLat, homeLng;

  public DBMission(int missionId, Date dateTime, int flightDuration, int numPicsTaken, double homeLat, double homeLng) {
    this.missionId = missionId;
    this.dateTime = dateTime;
    this.flightDuration = flightDuration;
    this.numPicsTaken = numPicsTaken;
    this.homeLat = homeLat;
    this.homeLng = homeLng;
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

  public int getNumPicsTaken() {
    return numPicsTaken;
  }

  public double getHomeLat() {
    return homeLat;
  }

  public double getHomeLng() {
    return homeLng;
  }
}
