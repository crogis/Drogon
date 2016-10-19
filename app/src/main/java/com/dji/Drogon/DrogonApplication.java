package com.dji.Drogon;

import android.app.Application;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.squareup.otto.Bus;

import dji.sdk.Camera.DJICamera;
import dji.sdk.Gimbal.DJIGimbal;
import dji.sdk.Products.DJIAircraft;
import dji.sdk.Products.DJIHandHeld;
import dji.sdk.SDKManager.DJISDKManager;
import dji.sdk.SDKManager.DJISDKManager.*;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIError;
import dji.sdk.base.DJIGimbalError;
import dji.sdk.base.DJISDKError;

public class DrogonApplication extends Application {

  public static final String FLAG_CONNECTION_CHANGE = "drogon_connection_change";
  public static final String FLAG_CONNECTION_CHANGE_FRAGMENT = "drogon_connection_change_fragment";

  private static DJIBaseProduct mProduct;

  private Handler mHandler;

  private static Bus bus;

  @Override
  public void onCreate() {
    super.onCreate();
    mHandler = new Handler(Looper.getMainLooper());
    //used to start SDK services and initiate SDK
    DJISDKManager.getInstance().initSDKManager(this, mDJISDKManagerCallback);
  }

  public static Bus getBus() {
    if(bus == null) {
      bus = new Bus();
    }
    return bus;
  }

  /*When starting SDK services, an instance of interface of DJISDKManagerCallback will be used to
   listen to the SDK Registration result and the product changing
   */
  private DJISDKManagerCallback mDJISDKManagerCallback = new DJISDKManagerCallback() {
    //listens to the sdk registration result
    //checks the application registration status
    @Override
    public void onGetRegisteredResult(DJIError dji) {
      if(dji == DJISDKError.REGISTRATION_SUCCESS) {
        onUIThread(
          new Runnable() {
            @Override
            public void run() {
              showToast("Register Success");
            }
          }
        );
        DJISDKManager.getInstance().startConnectionToProduct();
      }
      else
        onUIThread(
          new Runnable() {
            @Override
            public void run() {
              showToast("Register sdk fails, check network is available");
            }
          }
        );
      Log.e("TAG", dji.toString());
    }

    //Listens to the connected product changing, including two parts, component changing or product connection_75 changing.
    //checks the product connection_75 status and invoke the notifyStatusChange to notify status changes
    @Override
    public void onProductChanged(DJIBaseProduct oldProduct, DJIBaseProduct newProduct) {
      mProduct = newProduct;
      if(mProduct != null) {
        mProduct.setDJIBaseProductListener(mDJIBaseProductListener);
      }
      notifyStatusChange();
    }
  };

  private DJIBaseProduct.DJIBaseProductListener mDJIBaseProductListener = new DJIBaseProduct.DJIBaseProductListener() {
    //checks the product component change status and invoke the notifyStatusChange to notify status changes
    @Override
    public void onComponentChange(DJIBaseProduct.DJIComponentKey key, DJIBaseComponent oldComponent, DJIBaseComponent newComponent) {
      if(newComponent != null) {
        newComponent.setDJIComponentListener(mDJIComponentListener);
      }
      notifyStatusChange();
    }

    @Override
    public void onProductConnectivityChanged(boolean b) {
      notifyStatusChange();
    }
  };

  private DJIBaseComponent.DJIComponentListener mDJIComponentListener = new DJIBaseComponent.DJIComponentListener() {
    @Override
    public void onComponentConnectivityChanged(boolean b) {
      notifyStatusChange();
    }
  };

  //gets the instance of DJIBaseProduct, if no product is connected, returns null
  public static synchronized DJIBaseProduct getProductInstance() {
    if(mProduct == null) {
      mProduct = DJISDKManager.getInstance().getDJIProduct();
    }
    return mProduct;
  }

  public static synchronized DJICamera getCameraInstance() {
    return (getProductInstance() == null) ? null : getProductInstance().getCamera();
  }

  public static synchronized DJIGimbal getGimbalInstance() {
    return (getProductInstance() == null) ? null : getProductInstance().getGimbal();
  }

  public static boolean isAircraftConnected() {
    DJIBaseProduct product = getProductInstance();
    return product != null && product.isConnected() && product instanceof DJIAircraft;
  }

  public static boolean isHandHeldConnected() {
    return getProductInstance() != null && getProductInstance() instanceof DJIHandHeld;
  }

  private void notifyStatusChange() {
    mHandler.removeCallbacks(updateRunnable);
    mHandler.postDelayed(updateRunnable, 500);
  }

  private Runnable updateRunnable = new Runnable() {
    @Override
    public void run() {
      Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
      sendBroadcast(intent);

      Intent intent2 = new Intent(FLAG_CONNECTION_CHANGE_FRAGMENT);
      sendBroadcast(intent2);
    }
  };

  private void showToast(String msg) {
    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
  }

  private void onUIThread(Runnable r) {
    Handler handler = new Handler(Looper.getMainLooper());
    handler.post(r);
  }

}
