package com.dji.Drogon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dji.Drogon.anim.ExpandCollapseAnimation;
import com.dji.Drogon.anim.FillScreenAnimation;
import com.dji.Drogon.db.DrogonDatabase;
import com.dji.Drogon.domain.Dummy;
import com.dji.Drogon.domain.ReadableDBMission;
import com.dji.Drogon.domain.FileDirectory;
import com.dji.Drogon.domain.MainLayoutDimens;
import com.dji.Drogon.domain.MissionDetails;
import com.dji.Drogon.domain.WaypointMarkers;
import com.dji.Drogon.event.CaptureImageClicked;
import com.dji.Drogon.event.ClearWaypointsClicked;
import com.dji.Drogon.event.FragmentChange;
import com.dji.Drogon.event.MissionCompleted;
import com.dji.Drogon.event.StopMissionClicked;
import com.dji.Drogon.event.TakeOffClicked;
import com.dji.Drogon.event.WaypointAdded;
import com.dji.Drogon.helper.CSVWriter;
import com.dji.Drogon.helper.FileHelper;
import com.dji.Drogon.views.CustomLayoutParams;
import com.dji.Drogon.fragment.CameraFragment;
import com.dji.Drogon.fragment.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dji.sdk.AirLink.DJIAirLink;
import dji.sdk.AirLink.DJILBAirLink;
import dji.sdk.AirLink.DJISignalInformation;
import dji.sdk.Battery.DJIBattery;
import dji.sdk.Battery.DJIBattery.*;
import dji.sdk.Camera.DJICameraSettingsDef;
import dji.sdk.FlightController.DJIFlightController;
import dji.sdk.FlightController.DJIFlightControllerDataType.*;
import dji.sdk.FlightController.DJIFlightControllerDelegate;
import dji.sdk.FlightController.DJINoFlyZoneModel;
import dji.sdk.Gimbal.DJIGimbal;
import dji.sdk.Gimbal.DJIGimbal.*;
import dji.sdk.Products.DJIAircraft;
import dji.sdk.RemoteController.DJIRemoteController;
import dji.sdk.RemoteController.DJIRemoteController.*;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIError;
import dji.sdk.util.DJIParamCapability;
import dji.sdk.util.DJIParamMinMaxCapability;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;

public class MainActivity extends AppCompatActivity {

  @BindView(R.id.main_fragment) FrameLayout mainLayout;
  @BindView(R.id.sub_fragment) FrameLayout subLayout;
  @BindView(R.id.border_layout) FrameLayout borderLayout;
  @BindView(R.id.center_linear_layout) LinearLayout centerLayout;

  @BindView(R.id.parent_fragment_layout) RelativeLayout parentLayout;
  @BindView(R.id.settings_layout) RelativeLayout settingsLayout;

