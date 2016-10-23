package com.dji.Drogon.helper;

import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class FileHelper {

  final static String MISSION_DIRECTORY_NAME = "waypoint_mission";

  public final static String MISSION_DIRECTORY_PATH = Environment.getExternalStorageDirectory() +File.separator + MISSION_DIRECTORY_NAME;


  static SimpleDateFormat dateFormatter = DateFormatter.getCompressedDateFormat();

  public static void initializeWaypointDirectory() {
    File directory = new File(MISSION_DIRECTORY_PATH);
    //directory.mkdir() creates waypoint_mission directory in your phone
    //returns true if the directory was created, false if the directory already exists
    if(directory.mkdir()) {
      System.out.println("Created directory!");
    } else System.out.println("Directory already exists");
  }

//  public static String getFileNameByDate(Date date) {
//    return createSubDirectoryOrFileByDate(date);
//  }

  public static String getFilePath(String parent, String fileName) {
    return parent + File.separator + fileName;
  }

  public static void createWaypointSubDirectory(String path) {
    File directory = new File(path);
    if(directory.mkdir()) {
      System.out.println("Created directory: " + path);
    } else System.out.println("Directory already exists " + path);
  }

  public static void writeToFile(File file, String content) {
    try {
      FileWriter fw = new FileWriter(file, false);
      fw.write(content);
      fw.close();
    } catch (IOException e) {
      System.out.println("Unable to write to file");
    }
  }
}
