package com.dji.Drogon.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.dji.Drogon.db.domain.Mission.*;
import com.dji.Drogon.db.domain.MissionPicture.*;
import com.dji.Drogon.domain.Picture;
import com.dji.Drogon.domain.ReadableDBMission;
import com.dji.Drogon.domain.WritableDBMission;
import com.dji.Drogon.helper.DateFormatter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DrogonDatabase extends SQLiteOpenHelper {

  private static final String DATABASE_NAME = "Drogon";
  private static final int DATABASE_VERSION = 1;

  private static final String DELIMITER = ",";

  private final String CREATE_MISSION_TABLE = "CREATE TABLE IF NOT EXISTS " +
    MissionEntry.TABLE_NAME + " ( " +
      createColumn(MissionEntry.COLUMN_NAME_ID, "INTEGER PRIMARY KEY", DELIMITER) +
      createColumn(MissionEntry.COLUMN_NAME_DATE_TIME, "datetime DEFAULT CURRENT_TIMESTAMP", DELIMITER) +
      createColumn(MissionEntry.COLUMN_NAME_FLIGHT_DURATION, "int(11) NOT NULL", DELIMITER) +
      createColumn(MissionEntry.COLUMN_NAME_ALTITUDE, "int(2) NOT NULL", DELIMITER) +
      createColumn(MissionEntry.COLUMN_NAME_HOME_LAT, "decimal(64,16) NOT NULL", DELIMITER) +
      createColumn(MissionEntry.COLUMN_NAME_HOME_LNG, "decimal(64,16) NOT NULL", "") + " )";

  private final String CREATE_PICTURES_TABLE = "CREATE TABLE IF NOT EXISTS " +
    MissionPictureEntry.TABLE_NAME + " ( " +
      createColumn(MissionPictureEntry.COLUMN_NAME_ID, "INTEGER PRIMARY KEY", DELIMITER) +
      createColumn(MissionPictureEntry.COLUMN_NAME_MISSION_ID, "int(11)", DELIMITER) +
      createColumn(MissionPictureEntry.COLUMN_NAME_DATE_TIME, "datetime", DELIMITER) +
      createColumn(MissionPictureEntry.COLUMN_NAME_LAT, "decimal(64,16) NOT NULL", DELIMITER) +
      createColumn(MissionPictureEntry.COLUMN_NAME_LNG, "decimal(64,16) NOT NULL", "") + ")";

  SimpleDateFormat dateFormat = DateFormatter.getReadableDateFormat();

  public DrogonDatabase(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(CREATE_MISSION_TABLE);
    db.execSQL(CREATE_PICTURES_TABLE);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

  }

  public int insertMission(WritableDBMission mission) {
    SQLiteDatabase db = getWritableDatabase();
    ContentValues values = new ContentValues();
    values.put(MissionEntry.COLUMN_NAME_DATE_TIME, dateFormat.format(mission.getDateTime()));
    values.put(MissionEntry.COLUMN_NAME_FLIGHT_DURATION, mission.getFlightDuration());
    values.put(MissionEntry.COLUMN_NAME_ALTITUDE, mission.getAltitude());
    values.put(MissionEntry.COLUMN_NAME_HOME_LAT, mission.getHomeLat());
    values.put(MissionEntry.COLUMN_NAME_HOME_LNG, mission.getHomeLng());
    return (int) db.insert(MissionEntry.TABLE_NAME, null, values);
  }

  public void insertPictureEntry(Picture p, int missionId) {
    SQLiteDatabase db = getWritableDatabase();
    ContentValues values = new ContentValues();
    values.put(MissionPictureEntry.COLUMN_NAME_MISSION_ID, missionId);
    values.put(MissionPictureEntry.COLUMN_NAME_DATE_TIME, dateFormat.format(p.getDateTime()));
    values.put(MissionPictureEntry.COLUMN_NAME_LAT, p.getLat());
    values.put(MissionPictureEntry.COLUMN_NAME_LNG, p.getLng());
    db.insert(MissionPictureEntry.TABLE_NAME, null, values);
  }

  public List<ReadableDBMission> getMissions() {
    List<ReadableDBMission> missions = new ArrayList<>();
    SQLiteDatabase db = getReadableDatabase();
    Cursor cursor = db.query(MissionEntry.TABLE_NAME, null, null, null, null, null, null, null);
    cursor.moveToFirst();
    missions = readDBMission(cursor, missions);

    cursor.close();

    return missions;
  }

  private List<ReadableDBMission> readDBMission(Cursor cursor, List<ReadableDBMission> missions) {
    if(!cursor.isAfterLast()) {
      ValueGetter v = new ValueGetter(cursor);
      Date date;
      try {
        date = dateFormat.parse(v.getString(MissionEntry.COLUMN_NAME_DATE_TIME));
      } catch(ParseException e) {
        //should never happen
        date = new Date();
      }

      List<Picture> pictures = new ArrayList<>();
      SQLiteDatabase db = getReadableDatabase();
      int missionId = v.getInt(MissionEntry.COLUMN_NAME_ID);
      String selection = MissionPictureEntry.COLUMN_NAME_MISSION_ID + "= '" + missionId + "'";
      Cursor mpeCursor = db.query(MissionPictureEntry.TABLE_NAME, null, selection, null, null, null, null, null);
      mpeCursor.moveToFirst();
      pictures = readPicture(mpeCursor, pictures);
      mpeCursor.close();

      ReadableDBMission mission = new ReadableDBMission(
        missionId,
        date,
        v.getInt(MissionEntry.COLUMN_NAME_FLIGHT_DURATION),
        v.getInt(MissionEntry.COLUMN_NAME_ALTITUDE),
        v.getDouble(MissionEntry.COLUMN_NAME_HOME_LAT),
        v.getDouble(MissionEntry.COLUMN_NAME_HOME_LNG),
        pictures
      );
      missions.add(mission);
      cursor.moveToNext();
      return readDBMission(cursor, missions);
    } else return missions;
  }

  private List<Picture> readPicture(Cursor cursor, List<Picture> pictures) {
    if(!cursor.isAfterLast()) {
      ValueGetter v = new ValueGetter(cursor);
      Date date;
      try {
        date = dateFormat.parse(v.getString(MissionPictureEntry.COLUMN_NAME_DATE_TIME));
      } catch(ParseException e) {
        date = new Date();
      }

      Picture p = new Picture(date, v.getDouble(MissionPictureEntry.COLUMN_NAME_LAT), v.getDouble(MissionPictureEntry.COLUMN_NAME_LNG));
      pictures.add(p);
      cursor.moveToNext();
      return readPicture(cursor, pictures);
    } else return pictures;
  }

  private String createColumn(String columnName, String type, String delimiter) {
    return columnName + " " + type + delimiter;
  }

  private class ValueGetter {
    private Cursor cursor;
    public ValueGetter(Cursor cursor) {
      this.cursor = cursor;
    }

    public int getInt(String name) {
      return cursor.getInt(cursor.getColumnIndex(name));
    }

    public String getString(String name) {
      return cursor.getString(cursor.getColumnIndex(name));
    }

    public double getDouble(String name) {
      return cursor.getDouble(cursor.getColumnIndex(name));
    }
  }
}
