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
import com.dji.Drogon.db.DrogonDatabase;
import com.dji.Drogon.domain.Altitude;
import com.dji.Drogon.domain.Picture;
import com.dji.Drogon.domain.WritableDBMission;
import com.dji.Drogon.domain.MissionDetails;
import com.dji.Drogon.domain.WaypointMarkers;
import com.dji.Drogon.event.ClearWaypointsClicked;
import com.dji.Drogon.event.StopMissionClicked;
import com.dji.Drogon.event.WaypointAdded;
import com.dji.Drogon.event.TakeOffClicked;
import com.dji.Drogon.helper.DistanceComputation;
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
import com.google.maps.android.SphericalUtil;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import dji.sdk.Camera.DJICamera;
import dji.sdk.Camera.DJICameraSettingsDef;
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
import dji.sdk.base.DJIBaseComponent;
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

  private float missionSpeed = 1.0f;//10.0f;//1.0f; // 1m/s
  private DJIWaypointMissionFinishedAction missionFinishedAction = DJIWaypointMissionFinishedAction.GoHome;
  private DJIWaypointMissionHeadingMode missionHeadingMode = DJIWaypointMissionHeadingMode.Auto;
  private DJIWaypointMission waypointMission;
  private DJIMissionManager missionManager;

  Altitude chosenAltitude = Altitude.LEVEL_10;

  LatLng homeLatLng = null;

  WritableDBMission writableDBMission = null;

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
    filter.addAction(DrogonApplication.FLAG_CONNECTION_CHANGE_FRAGMENT);
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

        //todo remove this after testing
//        googleMap.clear();
//        droneLocationLat = 14.609592;
//        droneLocationLng = 121.079647;
//        cameraUpdate();
//        LatLng point0 = new LatLng(droneLocationLat, droneLocationLng);
//        markHome(point0);

        //todo uncomment this
        updateDroneLocation();
        setHomeCoordinate();
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

