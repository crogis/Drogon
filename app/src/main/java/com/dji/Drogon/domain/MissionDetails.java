package com.dji.Drogon.domain;

public class MissionDetails {

  private static MissionDetails missionDetailsInstance = new MissionDetails();

  private boolean inMission = false;

  private MissionDetails() {}

  public static MissionDetails getInstance() {
    return missionDetailsInstance;
  }

  public boolean isMissionInProgress() {
    return inMission;
  }

  private long startTime = 0;

  public void setMissionStart() {
    startTime = System.currentTimeMillis();
    inMission = true;
  }

  public long setMissionStop() {
    long endTime = System.currentTimeMillis();
    inMission = false;
    return (endTime - startTime) / 1000;
  }
}
