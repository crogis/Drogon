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

  public void setMissionStart() {
    inMission = true;
  }

  public void setMissionStop() {
    inMission = false;
  }
}
