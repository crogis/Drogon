package com.dji.Drogon.fragment;

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
import com.dji.Drogon.event.FragmentChange;
import com.squareup.otto.Subscribe;

import dji.sdk.Camera.DJICamera;
import dji.sdk.Codec.DJICodecManager;
import dji.sdk.base.DJIBaseProduct;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CameraFragment extends Fragment implements TextureView.SurfaceTextureListener {

  private static final String TAG = CameraFragment.class.getName();
  protected DJICamera.CameraReceivedVideoDataCallback receivedVideoDataCallback = null;
  protected DJICodecManager codecManager = null;

  @BindView(R.id.video_surface_texture_view) TextureView videoSurfaceTextureView;

  private boolean isMapFragmentMain = true;
  //Height and width of the full screen
  private int originalWidth, originalHeight = 0;

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

    videoSurfaceTextureView.setSurfaceTextureListener(this);

    receivedVideoDataCallback = new DJICamera.CameraReceivedVideoDataCallback() {
      @Override
      public void onResult(byte[] videoBuffer, int size) {
        //send the raw H264 video data to codec manager for decoding
        if(isNotNull(codecManager)) codecManager.sendDataToDecoder(videoBuffer, size);
        else {
          showToast("codecManager is null");
          Log.e(TAG, "codecManager is null");
        }
      }
    };

    return view;
  }

  @Subscribe
  public void onFragmentChange(FragmentChange change) {
    System.out.println("ON FRAGMENT CHANGE " + videoSurfaceTextureView.getWidth());
    isMapFragmentMain = change.getIsCameraFragmentMain();
    if(isNotNull(codecManager)) {
      codecManager.cleanSurface();
      codecManager = null;
    }
  }

  @Override
  public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
    Log.e(TAG, "onSurfaceTextureAvailable");
    if(isNull(codecManager)) {
      originalWidth = width;
      originalHeight = height;
      codecManager = new DJICodecManager(getContext(), surface, width, height);
    }
  }

  @Override
  public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    Log.e(TAG, "onSurfaceTextureSizeChanged " + " width: " +  width + " width: " + height);

    boolean mainCondition = isNull(codecManager);
    boolean condition1 = mainCondition && isMapFragmentMain && width == originalWidth && height == originalHeight;
    boolean condition2 = mainCondition && !isMapFragmentMain;
    if (condition1 || condition2) {
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

  private void initPreviewer() {
    DJIBaseProduct product = DrogonApplication.getProductInstance();
    if(isNull(product) || !product.isConnected()) {
      showToast(getString(R.string.disconnected));
    } else {
      if(isNotNull(videoSurfaceTextureView)) {
        videoSurfaceTextureView.setSurfaceTextureListener(this);
      }
      if(!product.getModel().equals(DJIBaseProduct.Model.UnknownAircraft)) {
        DJICamera camera = product.getCamera();
        if(isNotNull(camera)) {
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

  public void showToast(String msg) {
    Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
  }

  public <T> boolean isNotNull(T i) {
    return i != null;
  }

  public <T> boolean isNull(T i) {
    return i == null;
  }
}