  @BindView(R.id.settings_image_button) ImageButton settingsImageBtn;
  @BindView(R.id.take_off_image_button) public ImageButton takeOffImageBtn;
  @BindView(R.id.clear_waypoints_image_button) public ImageButton clearWaypointsImageBtn;
  @BindView(R.id.capture_image_btn) ImageButton captureImageBtn;

  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.battery_text_view) TextView batteryTextView;
  @BindView(R.id.battery_image_view) ImageView batteryImageView;
  @BindView(R.id.connection_status_text_view) TextView connectionStatusTextView;
  @BindView(R.id.remote_control_connection_image_view) ImageView remoteControlConnectionImageView;
  @BindView(R.id.flight_mode_text_view) TextView flightModeTextView;
  @BindView(R.id.satellite_connection_image_view) ImageView satelliteConnectionImageView;
  @BindView(R.id.satellite_count_text_view) TextView satelliteCountTextView;
  @BindView(R.id.hd_connection_image_view) ImageView hdConnectionImageView;

  //todo revert to private after testing
  @BindView(R.id.dropdown_notification_layout) public LinearLayout notificationLayout;
  @BindView(R.id.notification_text_view) TextView notificationTextView;

  Boolean isMapFragmentMain = true;

  WaypointMarkers markers = WaypointMarkers.getInstance();
  MissionDetails missionDetails = MissionDetails.getInstance();
  MainLayoutDimens dimens = MainLayoutDimens.getInstance();

  public DrogonDatabase database = new DrogonDatabase(this);

  private final String IP_ADDRESS_PREFERENCE = "ip_address";
  private final String DRONE_ANGLE_PREFERENCE = "drone_angle";

  protected BroadcastReceiver onConnectionChangeReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      DJIBaseProduct product = DrogonApplication.getProductInstance();
      if(isNotNull(product) && DrogonApplication.isAircraftConnected()) {
        DJIBattery battery = product.getBattery();
        DJIAirLink airLink = product.getAirLink();
        DJIRemoteController remoteController = ((DJIAircraft) product).getRemoteController();
        DJIFlightController flightController = ((DJIAircraft) product).getFlightController();

        if(isNotNull(battery)) {
          battery.setBatteryStateUpdateCallback(new DJIBatteryStateUpdateCallback() {
            @Override
            public void onResult(DJIBatteryState batteryState) {
              final int percent = batteryState.getBatteryEnergyRemainingPercent();
              final String percentage = batteryState.getBatteryEnergyRemainingPercent() + "%";
              runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  int imageResource = 0;
                  if(percent > 75) imageResource = R.drawable.battery_100;
                  else if(percent > 50) imageResource = R.drawable.battery_75;
                  else if(percent > 25) imageResource = R.drawable.battery_50;
                  else if(percent > 0) imageResource = R.drawable.battery_25;
                  else if(percent == 0) imageResource = R.drawable.battery_0;

                  batteryImageView.setImageResource(imageResource);
                  batteryTextView.setText(percentage);
                }
              });
            }
          });
        }

        if(isNotNull(airLink)) {
          if(airLink.isLBAirLinkSupported()) {
            hdConnectionImageView.setImageResource(R.drawable.connection_100);
            airLink.getLBAirLink().setLBAirLinkUpdatedRemoteControllerSignalInformationCallback(new DJILBAirLink.DJILBAirLinkUpdatedRemoteControllerSignalInformationCallback() {
              @Override
              public void onResult(ArrayList<DJISignalInformation> infoArr) {
                int sum = 0;
                for(DJISignalInformation info: infoArr) {
                  sum += info.getPercent();
                }
                final int aveConnSignal = (sum / infoArr.size());

                runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                    int imageResource = 0;
                    if(aveConnSignal > 75) imageResource = R.drawable.connection_100;
                    else if(aveConnSignal > 50) imageResource = R.drawable.connection_75;
                    else if(aveConnSignal > 25) imageResource = R.drawable.connection_50;
                    else if(aveConnSignal > 0) imageResource = R.drawable.connection_25;
                    else if(aveConnSignal == 0) imageResource = R.drawable.connection_0;
                    remoteControlConnectionImageView.setImageResource(imageResource);
                  }
                });
              }
            });
          }
        }


        if(isNotNull(remoteController) && isNotNull(flightController)) {
//          initializeFlightControllerDetails(flightController, null);
          setRemoteFlightControllerCallbacks(remoteController, flightController);

          showNativeToast("status " + flightController.getCurrentState().getNoFlyStatus().toString());
          flightController.getCurrentState().
          flightController.setReceivedNoFlyZoneCallback(new DJIFlightControllerDelegate.ReceivedNoFlyZoneFromFlightControllerCallback() {
            @Override
            public void onReceivingNoFlyZone(final DJINoFlyZoneModel.DJINoFlyZoneState djiNoFlyZoneState) {
              runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  connectionStatusTextView.setText(djiNoFlyZoneState.toString());
                }
              });
              showNativeToast("FLY ZONE " + djiNoFlyZoneState.toString());
            }
          });
        }
      } else {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            batteryImageView.setImageResource(R.drawable.battery_0);
            remoteControlConnectionImageView.setImageResource(R.drawable.connection_0);
            satelliteConnectionImageView.setImageResource(R.drawable.connection_0);
            hdConnectionImageView.setImageResource(R.drawable.connection_0);
            batteryTextView.setText(R.string.not_applicable);
            flightModeTextView.setText(R.string.not_applicable);
          }
        });
      }

      captureImageBtn.setEnabled(isNotNull(product));
    }
  };


  private void setRemoteFlightControllerCallbacks(DJIRemoteController remoteController, final DJIFlightController flightController) {
    remoteController.setHardwareStateUpdateCallback(new DJIRemoteController.RCHardwareStateUpdateCallback() {
      @Override
      public void onHardwareStateUpdate(final DJIRemoteController controller, final DJIRCHardwareState state) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            String flightSwitchMode = state.flightModeSwitch.mode.name();
            initializeFlightControllerDetails(flightController, flightSwitchMode);
          }
        });
      }
    });
  }

  private void initializeFlightControllerDetails(DJIFlightController flightController, String flightSwitchMode) {
    DJIFlightControllerCurrentState _currentState = flightController.getCurrentState();
    double satelliteCount = _currentState.getSatelliteCount();
    String flightModeName = _currentState.getFlightMode().name();

    //~12 according to forums
    double maxSatelliteCount = 12.00;
    double satellitePercentage = (satelliteCount / maxSatelliteCount) * 100;

    int imageResource = 0;
    if(satellitePercentage > 75) imageResource = R.drawable.connection_100;
    else if(satellitePercentage > 50) imageResource = R.drawable.connection_75;
    else if(satellitePercentage > 25) imageResource = R.drawable.connection_50;
    else if(satellitePercentage > 0) imageResource = R.drawable.connection_25;
    else if(satellitePercentage == 0) imageResource = R.drawable.connection_0;

    satelliteCountTextView.setText(String.valueOf((int)satelliteCount));

    satelliteCountTextView.setText(String.valueOf((int)satelliteCount));
    satelliteConnectionImageView.setImageResource(imageResource);
    String flightMode = flightModeName;
    if(isNotNull(flightSwitchMode)) flightMode = flightSwitchMode + "-" + flightMode;
    flightModeTextView.setText(flightMode);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
      WindowManager.LayoutParams.FLAG_FULLSCREEN);
    setContentView(R.layout.activity_main);

    //used this library to shorten code https://github.com/JakeWharton/butterknife
    ButterKnife.bind(this);

    initializeToolbar();

    takeOffImageBtn.setEnabled(false);
    captureImageBtn.setEnabled(false);

    Fragment cameraFragment = new CameraFragment();
    Fragment mapFragment = new MapFragment();
    addFragmentToMain(mapFragment);
    addFragmentToSub(cameraFragment);

    FileHelper.initializeWaypointDirectory();

    LatLng main = new LatLng(22.537106129131445,113.95363166709372);

    List<LatLng> list = Dummy.getPoints();

    List<LatLng> compare = Dummy.comparePoints();

    for(LatLng l: list) {
//      System.out.println("DISTANCE " + SphericalUtil.computeDistanceBetween(l, main));
      for(LatLng l1: compare) {
        if(SphericalUtil.computeDistanceBetween(l, l1) < 0.3) {
          System.out.println("NEAR!!! " + compare.indexOf(l1) + " " + l.toString());
        }
      }
    }
