package com.dji.Drogon.domain;

import com.dji.Drogon.helper.DateFormatter;
import com.dji.Drogon.helper.FileHelper;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileDirectory {

  private SimpleDateFormat dateFormatter = DateFormatter.getCompressedDateFormat();

  private final String CSV_FILE_EXTENSION  = ".csv";

  private Date now;

  public FileDirectory(Date now) {
    this.now = now;
  }

  public String getSubDirectoryPath() {
    return FileHelper.MISSION_DIRECTORY_PATH + File.separator + getNameByDate();
  }

  //no extensions
  public String getBaseFileName() {
    return getNameByDate();
  }

  public String getCSVFilePath() {
    return getSubDirectoryPath() + File.separator +  getCSVFileName();
  }

  public String getCSVFileName() {
    return getBaseFileName() + CSV_FILE_EXTENSION;
  }

  private String getNameByDate() {
    return dateFormatter.format(now);
  }
}
