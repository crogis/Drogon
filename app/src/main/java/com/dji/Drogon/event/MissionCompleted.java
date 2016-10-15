package com.dji.Drogon.event;

public class MissionCompleted {

  private int rowId;

  public MissionCompleted(int rowId) {
    this.rowId = rowId;
  }

  public int getRowId() {
    return rowId;
  }
}
