package com.dji.Drogon.domain;

import java.util.Date;

public class WritableDBMission {

  private long flightDuration = 0;
  private Date dateTime;
  private double homeLat, homeLng;
  private int altitude;

//  public WritableDBMission(Date dateTime, int flightDuration, int numPicsTaken, double homeLat, double homeLng) {
//    this.dateTime = dateTime;
//    this.flightDuration = flightDuration;
//    this.numPicsTaken = numPicsTaken;
//    this.homeLat = homeLat;
//    this.homeLng = homeLng;
//  }

  public WritableDBMission(Date dateTime, double homeLat, double homeLng, int altitude) {
    this.dateTime = dateTime;
    this.homeLat = homeLat;
    this.homeLng = homeLng;
    this.altitude = altitude;
  }

  public void setFlightDuration(long flightDuration) {
    this.flightDuration = flightDuration;
  }

  public Date getDateTime() {
    return dateTime;
  }

  public long getFlightDuration() {
    return flightDuration;
  }

  public double getHomeLat() {
    return homeLat;
  }

  public double getHomeLng() {
    return homeLng;
  }

  public int getAltitude() {
    return altitude;
  }
}
