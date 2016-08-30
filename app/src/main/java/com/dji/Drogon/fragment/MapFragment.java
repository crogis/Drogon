package com.dji.Drogon.fragment;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dji.Drogon.MainActivity;
import com.dji.Drogon.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;

public class MapFragment extends Fragment {

  private MapView mapView;
  private GoogleMap googleMap;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);

    View v = inflater.inflate(R.layout.fragment_map, container, false);

    boolean isMainFragment = false;
    if(getArguments() != null) {
      isMainFragment = getArguments().getBoolean("isMain");
    }

    final boolean isMainFragmentFinal = isMainFragment;

    System.out.println("MAP FRAGMENT");
    mapView = (MapView) v.findViewById(R.id.map_view);
    mapView.onCreate(savedInstanceState);
    mapView.onResume();

    try {
      MapsInitializer.initialize(getActivity().getApplicationContext());
      System.out.println("INITIALIZING MAP VIEW");
    } catch (Exception e) {
      e.printStackTrace();
    }

    mapView.setClickable(false);
    mapView.getMapAsync(new OnMapReadyCallback() {
      @Override
      public void onMapReady(GoogleMap mMap) {
        System.out.println("MAP READY!!!");
        googleMap = mMap;

        googleMap.setOnMapClickListener((point) -> {
          if(!isMainFragmentFinal) {
            MainActivity m = (MainActivity)getActivity();
            m.onFragmentChange();
          }
        });

        // For dropping a marker at a point on the Map
//        LatLng sydney = new LatLng(-34, 151);
//        googleMap.addMarker(new MarkerOptions().position(sydney).title("Marker Title").snippet("Marker Description"));

        // For zooming automatically to the location of the marker
//        CameraPosition cameraPosition = new CameraPosition.Builder().target(sydney).zoom(12).build();
//        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
      }
    });


//    SupportMapFragment mapFragment = (SupportMapFragment)(R.id.map_fragment);
//    mapFragment.getMapAsync(this);

    return v;
  }

  @Override
  public void onResume() {
    super.onResume();
    mapView.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();
    mapView.onPause();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    mapView.onDestroy();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }
}
