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
import com.google.android.gms.maps.model.PolylineOptions;
import com.squareup.otto.Subscribe;

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

  private void markWaypoint(LatLng point){
    //Create MarkerOptions object
    MarkerOptions markerOptions = new MarkerOptions();
    markerOptions.position(point);
    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.map_marker));
    Marker marker = googleMap.addMarker(markerOptions);
    waypointMarkers.put(waypointMarkers.size(), marker);
  }

  private MissionProgressStatusCallback missionProgressStatusCallback = new MissionProgressStatusCallback() {
    @Override
    public void missionProgressStatus(DJIMissionProgressStatus status) {

    }
  };

  //initializes flight controller and mission manager
  private void initializeDJI() {
    showToast("Init flight Controller");
    DJIBaseProduct product = DrogonApplication.getProductInstance();
    if(isNotNull(product) && product.isConnected() && product instanceof DJIAircraft) {
      //flight
      flightController = ((DJIAircraft) product).getFlightController();

      //mission
      missionManager = product.getMissionManager();
      missionManager.setMissionProgressStatusCallback(missionProgressStatusCallback);
      missionManager.setMissionExecutionFinishedCallback(setMissionExecutionCallback);
    } else {
      showToast("Product not connected :(");
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
    PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
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
    }
    googleMap.moveCamera(camUpdate);
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
