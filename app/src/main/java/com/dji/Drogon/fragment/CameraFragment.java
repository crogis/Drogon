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
        else Log.e(TAG, "codecManager is null");
      }
    };

    return view;
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
    Log.e(TAG, "onSurfaceTextureSizeChanged");
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

  @Override
  public void onResume() {
    Log.e(TAG, "onResume");
    super.onResume();
    onProductChange();
    if(isNull(videoSurfaceTextureView)) {
      Log.e(TAG, "mVideoSurface is null");
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