//      markWaypoint(point);
    }
  };

  List<LatLng> marked = new ArrayList<>();

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
//    markerOptions.anchor((float)0.5, (float)0.5);
    Marker marker = googleMap.addMarker(markerOptions);
    markers.add(marker);
    //todo move this
    markers.addLine(addPolyline());

    marked = DistanceComputation.getMarkersGivenDistance(markers, chosenAltitude.getCaptureDistance());
    for(LatLng p: marked) {
      addRectangle(p);
    }

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

  double SameThreshold = 0.1;
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

    if(isNotNull(flightController)) {
      flightController.setUpdateSystemStateCallback(new FlightControllerUpdateSystemStateCallback(){
        @Override
        public void onResult(DJIFlightControllerCurrentState state) {
          DJILocationCoordinate3D aircraftLocation = state.getAircraftLocation();
          droneLocationLat = aircraftLocation.getLatitude();
          droneLocationLng = aircraftLocation.getLongitude();

          LatLng curr = new LatLng(droneLocationLat, droneLocationLng);
          System.out.println("curr_loc1" + curr.toString());

          for(LatLng l: marked) {
            if(SphericalUtil.computeDistanceBetween(l, curr) < SameThreshold) {
              showToast("PASSING " + marked.indexOf(l));
              addPicture(droneLocationLat, droneLocationLng);
              capture();
            }
          }
          setHomeCoordinate();
          updateDroneLocation();
//          enableLeftSideButtons();
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
//          hideNotification();
        } else if (!checkGpsCoordinates(droneLocationLat, droneLocationLng) && isNull(homeLatLng)){
//          showNotification("Drone Location Not Found");
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
      markerOptions.icon(BitmapDescriptorFactory.defaultMarker());
//      markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.drone_for_map));
//      markerOptions.anchor((float)0.5, (float)0.5);
      getActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          if (isNotNull(droneMarker)) {
            droneMarker.remove();
          }
          droneMarker = googleMap.addMarker(markerOptions);
        }
      });
    }
  }

  private void enableLeftSideButtons() {
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (checkGpsCoordinates(droneLocationLat, droneLocationLng)) {
          LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
          if(!missionDetails.isMissionInProgress() && pos == homeLatLng) {
            mainActivity().takeOffImageBtn.setEnabled(true);
            mainActivity().clearWaypointsImageBtn.setEnabled(true);
          } else if(!missionDetails.isMissionInProgress()) {
            mainActivity().takeOffImageBtn.setEnabled(false);
            mainActivity().takeOffImageBtn.setImageResource(R.drawable.take_off);
          }
        }
      }
    });

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
    chosenAltitude = mainActivity().getChosenAltitude();

    waypointMission.removeAllWaypoints();

    String listString = "";
    for (LatLng s : marked)
    {
      listString += s.toString() + "\n";
    }

    System.out.println("MARKERS " + listString);

    for(int i = 0; i < markers.size(); i++) {
      LatLng point = markers.getPosition(i);
      DJIWaypoint waypoint = new DJIWaypoint(point.latitude, point.longitude, chosenAltitude.getAltitude());
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
  public void onClearWaypointsClicked(ClearWaypointsClicked clicked) {
    markers.clear();

    marked.clear();

    waypointMission.removeAllWaypoints();
  }

  private void configWayPointMission(){
    if (isNotNull(waypointMission)){
      waypointMission.finishedAction = missionFinishedAction;
      waypointMission.headingMode = missionHeadingMode;
      waypointMission.autoFlightSpeed = missionSpeed;

//      for (int i = 0; i< waypointMission.waypointsList.size(); i++){
//        waypointMission.getWaypointAtIndex(i).altitude = missionAltitude;
//      }
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
  }

  private void addRectangle(LatLng center) {
    List<LatLng> d  = DistanceComputation.getSquareCoordinates(center, chosenAltitude.getHeightInKM(), chosenAltitude.getWidthInKM());
    int color = getResources().getColor(R.color.transparentWhite);
    googleMap.addPolygon(new PolygonOptions()
      .addAll(d)
      .fillColor(color)
      .strokeColor(Color.BLACK)
      .strokeWidth(2)
    );
  }

  private DJICompletionCallback setMissionExecutionCallback = new DJICompletionCallback() {
    @Override
    public void onResult(DJIError error) {
      if(isNotNull(error)) {
        long durationInS = missionDetails.setMissionStop();
        writableDBMission.setFlightDuration(durationInS);
        if(isNotNull(writableDBMission)) {
          final int rowId = addMissionToDB(writableDBMission);
          addPictureEntryToDB(picturesTaken, rowId);
//        getActivity().runOnUiThread(new Runnable() {
//          @Override
//          public void run() {
//            DrogonApplication.getBus().post(new MissionCompleted(rowId));
//          }
//        });
        }
      }
      showNativeToast("Execution finished: " + (isNull(error) ? "Success" : error.getDescription()));
    }
  };

  private DJICompletionCallback prepareMissionCallback = new DJICompletionCallback() {
    @Override
    public void onResult(DJIError error) {
      if(isNull(error)) {
        startWaypointMission();
      }
      showNativeToast("Prepare mission: " + (isNull(error) ? "Success" : error.getDescription()));
    }
  };

  ArrayList<Picture> picturesTaken = new ArrayList<>();

  private DJICompletionCallback startMissionExecutionCallback = new DJICompletionCallback() {
    @Override
    public void onResult(DJIError error) {
      final int imgResource = isNull(error) ? R.drawable.cancel_mission : R.drawable.take_off;
      getActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          mainActivity().takeOffImageBtn.setImageResource(imgResource);
        }
      });

      if(isNull(error)) {
        missionDetails.setMissionStart();
        picturesTaken.clear();

        if(isNotNull(homeLatLng)) {
          addPicture(homeLatLng.latitude, homeLatLng.longitude);
          capture();
          writableDBMission = new WritableDBMission(new Date(), homeLatLng.latitude, homeLatLng.longitude, (int)chosenAltitude.getAltitude());
        }
      }
      showToast("Mission Started: " + (isNull(error) ? "Success" : error.getDescription()));
    }
  };

  private void addPicture(double lat, double lng) {
    picturesTaken.add(new Picture(new Date(), lat, lng));
  }

  private DJICompletionCallback stopMissionExecutionCallback = new DJICompletionCallback() {
    @Override
    public void onResult(final DJIError error) {
      final int imgResource = isNull(error) ? R.drawable.take_off : R.drawable.cancel_mission;
      final String status = isNull(error) ? "Success" : error.getDescription();
      getActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          mainActivity().takeOffImageBtn.setImageResource(imgResource);
          if(isNull(error)) {
            long durationInS = missionDetails.setMissionStop();
            mainActivity().clearWaypointsImageBtn.setEnabled(true);

            writableDBMission.setFlightDuration(durationInS);
            if(isNotNull(writableDBMission)) {
              addMissionToDB(writableDBMission);
            }
          }
          showToast("Mission Stopped: " + status);
        }
      });

      if(isNotNull(waypointMission)) {
        waypointMission.removeAllWaypoints();
      }
    }
  };

  private int addMissionToDB(WritableDBMission mission) {
    return mainActivity().database.insertMission(mission);
  }

  private void addPictureEntryToDB(List<Picture> pictures, int missionId) {
    DrogonDatabase db = mainActivity().database;

    for(Picture p: pictures) {
      db.insertPictureEntry(p, missionId);
    }
  }

  private void capture() {
    DJIBaseProduct product = DrogonApplication.getProductInstance();
    if(!product.getModel().equals(DJIBaseProduct.Model.UnknownAircraft)) {
      DJICamera camera = product.getCamera();
      if(isNotNull(camera)) {
        DJICameraSettingsDef.CameraMode cameraMode = DJICameraSettingsDef.CameraMode.ShootPhoto;
        if(isNotNull(camera)) {
          DJICameraSettingsDef.CameraShootPhotoMode photoMode = DJICameraSettingsDef.CameraShootPhotoMode.Single;
          camera.startShootPhoto(photoMode, new DJIBaseComponent.DJICompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
              String msg = isNotNull(djiError) ? djiError.getDescription() : "SUCCESS!";
              showToast("CAPTURED " + msg);
            }
          });

        }
      }
    }
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
