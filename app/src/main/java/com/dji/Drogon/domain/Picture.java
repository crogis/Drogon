package com.dji.Drogon.domain;

import java.util.Date;

public class Picture {

  private Date now;
  private double lat, lng;

  public Picture(Date now, double lat, double lng) {
    this.now = now;
    this.lat = lat;
    this.lng = lng;
  }

  public Date getDateTime() {
    return now;
  }

  public double getLat() {
    return lat;
  }

  public double getLng() {
    return lng;
  }

}
