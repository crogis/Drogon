package com.dji.Drogon.fragment;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.dji.Drogon.DrogonApplication;
import com.dji.Drogon.MainActivity;
import com.dji.Drogon.R;
import com.dji.Drogon.domain.DBMission;
import com.dji.Drogon.domain.MissionDetails;
import com.dji.Drogon.domain.WaypointMarkers;
import com.dji.Drogon.event.ClearWaypointsClicked;
import com.dji.Drogon.event.GoHomeClicked;
import com.dji.Drogon.event.StopMissionClicked;
import com.dji.Drogon.event.WaypointAdded;
import com.dji.Drogon.event.TakeOffClicked;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.*;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.squareup.otto.Subscribe;

import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import dji.sdk.FlightController.DJIFlightController;
import dji.sdk.FlightController.DJIFlightControllerDataType.*;
import dji.sdk.FlightController.DJIFlightControllerDelegate.*;
import dji.sdk.MissionManager.DJIMission;
import dji.sdk.MissionManager.DJIMission.*;
import dji.sdk.MissionManager.DJIMissionManager;
import dji.sdk.MissionManager.DJIMissionManager.*;
import dji.sdk.MissionManager.DJIWaypoint;
import dji.sdk.MissionManager.DJIWaypointMission;
import dji.sdk.MissionManager.DJIWaypointMission.*;
import dji.sdk.Products.DJIAircraft;
import dji.sdk.base.DJIBaseComponent.*;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIError;

public class MapFragment extends Fragment {

  @BindView(R.id.map_view) MapView mapView;

//  private double droneLocationLat = 14.609592, droneLocationLng = 121.079647;
  private double droneLocationLat = 0, droneLocationLng = 0;
  private Marker droneMarker = null;
  private DJIFlightController flightController;

  private GoogleMap googleMap;

  private boolean isAdd = false;

  WaypointMarkers markers = WaypointMarkers.getInstance();
  MissionDetails missionDetails = MissionDetails.getInstance();
//  private final Map<Integer, Marker> waypointMarkers = new ConcurrentHashMap<Integer, Marker>();

  private float missionAltitude = 10.0f;
  private float missionSpeed = 1.0f; // 1m/s
  private DJIWaypointMissionFinishedAction missionFinishedAction = DJIWaypointMissionFinishedAction.GoHome;
  private DJIWaypointMissionHeadingMode missionHeadingMode = DJIWaypointMissionHeadingMode.Auto;
  private DJIWaypointMission waypointMission;
  private DJIMissionManager missionManager;

  double meters = 0.013;

  LatLng homeLatLng = null;