//    List<WritableDBMission> missions = database.getMissions();
//    System.out.println("READING DATABASE " + missions.size());
//    for(int i = 0; i < missions.size(); i++) {
//      System.out.println(missions.get(i).getMissionId());
//    }

//    simulateCSV();
  }

  @Subscribe
  public void onMissionCompleted(MissionCompleted missionCompleted) {
    int rowId = missionCompleted.getRowId();
    List<ReadableDBMission> missions = database.getMissions();
    final ReadableDBMission mission = missions.get(rowId - 1);
    if(isNotNull(mission)) {
      showNativeToast("WRITING MISSION " + mission.getDateTime().toString());
    }
  }

  public void simulateCSV() {
    Date now = new Date();
    ReadableDBMission mission = new ReadableDBMission(1, now, 1, 1, 100.00, 200.00);

    FileDirectory fd = new FileDirectory(now);
    String parent = fd.getSubDirectoryPath();
    System.out.println("SUB DIRECTORY PATH " + parent);
    FileHelper.createWaypointSubDirectory(parent);

    String csvContent = CSVWriter.generateFromDBMission(mission);
    System.out.println("CONTENT " + csvContent);

    String csvFilePath = fd.getCSVFilePath();
    System.out.println("CSV FILE PATH " + csvFilePath);
    File csvFile = new File(csvFilePath);
    FileHelper.writeToFile(csvFile, csvContent);

    sendToServer(csvFile, fd.getBaseFileName());
  }

  private void sendToServer(final File csvFile, final String dirName) {
    final String ipAddress = getPreferenceString(IP_ADDRESS_PREFERENCE).trim();
    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        try  {
          try {
            try {
              URL u = new URL(ipAddress);
              final String hostPath = u.getHost() + u.getPath();
              FileInputStream fis= new FileInputStream(csvFile);

              String ipAddressPath = "smb://" + hostPath + "/" + dirName;

              showNotification("Sending files to: " + hostPath);

              System.out.println("IP ADDRESS PATH " + ipAddressPath);
              SmbFile dir = new SmbFile(ipAddressPath);
              if(!dir.exists()) dir.mkdir();

              SmbFile f = new SmbFile(ipAddressPath + "/" + csvFile.getName());
              if(!f.exists()) f.createNewFile();
              SmbFileOutputStream os = new SmbFileOutputStream(f, false);
              byte buffer[] = new byte[1024];
              int read;
              while((read = fis.read(buffer)) != -1){
                os.write(buffer, 0, read);
              }
              fis.close();
              os.close();

              hideNotification();

            } catch (final MalformedURLException e){
              showSendingErrorToast(e.getMessage());
              e.printStackTrace();
            }
          } catch (final SmbException e) {
            showSendingErrorToast(e.getMessage());
            e.printStackTrace();
          }
        } catch (Exception e) {
          showSendingErrorToast(e.getMessage());
          e.printStackTrace();
        }
      }
    });
    if(ipAddress.length() > 0) {
      thread.start();
    }
  }

  private void showSendingErrorToast(final String msg) {
    hideNotification();
    showNativeToast("Unable to send files: " + msg);
  }

  private void initializeToolbar() {
    // Sets the Toolbar to act as the ActionBar for this Activity window.
    // Make sure the toolbar exists in the activity and is not null
    setSupportActionBar(toolbar);
    ActionBar actionBar = getSupportActionBar();
    if(isNotNull(actionBar)) {
      actionBar.setDisplayShowTitleEnabled(false);
    }
  }

  @OnClick(R.id.settings_image_button) void onSettingsClicked() {
    createSettingsDialog();
  }

  @OnClick(R.id.take_off_image_button) void onTakeOffClicked() {
    if(missionDetails.isMissionInProgress()) {
      DrogonApplication.getBus().post(new StopMissionClicked());
    }
    else {
      DrogonApplication.getBus().post(new TakeOffClicked());
      clearWaypointsImageBtn.setEnabled(false);
    }
  }

  @OnClick(R.id.clear_waypoints_image_button) void onClearWaypointsClicked() {
    DrogonApplication.getBus().post(new ClearWaypointsClicked());
    configureLeftSideButtons();
  }

  @OnClick(R.id.capture_image_btn) void onCaptureImageClicked() {
    DrogonApplication.getBus().post(new CaptureImageClicked());
  }

  @OnClick(R.id.border_layout) void onFragmentChange() {
    //put somewhere else
    dimens.setDimens(centerLayout.getWidth(), centerLayout.getHeight());

    final CustomLayoutParams cp = new CustomLayoutParams(subLayout);
    FillScreenAnimation fillScreenAnimation =
            new FillScreenAnimation(
                    subLayout,
                    mainLayout.getHeight(),
                    cp.height,
                    mainLayout.getWidth(),
                    cp.width);
    fillScreenAnimation.setAnimationListener(new Animation.AnimationListener() {
      @Override public void onAnimationStart(Animation animation) {
        isMapFragmentMain = !isMapFragmentMain;
        if(!isMapFragmentMain) settingsLayout.setVisibility(View.VISIBLE);
        DrogonApplication.getBus().post(new FragmentChange(isMapFragmentMain));
      }
      @Override public void onAnimationRepeat(Animation animation) {}
      @Override
      public void onAnimationEnd(Animation animation) {
        if(isMapFragmentMain) settingsLayout.setVisibility(View.INVISIBLE);
        switchViews(cp);
        borderLayout.setEnabled(true);
      }
    });
    fillScreenAnimation.setDuration(500);
    subLayout.startAnimation(fillScreenAnimation);
    borderLayout.setEnabled(false);
  }

  private void switchViews(CustomLayoutParams cp) {
    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(cp.width, cp.height);
    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
    params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
    mainLayout.setLayoutParams(params);
    mainLayout.setPadding(cp.paddingLeft, cp.paddingTop, cp.paddingRight, cp.paddingBottom);
    mainLayout.requestLayout();
    parentLayout.bringChildToFront(mainLayout);
    borderLayout.requestLayout();

    FrameLayout temp = subLayout;
    subLayout = mainLayout;
    mainLayout = temp;
  }

  @Subscribe
  public void onWaypointAdded(WaypointAdded m) {
    if(markers.size() <= 2) {
      showToast("Add more waypoints before take-off! " + markers.size());
    }
    configureLeftSideButtons();
  }

  private void configureLeftSideButtons() {
    int visibility = markers.size() >= 2 ? View.VISIBLE : View.GONE;
    clearWaypointsImageBtn.setVisibility(visibility);

    takeOffImageBtn.setEnabled(markers.size() > 2);
  }

  private void createSettingsDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    // Get the layout inflater
    LayoutInflater inflater = getLayoutInflater();
    View v = inflater.inflate(R.layout.dialog_settings, null);
    // Inflate and set the layout for the dialog
    // Pass null as the parent view because its going in the
    // dialog layout

    builder.setCancelable(false);
    builder.setView(v);
    final AlertDialog dialog = builder.create();
    ImageButton btn = (ImageButton)v.findViewById(R.id.close_btn);
    SeekBar seekBar = (SeekBar) v.findViewById(R.id.angle_seek_bar);
    final EditText editText = (EditText) v.findViewById(R.id.ip_address_edit_text);

    btn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        validateServerAddress(editText.getText().toString().trim());
        dialog.dismiss();
      }
    });

    String ipAddress = getPreferenceString(IP_ADDRESS_PREFERENCE).trim();
    if(ipAddress.length() > 0)
      editText.setText(ipAddress);

    int prefAngle = getPreferenceInt(DRONE_ANGLE_PREFERENCE);
    int angle = prefAngle >= 0 ? prefAngle : 100;
    seekBar.setProgress(angle);
    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        changeGimbal(i);
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {}
      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {}
    });
    dialog.show();
  }

  private void validateServerAddress(String ipAddress) {
    if(ipAddress.length() > 0) {
      try {
        URL u = new URL(ipAddress);
        String protocol  = u.getProtocol();
        String host = u.getHost();
        String path = u.getPath();
        if(path.charAt(path.length() - 1) == '/') {
          path = path.substring(0, path.length() - 1);
          u = new URL(protocol, host, path);
        }

        putPreferenceString(IP_ADDRESS_PREFERENCE, u.toString());
      } catch (MalformedURLException e) {
        showToast("Invalid URL");
      }
    } else removePreference(IP_ADDRESS_PREFERENCE);
  }

  private void changeGimbal(final int progress) {
    DJIGimbal gimbal = DrogonApplication.getGimbalInstance();
    if(isNotNull(gimbal)) {
      DJIParamCapability capability = gimbal.gimbalCapability.get(DJIGimbal.DJIGimbalCapabilityKey.PitchRangeExtension);
      boolean ifPossible = false;
      if(isNotNull(capability)) {
        ifPossible = capability.isSuppported();
      }

      if (ifPossible) {
        gimbal.setPitchRangeExtensionEnabled(true, new DJIBaseComponent.DJICompletionCallback() {
          @Override
          public void onResult(DJIError djiError) {}}
        );
      }

      gimbal.setGimbalWorkMode(DJIGimbal.DJIGimbalWorkMode.YawFollowMode, new DJIBaseComponent.DJICompletionCallback() {
        @Override
        public void onResult(DJIError error) {}
      });
      DJIGimbalAngleRotation mPitchRotation =
              new DJIGimbalAngleRotation(true, 0, DJIGimbal.DJIGimbalRotateDirection.Clockwise);
      DJIParamMinMaxCapability minMaxCapability =
              ((DJIParamMinMaxCapability)gimbal.gimbalCapability.get(DJIGimbal.DJIGimbalCapabilityKey.AdjustPitch));
      float min = minMaxCapability.getMin().floatValue();
      float max = minMaxCapability.getMax().floatValue();
      //-90 min, 30 max

      float multiplier = Math.abs(max) + Math.abs(min);
      float percent = progress / 100.00f;
      final float value = (percent * multiplier) + min;

      mPitchRotation.direction = DJIGimbal.DJIGimbalRotateDirection.Clockwise;
      mPitchRotation.angle = value;

      gimbal.rotateGimbalByAngle(DJIGimbalRotateAngleMode.AbsoluteAngle, mPitchRotation, null, null,
        new DJIBaseComponent.DJICompletionCallback() {
          @Override
          public void onResult(final DJIError djiError) {
            if(isNull(djiError)) {
              putPreferenceInt(DRONE_ANGLE_PREFERENCE, progress);
            }
          }
        }
      );
    }
  }

  public void showToast(final String message) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        showNotification(message);
        new Handler().postDelayed(new Runnable() {
          @Override
          public void run() {
            hideNotification();
          }
        }, 3500);
      }
    });
  }

  public void showNativeToast(final String message) {
    final Context c = this;
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(c, message, Toast.LENGTH_SHORT).show();
      }
    });
  }

  public void showNotification(final String message) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if(notificationLayout.getVisibility() == View.GONE) {
          ExpandCollapseAnimation enterAnimation = new ExpandCollapseAnimation(notificationLayout, notificationTextView, 1000, 0);
          notificationLayout.startAnimation(enterAnimation);
          notificationTextView.setText(message);
        }
      }
    });

  }

  public void hideNotification() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if(notificationLayout.getVisibility() == View.VISIBLE) {
          ExpandCollapseAnimation exitAnimation = new ExpandCollapseAnimation(notificationLayout, notificationTextView, 1000, 1);
          notificationLayout.startAnimation(exitAnimation);
        }
      }
    });
  }

  private void addFragmentToMain(Fragment main) {
    addFragment(R.id.main_fragment, main);
  }

  private void addFragmentToSub(Fragment sub) {
    addFragment(R.id.sub_fragment, sub);
  }

  private void addFragment(int id, Fragment f) {
    getSupportFragmentManager().beginTransaction().add(id, f).commit();
  }

  private void putPreferenceString(String key, String value) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    preferences.edit().putString(key, value).apply();
  }

  private void putPreferenceInt(String key, int value) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    preferences.edit().putInt(key, value).commit();
  }

  private String getPreferenceString(String key) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    return preferences.getString(key, "");
  }

  private int getPreferenceInt(String key) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    return preferences.getInt(key, -1);
  }

  private void removePreference(String key) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    preferences.edit().remove(key).apply();
  }

  public <T> boolean isNotNull(T i) {
    return i != null;
  }

  public <T> boolean isNull(T i) {
    return i == null;
  }

  @Override
  protected void onResume() {
    super.onResume();

    //Register BroadcastReceiver
    IntentFilter filter = new IntentFilter();
    filter.addAction(DrogonApplication.FLAG_CONNECTION_CHANGE);
    registerReceiver(onConnectionChangeReceiver, filter);

    DrogonApplication.getBus().register(this);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    unregisterReceiver(onConnectionChangeReceiver);
  }


  @Override
  protected void onPause() {
    super.onPause();
    DrogonApplication.getBus().unregister(this);
  }
}
