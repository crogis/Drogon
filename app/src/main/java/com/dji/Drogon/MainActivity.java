package com.dji.Drogon;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dji.Drogon.anim.ExpandCollapseAnimation;
import com.dji.Drogon.anim.FillScreenAnimation;
import com.dji.Drogon.event.ClearWaypointsClicked;
import com.dji.Drogon.event.FragmentChange;
import com.dji.Drogon.event.TakeOffClicked;
import com.dji.Drogon.event.WaypointAdded;
import com.dji.Drogon.views.CustomLayoutParams;
import com.dji.Drogon.fragment.CameraFragment;
import com.dji.Drogon.fragment.MapFragment;
import com.squareup.otto.Subscribe;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

  @BindView(R.id.main_fragment) FrameLayout mainLayout;
  @BindView(R.id.sub_fragment) FrameLayout subLayout;
  @BindView(R.id.border_layout) FrameLayout borderLayout;

  @BindView(R.id.parent_fragment_layout) RelativeLayout parentLayout;
  @BindView(R.id.settings_layout) RelativeLayout settingsLayout;

  @BindView(R.id.settings_image_button) ImageButton settingsImageBtn;
  @BindView(R.id.take_off_image_button) ImageButton takeOffImageBtn;
  @BindView(R.id.go_home_image_button) ImageButton goHomeImageBtn;
  @BindView(R.id.clear_waypoints_image_button) ImageButton clearWaypointsImageBtn;

  @BindView(R.id.toolbar) Toolbar toolbar;

  @BindView(R.id.dropdown_notification_layout) LinearLayout notificationLayout;
  @BindView(R.id.notification_text_view) TextView notificationTextView;

  Boolean isMapFragmentMain = true;

  WaypointMarkers markers = WaypointMarkers.getInstance();

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
    goHomeImageBtn.setEnabled(false);

    Fragment cameraFragment = new CameraFragment();
    Fragment mapFragment = new MapFragment();
    addFragmentToMain(mapFragment);
    addFragmentToSub(cameraFragment);
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
    DrogonApplication.getBus().post(new TakeOffClicked());
    clearWaypointsImageBtn.setEnabled(false);
  }

  @OnClick(R.id.clear_waypoints_image_button) void onClearWaypointsClicked() {
    DrogonApplication.getBus().post(new ClearWaypointsClicked());
    configureLeftSideButtons();
  }

  @OnClick(R.id.border_layout) void onFragmentChange() {
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
    System.out.println("SIZE === " + markers.size());

    if(markers.size() <= 1) {
      showToast("Add more waypoints before take-off!");
    }
    configureLeftSideButtons();
  }

  private void configureLeftSideButtons() {
    int visibility = markers.size() >= 2 ? View.VISIBLE : View.GONE;
    clearWaypointsImageBtn.setVisibility(visibility);

    takeOffImageBtn.setEnabled(markers.size() >= 2);
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
    btn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        dialog.dismiss();
      }
    });
    dialog.show();
  }

  public void showToast(String message) {
    if(notificationLayout.getVisibility() == View.GONE) {
      showNotification(message);
      new Handler().postDelayed(new Runnable() {
        @Override
        public void run() {
          hideNotification();
        }
      }, 3500);
    }
  }

  public void showNotification(String message) {
    if(notificationLayout.getVisibility() == View.GONE) {
      ExpandCollapseAnimation enterAnimation = new ExpandCollapseAnimation(notificationLayout, notificationTextView, 1000, 0);
      notificationLayout.startAnimation(enterAnimation);
      notificationTextView.setText(message);
    }
  }

  public void hideNotification() {
    if(notificationLayout.getVisibility() == View.VISIBLE) {
      ExpandCollapseAnimation exitAnimation = new ExpandCollapseAnimation(notificationLayout, notificationTextView, 1000, 1);
      notificationLayout.startAnimation(exitAnimation);
    }
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

  public <T> boolean isNotNull(T i) {
    return i != null;
  }

  public <T> boolean isNull(T i) {
    return i == null;
  }

  @Override
  protected void onResume() {
    super.onResume();
    DrogonApplication.getBus().register(this);
  }

  @Override
  protected void onPause() {
    super.onPause();
    DrogonApplication.getBus().unregister(this);
  }
}