  protected BroadcastReceiver onConnectionChangeReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      initializeDJI();
    }
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    View view = inflater.inflate(R.layout.fragment_map, container, false);

    ButterKnife.bind(this, view);

    //Register BroadcastReceiver
    IntentFilter filter = new IntentFilter();
    filter.addAction(DrogonApplication.FLAG_CONNECTION_CHANGE);
    getContext().registerReceiver(onConnectionChangeReceiver, filter);

    mapView.onCreate(savedInstanceState);
    mapView.onResume();

    try {
      MapsInitializer.initialize(getActivity().getApplicationContext());
    } catch (Exception e) {
      e.printStackTrace();
    }

    mapView.getMapAsync(new OnMapReadyCallback() {
      @Override
      public void onMapReady(GoogleMap mMap) {
        googleMap = mMap;
        googleMap.setOnMapClickListener(mapClickListener);
        //todo remove this
//        cameraUpdate();
        updateDroneLocation();
      }
    });
    return view;
  }

  private OnMapClickListener mapClickListener = new OnMapClickListener() {
    @Override
    public void onMapClick(LatLng point) {
      if(DrogonApplication.isAircraftConnected() && checkGpsCoordinates(droneLocationLat, droneLocationLng)) {
        if (isNotNull(waypointMission)) {
          markWaypoint(point);
          showNativeToast("AddWaypoint");
        }
      } else {
        showNativeToast("Unable to add waypoints");
      }
    }
  };

  private void markHome(LatLng point) {
    markLocation(point, R.drawable.green_circle);
  }

  private void markWaypoint(LatLng point){
    markLocation(point, R.drawable.red_circle);
  }

  private void markLocation(LatLng point, int pointResource) {
    //Create MarkerOptions object
    MarkerOptions markerOptions = new MarkerOptions();
    markerOptions.position(point);
    markerOptions.icon(BitmapDescriptorFactory.fromResource(pointResource));
    markerOptions.anchor((float)0.5, (float)0.5);
    Marker marker = googleMap.addMarker(markerOptions);
    markers.add(marker);
    //todo move this
    markers.addLine(addPolyline());

    DrogonApplication.getBus().post(new WaypointAdded());
  }

  private Polyline addPolyline() {
    PolylineOptions options = new PolylineOptions().width(10).color(Color.BLACK).geodesic(true);
    for (int z = 0; z < markers.size(); z++) {
      LatLng point = markers.getPosition(z);
      options.add(point);
    }
    return googleMap.addPolyline(options);
  }

  private MissionProgressStatusCallback missionProgressStatusCallback = new MissionProgressStatusCallback() {
    @Override
    public void missionProgressStatus(DJIMissionProgressStatus status) {}
  };

  //initializes flight controller and mission manager
  private void initializeDJI() {
    DJIBaseProduct product = DrogonApplication.getProductInstance();
    if(DrogonApplication.isAircraftConnected()) {
      //flight
      flightController = ((DJIAircraft) product).getFlightController();

      //mission
      missionManager = product.getMissionManager();
      missionManager.setMissionProgressStatusCallback(missionProgressStatusCallback);
      missionManager.setMissionExecutionFinishedCallback(setMissionExecutionCallback);
      updateDroneLocation();
    } else {
//      showNativeToast("Product not connected :(");
      missionManager = null;
    }

    showNativeToast("Init flight Controller " + DrogonApplication.isAircraftConnected());

    if(isNotNull(flightController)) {
      showNativeToast("setting state callback");
      flightController.setUpdateSystemStateCallback(new FlightControllerUpdateSystemStateCallback(){
        @Override
        public void onResult(DJIFlightControllerCurrentState state) {
          DJILocationCoordinate3D aircraftLocation = state.getAircraftLocation();
          droneLocationLat = aircraftLocation.getLatitude();
          droneLocationLng = aircraftLocation.getLongitude();
          setHomeCoordinate();
          updateDroneLocation();
        }
      });
    }

    waypointMission = new DJIWaypointMission();
  }

  private void setHomeCoordinate() {
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if(checkGpsCoordinates(droneLocationLat, droneLocationLng) && isNull(homeLatLng)) {
          homeLatLng = new LatLng(droneLocationLat, droneLocationLng);
          markHome(homeLatLng);
          cameraUpdate();
        }
      }
    });
  }

  private void updateDroneLocation(){
    if (checkGpsCoordinates(droneLocationLat, droneLocationLng)) {
      LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
      //Create MarkerOptions object
      final MarkerOptions markerOptions = new MarkerOptions();
      markerOptions.position(pos);
      markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.drone_for_map));
      markerOptions.anchor((float)0.5, (float)0.5);
      getActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          if (isNotNull(droneMarker)) {
            droneMarker.remove();
          }
          if (checkGpsCoordinates(droneLocationLat, droneLocationLng)) {
            droneMarker = googleMap.addMarker(markerOptions);
          }
        }
      });
      hideNotification();
    } else {
      showNotification("Drone Location Not Found");
    }
  }

  private void showToast(final String message) {
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        mainActivity().showToast(message);
      }
    });
  }

  private void showNotification(final String message) {
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        mainActivity().showNotification(message);
      }
    });
  }

  private void hideNotification() {
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
          mainActivity().hideNotification();
      }
    });
  }

  @Subscribe
  public void onTakeOffClicked(TakeOffClicked clicked) {
    for(int i = 0; i < markers.size(); i++) {
      LatLng point = markers.getPosition(i);
      DJIWaypoint waypoint = new DJIWaypoint(point.latitude, point.longitude, missionAltitude);
      waypointMission.addWaypoint(waypoint);
    }
    configWayPointMission();
    prepareWayPointMission();
  }

  @Subscribe
  public void onStopMissionClicked(StopMissionClicked clicked) {
    stopWaypointMission();
  }

  @Subscribe
  public void onGoHomeClicked(GoHomeClicked clicked) {
    stopWaypointMission();
  }

  @Subscribe
  public void onClearWaypointsClicked(ClearWaypointsClicked clicked) {
    markers.clear();
    waypointMission.removeAllWaypoints();
  }

  private void configWayPointMission(){
    if (isNotNull(waypointMission)){
      waypointMission.finishedAction = missionFinishedAction;
      waypointMission.headingMode = missionHeadingMode;
      waypointMission.autoFlightSpeed = missionSpeed;

      for (int i = 0; i< waypointMission.waypointsList.size(); i++){
        waypointMission.getWaypointAtIndex(i).altitude = missionAltitude;
      }
      showNativeToast("Successfully set waypoints");
    }
  }

  private void prepareWayPointMission(){
    if(isNotNull(missionManager) && isNotNull(waypointMission)) {
      DJIMissionProgressHandler progressHandler = new DJIMissionProgressHandler() {
        @Override
        public void onProgress(DJIMission.DJIProgressType type, float progress) {
        }
      };
      missionManager.prepareMission(waypointMission, progressHandler, prepareMissionCallback);
    }
  }

  private void startWaypointMission(){
    if(isNotNull(missionManager)) {
      missionManager.startMissionExecution(startMissionExecutionCallback);
    }
  }

  private void stopWaypointMission(){
    if(isNotNull(missionManager)) {
      missionManager.stopMissionExecution(stopMissionExecutionCallback);
      if(isNotNull(waypointMission)) {
        waypointMission.removeAllWaypoints();
      }
    }
  }

  private void cameraUpdate(){
    CameraUpdate camUpdate;
    float zoomLevel = 18.0f;
    if(checkGpsCoordinates(droneLocationLat, droneLocationLng)) {
      LatLng dronePosition = new LatLng(droneLocationLat, droneLocationLng);
      camUpdate = CameraUpdateFactory.newLatLngZoom(dronePosition, zoomLevel);
      googleMap.moveCamera(camUpdate);
    }
//    else {
//      //todo find a way to not add a position
//      LatLng defaultPosition = new LatLng(14.609592, 121.079647);
//      camUpdate = CameraUpdateFactory.newLatLngZoom(defaultPosition, zoomLevel);
//      markHome(defaultPosition);
//    }
//    googleMap.moveCamera(camUpdate);

    /*

    LatLng center = new LatLng(14.609592, 121.079647);

//    LatLng bearingLatLng = new LatLng(14.609709, 121.079457);
    LatLng bearingLatLng = new LatLng(14.610730, 121.077298);

    markWaypoint(bearingLatLng);
    addSquare(bearingLatLng, meters);


    //returns meters
    Double distance = SphericalUtil.computeDistanceBetween(center, bearingLatLng);
    Double heading = SphericalUtil.computeHeading(center, bearingLatLng);
    System.out.println("DISTANCE " + distance);
    System.out.println("HEADING " + heading);

    Location init = new Location("init");
    init.setLatitude(14.609592);
    init.setLongitude(121.079647);

    Location finalz = new Location("finals");
    finalz.setLatitude(14.610730);
    finalz.setLongitude(121.077298);
    System.out.println("HEADINGZ " + init.bearingTo(finalz));
    System.out.println("HEADINGZ1 " + bearingInRadians(center, bearingLatLng));
    System.out.println("HEADINGZ2 " + bearingInDegrees(center, bearingLatLng));


//    double bearing = angleFromCoordinate2(center, bearingLatLng);
//    LatLng dest = GetDestinationPoint(center, (float) bearing, (float)meters);
   */

  }

  public static double bearingInRadians(LatLng src, LatLng dst) {
    double srcLat = Math.toRadians(src.latitude);
    double dstLat = Math.toRadians(dst.latitude);
    double dLng = Math.toRadians(dst.longitude - src.longitude);

    return Math.atan2(Math.sin(dLng) * Math.cos(dstLat),
            Math.cos(srcLat) * Math.sin(dstLat) -
                    Math.sin(srcLat) * Math.cos(dstLat) * Math.cos(dLng));
  }

  public static double bearingInDegrees(LatLng src, LatLng dst) {
    return Math.toDegrees((bearingInRadians(src, dst) + Math.PI) % Math.PI);
  }

  private void addSquare(LatLng center, double meters) {
    LatLng one = doSomething(center, meters, 0); // |
    LatLng two = doSomething(center, -meters, 0); // |

    LatLng upperRight = doSomething(one, 0, meters);
    LatLng upperLeft = doSomething(one, 0, -meters);

    LatLng lowerRight = doSomething(two, 0, meters);
    LatLng lowerLeft = doSomething(two, 0, -meters);

    System.out.println("ONE " + one.toString());
    System.out.println("upper right " + upperRight.toString());
    System.out.println("upper left " + upperLeft.toString());
    System.out.println("lower right " + lowerRight.toString());
    System.out.println("lower right " + lowerLeft.toString());

    List<LatLng> d = Arrays.asList(upperRight, upperLeft, lowerLeft, lowerRight);

    googleMap.addPolygon(
            new PolygonOptions()
                    .addAll(d)
                    .fillColor(Color.CYAN)
                    .strokeColor(Color.BLUE)
                    .strokeWidth(5)
    );

  }

  //radius of the earth
  double rEarth = 6378;//km

  private LatLng doSomething(LatLng center, double dy, double dx) {
    double newLatitude  = center.latitude  + (dy / rEarth) * (180 / Math.PI);
    double newLongitude = center.longitude + (dx / rEarth) * (180 / Math.PI) / Math.cos(center.latitude * Math.PI/180);
    return new LatLng(newLatitude, newLongitude);
  }

  public static LatLng GetDestinationPoint(LatLng startLoc, float bearing, float depth) {

    double radius = 6371.0; // earth's mean radius in km
    double lat1 = Math.toRadians(startLoc.latitude);
    double lng1 = Math.toRadians(startLoc.longitude);
    double brng = Math.toRadians(bearing);
    double lat2 = Math.asin( Math.sin(lat1)*Math.cos(depth/radius) + Math.cos(lat1)*Math.sin(depth/radius)*Math.cos(brng) );
    double lng2 = lng1 + Math.atan2(Math.sin(brng)*Math.sin(depth/radius)*Math.cos(lat1), Math.cos(depth/radius)-Math.sin(lat1)*Math.sin(lat2));
    lng2 = (lng2+Math.PI)%(2*Math.PI) - Math.PI;

    // normalize to -180...+180
//    if (lat2 == 0 || lng2 == 0)
//    {
//      newLocation.setLatitude(0.0);
//      newLocation.setLongitude(0.0);
//    }
//    else
//    {
//      newLocation.setLatitude(Math.toDegrees(lat2));
//      newLocation.setLongitude(Math.toDegrees(lng2));
//    }
    return new LatLng(Math.toDegrees(lat2), Math.toDegrees(lng2));
  }

  private double angleFromCoordinate2(LatLng init, LatLng finalz) {
    double lat1 = init.latitude;
    double long1 = init.longitude;

    double lat2 = finalz.latitude;
    double long2 = finalz.longitude;

    double dLon = (long2 - long1);

    double y = Math.sin(dLon) * Math.cos(lat2);
    double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1)
            * Math.cos(lat2) * Math.cos(dLon);

    double brng = Math.atan2(y, x);

    brng = Math.toDegrees(brng);
    brng = (brng + 360) % 360;
    brng = 360 - brng;

    return brng;
  }

  private List<LatLng> createRectangle(LatLng center, double halfWidth, double halfHeight) {
    return Arrays.asList(new LatLng(center.latitude - halfHeight, center.longitude - halfWidth),
            new LatLng(center.latitude - halfHeight, center.longitude + halfWidth),
            new LatLng(center.latitude + halfHeight, center.longitude + halfWidth),
            new LatLng(center.latitude + halfHeight, center.longitude - halfWidth),
            new LatLng(center.latitude - halfHeight, center.longitude - halfWidth));
  }

  private DJICompletionCallback setMissionExecutionCallback = new DJICompletionCallback() {
    @Override
    public void onResult(DJIError error) {
      showNativeToast("Execution finished: " + (error == null ? "Success" : error.getDescription()));
    }
  };

  private DJICompletionCallback prepareMissionCallback = new DJICompletionCallback() {
    @Override
    public void onResult(DJIError error) {
      if(isNull(error)) {
        startWaypointMission();
      } else {
        showNativeToast("error!!!! :( " + error.getDescription());
        //show toast that's there's an error
      }
      showNativeToast("Prepare mission: " + (error == null ? "Success" : error.getDescription()));
    }
  };

  private DJICompletionCallback startMissionExecutionCallback = new DJICompletionCallback() {
    @Override
    public void onResult(DJIError error) {
      int imgResource = isNull(error) ? R.drawable.cancel_mission : R.drawable.take_off;
      mainActivity().takeOffImageBtn.setImageResource(imgResource);

      if(isNull(error)) {
        missionDetails.setMissionStart();
      }

      String status = isNull(error) ? "Success" : error.getDescription();
      showToast("Mission Started: " + status);
//      showNativeToast("Start mission: " + (error == null ? "Success" : error.getDescription()));
    }
  };

  private DJICompletionCallback stopMissionExecutionCallback = new DJICompletionCallback() {
    @Override
    public void onResult(DJIError error) {
      int imgResource = isNull(error) ? R.drawable.take_off : R.drawable.cancel_mission;
      mainActivity().takeOffImageBtn.setImageResource(imgResource);

      if(isNull(error)) {
        missionDetails.setMissionStop();
        mainActivity().clearWaypointsImageBtn.setEnabled(true);
        mainActivity().goHomeImageBtn.setEnabled(false);
      }
      String status = isNull(error) ? "Success" : error.getDescription();
      showToast("Mission Stopped: " + status);
//      showNativeToast("Stop mission: " + (error == null ? "Success" : error.getDescription()));
    }
  };

  private void addMissionToDB(DBMission mission) {
    mainActivity().database.insertMission(mission);
  }

  @Override
  public void onResume() {
    super.onResume();
    mapView.onResume();
    initializeDJI();
    DrogonApplication.getBus().register(this);
  }

  @Override
  public void onPause() {
    super.onPause();
    mapView.onPause();
    DrogonApplication.getBus().unregister(this);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    mapView.onDestroy();
    getContext().unregisterReceiver(onConnectionChangeReceiver);
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  private static boolean checkGpsCoordinates(double latitude, double longitude) {
    return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
  }

  public void showNativeToast(String msg) {
    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
  }

  public <T> boolean isNotNull(T i) {
    return i != null;
  }

  public <T> boolean isNull(T i) {
    return i == null;
  }

  private MainActivity mainActivity() {
    return (MainActivity)getActivity();
  }
}
