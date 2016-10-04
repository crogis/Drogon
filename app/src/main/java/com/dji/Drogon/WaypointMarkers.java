package com.dji.Drogon;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WaypointMarkers {

  private static WaypointMarkers waypointDetailsInstance = new WaypointMarkers();;

  private static final Map<Integer, Marker> markers = new ConcurrentHashMap<Integer, Marker>();

  private static final ArrayList<Polyline> polylines = new ArrayList<>();

  private WaypointMarkers() {}

  public static WaypointMarkers getInstance() {
    return waypointDetailsInstance;
  }

  public static void add(Marker marker) {
    markers.put(markers.size(), marker);
  }

  public static void clear() {
    // initializing size here because if it's inside the for loop it will always get the updated size
    int size = markers.size();
    //don't clear the first marker (home)
    for(int i = 1; i < size; i++) {
      markers.get(i).remove();
      markers.remove(i);
    }
    removePolyline();
  }

  public static int size() {
    return markers.size();
  }

  public static LatLng getPosition(int position) {
    return markers.get(position).getPosition();
  }


  public static void addLine(Polyline p) {
    polylines.add(p);
  }

  public static void removePolyline() {
    for(int i = 0; i < polylines.size(); i++) {
      polylines.get(i).remove();
    }
    polylines.clear();
  }
}
