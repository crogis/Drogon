package com.dji.Drogon.helper;

import com.dji.Drogon.domain.WaypointMarkers;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DistanceComputation {

  //radius of the earth in km
  private static final double rEarth = 6378;//km

  //height and width are in km
  //height - x
  //width - y
  public static List<LatLng> getSquareCoordinates(LatLng center, double kmHeight, double kmWidth) {
    LatLng yPos = computeNewLatLng(center, kmHeight, 0); // |
    LatLng yNeg = computeNewLatLng(center, -kmHeight, 0); // |

    LatLng upperRight = computeNewLatLng(yPos, 0, kmWidth);
    LatLng upperLeft = computeNewLatLng(yPos, 0, -kmWidth);

    LatLng lowerRight = computeNewLatLng(yNeg, 0, kmWidth);
    LatLng lowerLeft = computeNewLatLng(yNeg, 0, -kmWidth);

    return Arrays.asList(upperRight, upperLeft, lowerLeft, lowerRight);
  }


  public static List<LatLng> getMarkersGivenDistance(WaypointMarkers markers, double captureDistance) {
    List<LatLng> marked = new ArrayList<>();
    System.out.println("CAPTURE DISTANCE" + captureDistance);

    double remainder = 0.0;

    for(int i = 1; i < markers.size(); i++) {
      LatLng from = markers.getPosition(i - 1);
      LatLng to = markers.getPosition(i);

      System.out.println(i + " FROM " + from.toString());
      System.out.println(i + " TO " + to.toString());
      double distancePoint0To1 = SphericalUtil.computeDistanceBetween(from, to);
      double heading = SphericalUtil.computeHeading(from, to);
      System.out.println("DISTANCE FROM POINT 0 TO 1 " + distancePoint0To1);

      //remainder should be less than captureDistance
      if(remainder > 0.0 && remainder < captureDistance) {
        if(distancePoint0To1 > remainder) {
          from = SphericalUtil.computeOffset(from, remainder, heading);
          marked.add(from);
          distancePoint0To1 = distancePoint0To1 - remainder;
          remainder = 0.0;
        } else {
          remainder = remainder - distancePoint0To1;
        }
        System.out.println("REMAINING REMAINDER " + remainder);
      }

      if(distancePoint0To1 < captureDistance && remainder == 0.0) {
        remainder = captureDistance - distancePoint0To1;
      } else if(distancePoint0To1 >= captureDistance && remainder == 0.0){
        remainder = distancePoint0To1 % captureDistance;
        int numPointsInBetween = Double.valueOf(distancePoint0To1 / captureDistance).intValue();

        System.out.println("NUM POINTS IN BETWEEN " + numPointsInBetween);
        System.out.println("REMAINDER " + remainder);

        for (int j = 0; j < numPointsInBetween; j++) {
          to = SphericalUtil.computeOffset(from, captureDistance, heading);
          marked.add(to);
          from = to;
        }
      }
    }
    return marked;
  }

  public static LatLng computeNewLatLng(LatLng center, double dy, double dx) {
    double newLatitude  = center.latitude  + (dy / rEarth) * (180 / Math.PI);
    double newLongitude = center.longitude + (dx / rEarth) * (180 / Math.PI) / Math.cos(center.latitude * Math.PI/180);
    return new LatLng(newLatitude, newLongitude);
  }
}
