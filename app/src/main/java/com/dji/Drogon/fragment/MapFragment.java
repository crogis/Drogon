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
import com.dji.Drogon.R;
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
import com.google.android.gms.maps.model.PolylineOptions;
import com.squareup.otto.Subscribe;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
  private final Map<Integer, Marker> waypointMarkers = new ConcurrentHashMap<Integer, Marker>();

  private float missionAltitude = 10.0f;
  private float missionSpeed = 10.0f;
  private DJIWaypointMissionFinishedAction missionFinishedAction = DJIWaypointMissionFinishedAction.GoHome;
  private DJIWaypointMissionHeadingMode missionHeadingMode = DJIWaypointMissionHeadingMode.Auto;
  private DJIWaypointMission waypointMission;
  private DJIMissionManager missionManager;

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

//        LatLng dronePosition = new LatLng(droneLocationLat, droneLocationLng);
//        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(dronePosition, 18.0f));
        cameraUpdate();
        updateDroneLocation();
      }
    });
    return view;
  }

  private OnMapClickListener mapClickListener = new OnMapClickListener() {
    @Override
    public void onMapClick(LatLng point) {
      markWaypoint(point);
      DJIWaypoint waypoint = new DJIWaypoint(point.latitude, point.longitude, missionAltitude);

      if (isNotNull(waypointMission)) {
        waypointMission.addWaypoint(waypoint);
        showToast("AddWaypoint");
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
    waypointMarkers.put(waypointMarkers.size(), marker);

    addLines();
  }

  private void addLines() {

    PolylineOptions options = new PolylineOptions().width(10).color(Color.BLACK).geodesic(true);
    for (int z = 0; z < waypointMarkers.size(); z++) {
      LatLng point = waypointMarkers.get(z).getPosition();
      options.add(point);
    }
    googleMap.addPolyline(options);
//    PolylineOptions options = new PolylineOptions().width(10).color(Color.BLACK).geodesic(true);
//    options.add(point);
//    googleMap.addPolyline(options);
  }

  private MissionProgressStatusCallback missionProgressStatusCallback = new MissionProgressStatusCallback() {
    @Override
    public void missionProgressStatus(DJIMissionProgressStatus status) {

    }
  };

  //initializes flight controller and mission manager
  private void initializeDJI() {
//    showToast("Init flight Controller");
    DJIBaseProduct product = DrogonApplication.getProductInstance();
    if(isNotNull(product) && product.isConnected() && product instanceof DJIAircraft) {
      //flight
      flightController = ((DJIAircraft) product).getFlightController();

      //mission
      missionManager = product.getMissionManager();
      missionManager.setMissionProgressStatusCallback(missionProgressStatusCallback);
      missionManager.setMissionExecutionFinishedCallback(setMissionExecutionCallback);
    } else {
//      showToast("Product not connected :(");
      missionManager = null;
    }

    if(isNotNull(flightController)) {
      flightController.setUpdateSystemStateCallback(new FlightControllerUpdateSystemStateCallback(){
        @Override
        public void onResult(DJIFlightControllerCurrentState state) {
          DJILocationCoordinate3D aircraftLocation = state.getAircraftLocation();
          droneLocationLat = aircraftLocation.getLatitude();
          droneLocationLng = aircraftLocation.getLongitude();
          updateDroneLocation();
        }
      });
    }

    waypointMission = new DJIWaypointMission();
  }

  private void updateDroneLocation(){
    if (checkGpsCoordinates(droneLocationLat, droneLocationLng)) {
      LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
      //Create MarkerOptions object
      final MarkerOptions markerOptions = new MarkerOptions();
      markerOptions.position(pos);
      markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.drone_for_map));
      getActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          if (droneMarker != null) {
            droneMarker.remove();
          }
          if (checkGpsCoordinates(droneLocationLat, droneLocationLng)) {
            droneMarker = googleMap.addMarker(markerOptions);
          }
        }
      });
    }
  }

  @Subscribe
  public void onTakeOffClicked(TakeOffClicked clicked) {
    configWayPointMission();
    prepareWayPointMission();
    PolylineOptions options = new PolylineOptions().width(10).color(Color.BLACK).geodesic(true);
    for (int z = 0; z < waypointMarkers.size(); z++) {
      LatLng point = waypointMarkers.get(z).getPosition();
      options.add(point);
    }
    googleMap.addPolyline(options);
  }

  private void configWayPointMission(){
    if (isNotNull(waypointMission)){
      waypointMission.finishedAction = missionFinishedAction;
      waypointMission.headingMode = missionHeadingMode;
      waypointMission.autoFlightSpeed = missionSpeed;

      //todo remove this if statement and the toast
      if(waypointMission.waypointsList.size() > 0) {
        for (int i = 0; i< waypointMission.waypointsList.size(); i++){
          waypointMission.getWaypointAtIndex(i).altitude = missionAltitude;
        }
        showToast("Successfully set waypoints");
      }
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
    } else {
      //todo find a way to not add a position
      LatLng defaultPosition = new LatLng(14.609592, 121.079647);
      camUpdate = CameraUpdateFactory.newLatLngZoom(defaultPosition, zoomLevel);
      markHome(defaultPosition);
    }
    googleMap.moveCamera(camUpdate);

    double meters = 0.013;

    LatLng center = new LatLng(14.609592, 121.079647);

    LatLng bearingLatLng = new LatLng(14.609709, 121.079457);

    double bearing = angleFromCoordinate2(center, bearingLatLng);

    GetDestinationPoint(center, (float) bearing, (float)meters);
//    googleMap.addPolygon(
//      new PolygonOptions()
//        .addAll(createRectangle(new LatLng(14.609592, 121.079647), 5, 5))
//        .fillColor(Color.CYAN)
//        .strokeColor(Color.BLUE)
//        .strokeWidth(5)
//    );

    //meters?
//    doSomething(center, 1, 1);


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
      showToast("Execution finished: " + (error == null ? "Success" : error.getDescription()));
    }
  };

  private DJICompletionCallback prepareMissionCallback = new DJICompletionCallback() {
    @Override
    public void onResult(DJIError error) {
      if(isNotNull(error)) {
        startWaypointMission();
      } else {
        //show toast that's there's an error
      }
      showToast("Prepare mission: " + (error == null ? "Success" : error.getDescription()));
    }
  };

  private DJICompletionCallback startMissionExecutionCallback = new DJICompletionCallback() {
    @Override
    public void onResult(DJIError error) {
      showToast("Start mission: " + (error == null ? "Success" : error.getDescription()));
    }
  };

  private DJICompletionCallback stopMissionExecutionCallback = new DJICompletionCallback() {
    @Override
    public void onResult(DJIError error) {
      showToast("Stop mission: " + (error == null ? "Success" : error.getDescription()));
    }
  };

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

  public void showToast(String msg) {
    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
  }

  public <T> boolean isNotNull(T i) {
    return i != null;
  }

  public <T> boolean isNull(T i) {
    return i == null;
  }
}
