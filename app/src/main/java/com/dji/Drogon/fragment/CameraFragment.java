package com.dji.Drogon.fragment;

//view na nakikita ng quad

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.dji.Drogon.DrogonApplication;
import com.dji.Drogon.R;
import com.dji.Drogon.domain.MainLayoutDimens;
import com.dji.Drogon.event.CaptureImageClicked;
import com.dji.Drogon.event.FragmentChange;
import com.squareup.otto.Subscribe;

import dji.sdk.Camera.DJICamera;
import dji.sdk.Camera.DJICameraSettingsDef;
import dji.sdk.Camera.DJICameraSettingsDef.*;
import dji.sdk.Codec.DJICodecManager;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;

import butterknife.BindView;
import butterknife.ButterKnife;
import dji.sdk.base.DJIError;

public class CameraFragment extends Fragment implements TextureView.SurfaceTextureListener {

  private static final String TAG = CameraFragment.class.getName();
  protected DJICamera.CameraReceivedVideoDataCallback receivedVideoDataCallback = null;
  protected DJICodecManager codecManager = null;

  MainLayoutDimens dimens = MainLayoutDimens.getInstance();

  @BindView(R.id.video_surface_texture_view) TextureView videoSurfaceTextureView;

  private boolean isMapFragmentMain = true;

  protected BroadcastReceiver onConnectionChangeReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      onProductChange();
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

    View view = inflater.inflate(R.layout.fragment_camera, container, false);
    ButterKnife.bind(this, view);

    //Register BroadcastReceiver
    //This receiver gets the connection change of the drone and the android application
    IntentFilter filter = new IntentFilter();
    filter.addAction(DrogonApplication.FLAG_CONNECTION_CHANGE_FRAGMENT);
    getContext().registerReceiver(onConnectionChangeReceiver, filter);

    videoSurfaceTextureView.setSurfaceTextureListener(this);

    //receives video data from your drone
    receivedVideoDataCallback = new DJICamera.CameraReceivedVideoDataCallback() {
      @Override
      public void onResult(byte[] videoBuffer, int size) {
        //send the raw H264 video data to codec manager for decoding
        if(isNotNull(codecManager)) codecManager.sendDataToDecoder(videoBuffer, size);
        else {
//          showToast("codecManager is null");
          Log.e(TAG, "codecManager is null");
        }
      }
    };

    return view;
  }

  @Subscribe
  public void onFragmentChange(FragmentChange change) {
    System.out.println("ON FRAGMENT CHANGE " + videoSurfaceTextureView.getWidth());
    //isMapFragmentMain = false
    isMapFragmentMain = change.getIsMapFragmentMain();

    if(isNotNull(codecManager)) {
      codecManager.cleanSurface();
      codecManager = null;
    }
  }

  @Subscribe
  public void onCaptureImageClicked(CaptureImageClicked clicked) {
    DJIBaseProduct product = DrogonApplication.getProductInstance();
    if(!product.getModel().equals(DJIBaseProduct.Model.UnknownAircraft)) {
      //gets the camera of the product/drone
      DJICamera camera = product.getCamera();
      if(isNotNull(camera)) {
        CameraMode cameraMode = CameraMode.ShootPhoto;
        if(isNotNull(camera)) {
          CameraShootPhotoMode photoMode = CameraShootPhotoMode.Single;
          //shoots photos
          camera.startShootPhoto(photoMode, new DJIBaseComponent.DJICompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
//              String msg = isNotNull(djiError) ? djiError.getDescription() : "SUCCESS!";
//              showToast("CAPTURED " + msg);
            }
          });
        }
      }
    }
  }

  @Override
  public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
    Log.e(TAG, "onSurfaceTextureAvailable");
    if(isNull(codecManager)) {
      codecManager = new DJICodecManager(getContext(), surface, width, height);
    }
  }

  @Override
  public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    Log.e(TAG, "onSurfaceTextureSizeChanged1 " + " width: " +  width + " width: " + height);

    boolean mainCondition = isNull(codecManager);
    //initializes the codec manager is the CameraFragment is the main fragment OR if it's the subfragment
    boolean condition1 = mainCondition && !isMapFragmentMain && width == dimens.getWidth() && height == dimens.getHeight();
    boolean condition2 = mainCondition && isMapFragmentMain;

    if (condition1 || condition2) {
      //DJICodecManager handles the pictures, yung kung ano yung nakikita ng camera ng drone
      codecManager = new DJICodecManager(getContext(), surface, width, height);
    }
  }

  @Override
  public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
    Log.e(TAG, "onSurfaceTextureDestroyed");
    if(isNotNull(codecManager)) {
      codecManager.cleanSurface();
      codecManager = null;
    }
    return false;
  }

  @Override
  public void onSurfaceTextureUpdated(SurfaceTexture surface) {}

  protected void onProductChange() {
    initPreviewer();
  }
  String test = "test";

  private void initPreviewer() {
    DJIBaseProduct product = DrogonApplication.getProductInstance();
    if(isNull(product) || !product.isConnected()) {
//      showToast(getString(R.string.disconnected));
      uninitPreviewer();
    } else {
//      showToast("CONNECTED!!!!");
      if(isNotNull(videoSurfaceTextureView)) {
        videoSurfaceTextureView.setSurfaceTextureListener(this);
      }
      if(!product.getModel().equals(DJIBaseProduct.Model.UnknownAircraft)) {
        DJICamera camera = product.getCamera();
        if(isNotNull(camera)) {
          test = "processed";
          camera.setCameraMode(CameraMode.ShootPhoto, new DJIBaseComponent.DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
              if(isNotNull(error)) {
//                showToast("error setting camera mode! " + error.getDescription());
                Log.e(TAG, "Error setting camera mode " + error.getDescription());
              }
            }
          });
          camera.setDJICameraReceivedVideoDataCallback(receivedVideoDataCallback);
        }
      }
    }
  }

  private void uninitPreviewer() {
    DJICamera camera = DrogonApplication.getCameraInstance();
    if(isNotNull(camera)) {
      camera.setDJICameraReceivedVideoDataCallback(null);
    }
  }

  @Override
  public void onResume() {
    Log.e(TAG, "onResume");
    super.onResume();
    DrogonApplication.getBus().register(this);
    onProductChange();
    if(isNull(videoSurfaceTextureView)) {
      Log.e(TAG, "mVideoSurface is null");
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    try {
      DrogonApplication.getBus().unregister(this);
    } catch(Exception e) {
      Log.e(TAG, "Unable to unregister fragment: " + e.getMessage());
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    getContext().unregisterReceiver(onConnectionChangeReceiver);
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
