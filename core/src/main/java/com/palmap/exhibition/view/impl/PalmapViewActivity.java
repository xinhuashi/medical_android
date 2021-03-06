package com.palmap.exhibition.view.impl;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.palmap.exhibition.AndroidApplication;
import com.palmap.exhibition.R;
import com.palmap.exhibition.config.Config;
import com.palmap.exhibition.exception.NetWorkTypeException;
import com.palmap.exhibition.iflytek.IFlytekController;
import com.palmap.exhibition.launcher.LauncherModel;
import com.palmap.exhibition.listenetImpl.MapOnZoomListener;
import com.palmap.exhibition.model.ExFloorModel;
import com.palmap.exhibition.model.FacilityModel;
import com.palmap.exhibition.model.PoiModel;
import com.palmap.exhibition.model.SearchResultModel;
import com.palmap.exhibition.model.ServiceFacilityModel;
import com.palmap.exhibition.presenter.PalMapViewPresenter;
import com.palmap.exhibition.service.LampSiteLocationService;
import com.palmap.exhibition.view.FloorDataProvides;
import com.palmap.exhibition.view.PalMapView;
import com.palmap.exhibition.view.adapter.FacilitiesListAdapter;
import com.palmap.exhibition.view.adapter.FloorListAdapter;
import com.palmap.exhibition.view.base.ExActivity;
import com.palmap.exhibition.widget.CompassView;
import com.palmap.exhibition.widget.IPoiMenu;
import com.palmap.exhibition.widget.NavigationTipPanelView;
import com.palmap.exhibition.widget.PoiMenuLayout;
import com.palmap.exhibition.widget.Scale;
import com.palmap.exhibition.widget.ShadowLayout;
import com.palmap.exhibition.widget.StartEndPoiChooseView;
import com.palmap.library.utils.ActivityUtils;
import com.palmap.library.utils.DeviceUtils;
import com.palmap.library.utils.LogUtil;
import com.palmaplus.nagrand.core.Types;
import com.palmaplus.nagrand.core.Value;
import com.palmaplus.nagrand.data.Feature;
import com.palmaplus.nagrand.data.FeatureCollection;
import com.palmaplus.nagrand.data.LocationList;
import com.palmaplus.nagrand.data.LocationModel;
import com.palmaplus.nagrand.data.PlanarGraph;
import com.palmaplus.nagrand.position.PositioningManager;
import com.palmaplus.nagrand.view.MapView;
import com.palmaplus.nagrand.view.layer.FeatureLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import static com.palmap.exhibition.view.impl.PalmapViewState.END_SET;
import static com.palmap.exhibition.view.impl.PalmapViewState.Navigating;
import static com.palmap.exhibition.view.impl.PalmapViewState.Normal;
import static com.palmaplus.nagrand.navigate.DynamicNavigateAction.ACTION_ARRIVE;
import static com.palmaplus.nagrand.navigate.DynamicNavigateAction.ACTION_BACK_LEFT;
import static com.palmaplus.nagrand.navigate.DynamicNavigateAction.ACTION_BACK_RIGHT;
import static com.palmaplus.nagrand.navigate.DynamicNavigateAction.ACTION_FRONT_LEFT;
import static com.palmaplus.nagrand.navigate.DynamicNavigateAction.ACTION_FRONT_RIGHT;
import static com.palmaplus.nagrand.navigate.DynamicNavigateAction.ACTION_TURN_LEFT;
import static com.palmaplus.nagrand.navigate.DynamicNavigateAction.ACTION_TURN_RIGHT;

public class PalmapViewActivity extends ExActivity<PalMapViewPresenter> implements PalMapView, FloorDataProvides {

    private static final String KEY_LOCATION_ID = "key_location_id";
    private static final String KEY_LOCATION_TYPE = "key_location_type";
    private static final String KEY_FLOOR_ID = "key_floor_id";
    private static final String KEY_FEATURE_ID = "key_feature_id";
    private static final String KEY_MAP_ID = "key_map_id";
    private static final String KEY_SEARCH_TEXT = "key_searchText";
    private static final String KEY_TITLE_NAME = "key_title_name";
    private static final String KEY_IS_TRAVEL_PLANNING = "key_isTravelPlanning";
    private static final String KEY_SEARCH_LIST = "key_search_list";
    private static final int CODE_SEARCH_REQUEST = 1001;//搜索code

    @Inject
    PalMapViewPresenter presenter;
    @Inject
    PositioningManager beaconPositioningManager;
    @Inject
    ExecutorService mapViewDrawExecutor;

    MapView mapView;
    ListView facilitiesListView;
    ListView floorListView;
    CompassView compassView;
    ImageView mapLocation;
    Scale scaleView;
    ViewGroup layout_overlay;
    TextView tvTitle;
    RelativeLayout containerRetry;
    TextView tvLocationMessage;
    ViewGroup layout_mapView;
    View map_location;
    FloorListAdapter floorListAdapter;
    FacilitiesListAdapter facilitiesListAdapter;
    ViewGroup layout_zoom;
    ViewGroup layout_floor;
    FeatureLayer navigateLayer;
    StartEndPoiChooseView startEndPoiChooseView;
    NavigationTipPanelView navigationTipPanelView;
    ShadowLayout searchPanel;

    PoiMenuLayout poiMenuLayout;
    IPoiMenu poiMenu;

    /**
     * 是否添加了导航层
     */
    private boolean isAttachNavigateLayer = false;
    private ArrayList<ExFloorModel> floorModelList;

    private boolean usePDR = false;

    private PalmapViewActivity self;

    private static int LAUNCHER_INDEX = 0;

    public static void navigatorThis(Context that) {
        Intent intent = new Intent(that, PalmapViewActivity.class);
        that.startActivity(intent);
    }

    public static void navigatorThis(Context that, long mapId, long floorId, long featureId) {
        navigatorThis(that, null, mapId, floorId, featureId, false);
    }

    public static void navigatorThis(Context that, String title, long mapId, long floorId, long featureId, boolean isTravelPlanning) {
        Intent intent = new Intent(that, PalmapViewActivity.class);
        intent.putExtra(KEY_FLOOR_ID, floorId);
        intent.putExtra(KEY_FEATURE_ID, featureId);
        intent.putExtra(KEY_MAP_ID, mapId);
        intent.putExtra(KEY_TITLE_NAME, title);
        intent.putExtra(KEY_IS_TRAVEL_PLANNING, isTravelPlanning);
        that.startActivity(intent);
    }

    public static void navigatorThis(Context that, LauncherModel launcherModel) {
        if (launcherModel == null) return;
        AndroidApplication.getInstance().setLocationMapId(launcherModel.getMapId());
        navigatorThis(
                that,
                launcherModel.getTitle(),
                launcherModel.getMapId(),
                launcherModel.getFloorId(),
                launcherModel.getFeatureId(),
                launcherModel.isTravelPlanning()
        );
    }

    public static Intent getPoiSearchResultIntent(ArrayList<SearchResultModel> data) {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(KEY_SEARCH_LIST, data);
        intent.putExtras(bundle);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityUtils.noBackground(this);
        setContentView(R.layout.view_palmap);
        self = this;
        initView();
        //initStatusBar(R.color.ngr_colorPrimary);
        initMapView();
        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            final long floorId = intent.getExtras().getLong(KEY_FLOOR_ID, -1);
            final long featureId = intent.getExtras().getLong(KEY_FEATURE_ID, -1);
            final long mapId = intent.getExtras().getLong(KEY_MAP_ID, -1);
            final String title = intent.getExtras().getString(KEY_TITLE_NAME, null);
            tvTitle.setText(title);
            presenter.attachView(self, mapId, floorId, featureId);
        } else {
            presenter.attachView(self);
        }
    }

    private void initMapView() {
        LogUtil.e("LAUNCHER_INDEX :" + LAUNCHER_INDEX);
        mapView = new MapView("default" + LAUNCHER_INDEX % 7, getContext());
        mapView.start();
        layout_mapView.addView(mapView);
        mapView.setMinAngle(45);
        registerLister();
        LAUNCHER_INDEX++;
    }

    private void registerLister() {
        mapView.setOverlayContainer(layout_overlay);
        mapView.setMapOptions(presenter.configMap());

        mapView.setOnSingleTapListener(presenter);
        mapView.setOnChangePlanarGraph(presenter);
        mapView.setOnDoubleTapListener(presenter);
        mapView.setOnLongPressListener(presenter);
        mapView.setOnLoadStatusListener(presenter);

        mapView.setOnZoomListener(new MapOnZoomListener(scaleView, compassView));

    }

    protected PalMapViewPresenter inject() {
        daggerInject().inject(this);
        return presenter;
    }

    @Override
    public MapView getMapView() {
        return mapView;
    }

    @Override
    public void moveToLocationPoint() {
        if (presenter.getUserCoordinate() != null && presenter.getUserCoordinate().getZ() - presenter.getCurrentFloorId() == 0) {
            mapView.moveToPoint(presenter.getUserCoordinate());
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    mapView.getOverlayController().refresh();
                }
            }, 50);
        }
    }

    @Override
    public void readTitle(String title) {
        if (tvTitle.getText() != null) {
            return;
        }
        tvTitle.setText(title);
    }

    private float oldScale = 0;

    @Override
    public void readMapData(final PlanarGraph planarGraph) {
        resetCompass();
        mapView.initRotate(Config.MAP_ANGLE);
        oldScale = mapView.getCurrentScale();
        mapViewDrawExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mapView.drawPlanarGraph(planarGraph);
            }
        });
        scaleView.setMapView(mapView);
    }

    @Override
    public void resetCompass() {
        compassView.setRotateAngle(Config.MAP_ANGLE);
    }

    @Override
    public void refreshScaleView() {
        if (scaleView != null) scaleView.postInvalidate();

    }

    @Override
    public void refreshCompassView(long time) {
        if (compassView != null) {
            if (time > 0) {
                compassView.animRefresh(mapView, time);
            } else {
                compassView.setRotateAngle(-(float) mapView.getRotate());
            }
        }
    }

    @Override
    public void resetFeatureStyle(Feature feature) {
        if (feature == null) return;
        mapView.resetOriginStyle("Area", LocationModel.id.get(feature));
    }

    @Override
    public void readFeatureColor(Feature feature, int color) {
        mapView.setRenderableColor("Area", LocationModel.id.get(feature), color);
    }

    @Override
    public void showPoiMenu(final PoiModel poiModel, PalmapViewState state) {
        if (state == PalmapViewState.END_SET) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.ngr_progressDialogTitle))
                    .setMessage(getString(R.string.ngr_set_poi_as_start_or_not))
                    .setPositiveButton(R.string.ngr_dlg_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            presenter.startMark(poiModel);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(R.string.ngr_dlg_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            presenter.removeTapAndPoiMark();
                            presenter.resetFeature();
                            dialog.dismiss();
                        }
                    });
            builder.create().show();
            return;
        }
        if (poiModel == null) {
            poiMenu.refreshView(state);
        } else {
            poiMenu.refreshView(poiModel, state);
        }
        changePalmapViewWidget(PalmapViewState.Select);
    }

    @Override
    public void hidePoiMenuAtFloorChange() {
//        if (!layoutPoiMenu.layoutTipIsShow() && presenter.getState() != PalmapViewState.Navigating) {
//            hidePoiMenu();
//        }
    }

    @Override
    public void hidePoiMenu() {
        if (presenter.getState() != END_SET) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    poiMenuLayout.animHide();
                }
            });
        }
    }

    @Override
    public void hidePoiMenuTop() {
    }

    @Override
    public void showLocationMsgView(String title, String msg) {
    }

    @Override
    public int getWindowRotation() {
        return getWindowManager().getDefaultDisplay().getRotation();
    }

    @Override
    public void readNavigateRoad(long[] floorIds, FeatureCollection featureCollection) {
        attachNavigateLayer();
        navigateLayer.clearFeatures();
        navigateLayer.addFeatures(featureCollection);
    }

    @Override
    public void clearNavigateRoad() {
        if (navigateLayer != null && isAttachNavigateLayer) {
            navigateLayer.clearFeatures();
            detachNavigateLayer();
        }
    }

    @Override
    public void showLocationStatus(PositioningManager.LocationStatus status) {
        switch (status) {
            case START:
                showLocationMsgView(getString(R.string.ngr_msg_locationing));
                break;
            case ERROR:
                mapLocation.setSelected(false);
                showLocationMsgView(getString(R.string.ngr_msg_locationErr));
                break;
            case MOVE:
                mapLocation.setSelected(true);
            default:
                tvLocationMessage.setVisibility(View.GONE);
                break;
        }
    }

    private void showLocationMsgView(String msg) {
        tvLocationMessage.setText(msg);
        tvLocationMessage.setVisibility(View.VISIBLE);
        AnimationDrawable ellipsis =
                (AnimationDrawable) ContextCompat.getDrawable(getContext(), R.drawable.dancing_ellipsis);
        tvLocationMessage.setCompoundDrawablesWithIntrinsicBounds(null, null, ellipsis, null);
        ellipsis.start();

        handler.postDelayed(goneLocationMsgViewTask, 5000);
    }

    /**
     * 移除定位提示语
     */
    private Runnable goneLocationMsgViewTask = new Runnable() {
        @Override
        public void run() {
            if (tvLocationMessage != null) {
                tvLocationMessage.setVisibility(View.GONE);
            }
        }
    };

    @Override
    public void showLocationError(final Throwable throwable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (throwable instanceof NetWorkTypeException) {
                    LogUtil.e("非联通4G无法定位");
                    showLocationMsgView(getString(R.string.ngr_msg_locationErr));
                    mapLocation.setSelected(false);
                } else {
                    showLocationMsgView(getString(R.string.ngr_msg_locationErr));
                }
            }
        });
    }

    @Override
    public void readFloorData(LocationList locationList, long selectFloorId) {
        if ((locationList == null || locationList.getSize() == 0)) {
            if (floorListAdapter != null) {
                floorListAdapter.clear();
                floorListAdapter = null;
            }
            floorListView.setVisibility(View.GONE);
            return;
        }
        this.floorModelList = new ArrayList<>();
        for (int i = locationList.getSize() - 1; i >= 0; i--) {
            ExFloorModel temp = new ExFloorModel(locationList.getPOI(i));
            if (temp.isFloor()) {
                floorModelList.add(temp);
            }
        }
        if (floorListAdapter == null) {
            floorListAdapter = new FloorListAdapter(this, floorModelList, selectFloorId);
            if (locationList.getSize() < 3) {
                ViewGroup.LayoutParams layoutParams = floorListView.getLayoutParams();
                layoutParams.height = getResources().getDimensionPixelOffset(R.dimen.uiSize) *
                        locationList.getSize();
                floorListView.setLayoutParams(layoutParams);
            }
            layout_floor.setVisibility(View.VISIBLE);
            floorListView.setAdapter(floorListAdapter);
            floorListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                    ExFloorModel floorModel = floorListAdapter.getItem(position);
                    long nextFloorId = floorModel.getId();
                    if (nextFloorId == presenter.getCurrentFloorId()) {
                        return;
                    }
                    //TODO 准备切换楼层 先去除所有覆盖层
                    mapView.removeAllOverlay();
                    //TODO 自动手动切换了楼层 就取消自动切换楼层的支持了
                    presenter.setCanAutoChangeFloor(false);
                    LogUtil.d("changeFloor:" + nextFloorId);
                    presenter.changeFloor(nextFloorId);
                }
            });
        } else {
            floorListAdapter.replaceData(floorModelList, selectFloorId);
        }
    }

    @Override
    public void onFloorChanged(final long oldFloorId, final long newFloorId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (presenter.getState() == PalmapViewState.Select) {
                    poiMenuLayout.refreshView(Normal);
                    changePalmapViewWidget(Normal);
                }
                if (presenter.getState() == Navigating) {
                    mapView.zoom(oldScale);
                }
                clearFacilityListSelect();
                if (floorListAdapter != null) {
                    floorListAdapter.setSelectFloorId(newFloorId);
                    floorListView.smoothScrollToPosition(floorListAdapter.getSelectIndex());
                }
                if (oldFloorId != newFloorId) {
                    isAttachNavigateLayer = false;
                }
            }
        });
    }

    @Override
    public void visibilityOutSideView(boolean isShow) {
    }

    /**
     * 清除设施选中item
     */
    @Override
    public void clearFacilityListSelect() {
        if (facilitiesListAdapter != null) {
            facilitiesListAdapter.setSelectPosition(-1);
        }
    }

    @Override
    public boolean canUsePDR() {
        return usePDR;
    }

    /**
     * 显示剩余距离
     *
     * @param mDynamicNaviExplain
     * @param mRemainingLength
     */
    @Override
    public void readRemainingLength(final String mDynamicNaviExplain, final float mRemainingLength) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (layout_floor.getVisibility() == View.VISIBLE) {
                    changePalmapViewWidget(presenter.getState());
                }
                poiMenuLayout.refreshView(presenter.getState());
                poiMenuLayout.readRemainingLength(mDynamicNaviExplain, mRemainingLength);
            }
        });
    }

    @Override
    public void readPubFacility(final List<FacilityModel> data) {
        if (data.size() == 0) return;
        facilitiesListAdapter = new FacilitiesListAdapter(getContext(), data);
        facilitiesListView.setAdapter(facilitiesListAdapter);
        facilitiesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (facilitiesListAdapter.getSelectPosition() == position) {
                    presenter.clearFacilityMarks();
                    facilitiesListAdapter.setSelectPosition(-1);
                    return;
                }
                presenter.clearFacilityMarks();
                ArrayList<Long> ids = data.get(position).getIds();
                List<Feature> facilityModelCoordinates = new ArrayList<>();
                for (int i = 0; i < ids.size(); i++) {
                    List<Feature> tempResult = mapView.searchFeature("Area", "category", new Value(ids.get(i)));
                    if (tempResult != null && tempResult.size() > 0) {
                        facilityModelCoordinates.addAll(tempResult);
                    }
                }
                if (facilityModelCoordinates.size() == 0) {
                    showMessage(getString(R.string.ngr_msg_facility_none));
                    facilitiesListAdapter.setSelectPosition(-1);
                    return;
                } else {
                    facilitiesListAdapter.setSelectPosition(position);
                    presenter.addFacilityMarks(facilityModelCoordinates);
                }
            }
        });
    }

    @Override
    public void readServiceFacility(List<ServiceFacilityModel> data) {
    }

    void compassViewClick() {
        if (compassView.isSelected()) {
            mapView.rotateByNorth(new Types.Point(DeviceUtils.getWidth(this) / 2, DeviceUtils.getHeight(this) / 2), Config.MAP_ANGLE);
            compassView.setRotateAngle(Config.MAP_ANGLE);
        } else {
            mapView.rotateByNorth(new Types.Point(DeviceUtils.getWidth(this) / 2, DeviceUtils.getHeight(this) / 2), 0);
            compassView.reset();
        }
        delayedDoCollisionDetection();
        compassView.setSelected(!compassView.isSelected());
    }

    private void delayedDoCollisionDetection() {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mapView != null) {
                    mapView.doCollisionDetection();
                }
            }
        }, 50);
    }

    void locationClick() {
        presenter.startLocation(beaconPositioningManager);//实用lampsite点位 传null
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CODE_SEARCH_REQUEST && resultCode == RESULT_OK) {
            try {
                ArrayList<SearchResultModel> models =
                        data.getParcelableArrayListExtra(KEY_SEARCH_LIST);
                presenter.handlerSearchResult(models);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        IFlytekController.getInstance().destroyAllSpeechSynthesizer();
        if (mapView != null && !mapView.getPtr().isRelase()) {
            mapView.drop();
        }
        // TODO: 2016/7/24 恢复语言
        Resources resources = getResources();//获得res资源对象
        Configuration config = resources.getConfiguration();//获得设置对象
        DisplayMetrics dm = resources.getDisplayMetrics();//获得屏幕参数：主要是分辨率，像素等。
        config.locale = Config.oldLanguage;
        resources.updateConfiguration(config, dm);
        // TODO: 2016/10/26 移除隐藏定位提示语的任务 防止内存泄漏
        handler.removeCallbacks(goneLocationMsgViewTask);
        mapViewDrawExecutor.shutdown();
    }

    @Override
    public void showRetry(final Throwable throwable, final Runnable runnable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(getString(R.string.ngr_progressDialogTitle)).setMessage(getString(R.string.ngr_errorMsg));
                builder.setPositiveButton(getString(R.string.ngr_reload), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        runOnUiThread(runnable);
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        presenter.refreshData();
                    }
                });
                builder.create().show();
            }
        });
    }

    @Override
    public void showRetryDialog(final Throwable throwable, final Runnable runnable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(getString(R.string.ngr_progressDialogTitle)).setMessage(throwable.getMessage());
                builder.setPositiveButton(getString(R.string.ngr_reload), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        runOnUiThread(runnable);
                    }
                });
                builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        presenter.resetState();
                    }
                });
                builder.create().show();
            }
        });
    }

    @Override
    public void hideRetry() {
        super.hideRetry();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                containerRetry.removeAllViews();
                containerRetry.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public boolean isRetry() {
        return !(containerRetry.getChildCount() == 0);
    }

    @Override
    public boolean checkFloorIdOnCurrentBuilding(long floorId) {
        if (null == floorListAdapter) {
            return false;
        }
        return floorListAdapter.checkFloorId(floorId);
    }

    @Override
    public void resetFailityMenu() {
    }

    @Override
    public void hideMapViewControl() {
        floorListView.setVisibility(View.INVISIBLE);
        scaleView.setVisibility(View.INVISIBLE);
        map_location.setVisibility(View.INVISIBLE);
        layout_zoom.setVisibility(View.INVISIBLE);
        facilitiesListView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void showMapViewControl() {
        floorListView.setVisibility(View.VISIBLE);
        scaleView.setVisibility(View.VISIBLE);
        map_location.setVisibility(View.VISIBLE);
        layout_zoom.setVisibility(View.VISIBLE);
        facilitiesListView.setVisibility(View.VISIBLE);
    }

    /**
     * 显示起点信息
     *
     * @param lable 名称
     * @param msg   楼层 + 地址
     */
    @Override
    public void showRouteInfoStart(final String lable, final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startEndPoiChooseView.setStartPoiName(lable);
                startEndPoiChooseView.setStartPoiDes(msg);
            }
        });
    }

    /**
     * 显示终点信息
     *
     * @param label 名称
     * @param msg   楼层 + 地址
     */
    @Override
    public void showRouteInfoEnd(final String label, final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startEndPoiChooseView.setEndPoiName(label);
                startEndPoiChooseView.setEndPoiDes(msg);
                poiMenuLayout.refreshView(PalmapViewState.END_SET);
                changePalmapViewWidget(PalmapViewState.END_SET);
            }
        });
    }

    private void showStartEndPoiChoosePanel(final boolean isAnimated) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                searchPanel.setVisibility(View.GONE);
                startEndPoiChooseView.setVisibility(View.VISIBLE);
                if (isAnimated) {
                    startEndPoiChooseView.showDropAnimation();
                }
            }
        });
    }

    @Override
    public void hideStartEndPoiChoosePanel(final boolean isAnimated) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startEndPoiChooseView.setVisibility(View.GONE);
                startEndPoiChooseView.resetInfo();
                if (isAnimated) {
                    startEndPoiChooseView.showRiseAnimation();
                }
                searchPanel.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showNavigationTipPanel(final boolean isAnimated) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startEndPoiChooseView.setVisibility(View.GONE);
                startEndPoiChooseView.resetInfo();
                navigationTipPanelView.setVisibility(View.VISIBLE);
                if (isAnimated) {
                    navigationTipPanelView.showDropAnimation();
                }
            }
        });
    }

    private void hideNavigationTipPanel(final boolean isAnimated) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                navigationTipPanelView.setVisibility(View.GONE);
                if (isAnimated) {
                    navigationTipPanelView.showRiseAnimation();
                }
                navigationTipPanelView.resetInfo();
                searchPanel.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * 显示 路线信息 (直行xx米 左转)
     *
     * @param msg
     * @param mAction        动作{@link com.palmaplus.nagrand.navigate.DynamicNavigateAction}
     * @param startFloorName 起点的楼层名
     * @param endFloorName   终点的楼层名
     */
    @Override
    public void showRouteInfoDetails(
            final String msg, final int mAction, final String startFloorName, final String endFloorName) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (navigationTipPanelView.getVisibility() == View.GONE) {
                    showNavigationTipPanel(true);
                }
                int resId = R.mipmap.ic_nav_straight;
                switch (mAction) {
                    case ACTION_FRONT_LEFT:
                    case ACTION_TURN_LEFT:
                    case ACTION_BACK_LEFT: {
                        resId = R.mipmap.ic_nav_left;
                        break;
                    }
                    case ACTION_FRONT_RIGHT:
                    case ACTION_TURN_RIGHT:
                    case ACTION_BACK_RIGHT: {
                        resId = R.mipmap.ic_nav_right;
                        break;
                    }
                    case ACTION_ARRIVE: {
                        resId = 0;
                    }
                    default:
                        break;
                }
                navigationTipPanelView.setSignIcon(resId);
                navigationTipPanelView.setNavigationTip(msg);
                navigationTipPanelView.setTvCurrentPosition(getCurrentFloorName());
                navigationTipPanelView.setTargetPosition(endFloorName);
            }
        });
    }

    @Override
    public void hideRouteInfoView() {
    }

    @Override
    public void showRouteUpView() {
    }

    @Override
    public void showRouteDownView() {
    }

    @Override
    public void hideRouteArrowView() {
    }

    @Override
    public String getCurrentFloorName() {
        if (floorListAdapter != null && floorModelList != null) {
            long selectFloorId = floorListAdapter.getSelectFloorId();
            for (ExFloorModel exFloorModel : floorModelList) {
                if (exFloorModel.getId() == selectFloorId) {
                    return exFloorModel.getName();
                }
            }
        }
        return null;
    }

    /**
     * 显示导航距离
     *
     * @param msg
     */
    @Override
    public void showRouteLength(String msg) {
        poiMenuLayout.showRouteInfoDetails(msg);
    }

    /**
     * 附加上导航层
     */
    private void attachNavigateLayer() {
        if (isAttachNavigateLayer) return;
        isAttachNavigateLayer = true;
        //定位层
        navigateLayer = new FeatureLayer("navigate");
        mapView.addLayer(navigateLayer);
        mapView.setLayerOffset(navigateLayer);
    }

    private void detachNavigateLayer() {
        if (navigateLayer != null) {
            mapView.removeLayer(navigateLayer);
        }
        isAttachNavigateLayer = false;
    }

    private void initView() {
        floorListView = findView(R.id.floorListView);
        compassView = findView(R.id.compassView);
        compassView.setAlpha(.9f);

        mapLocation = findView(R.id.map_location);
        facilitiesListView = findView(R.id.list_facilities);
        scaleView = findView(R.id.scale);
        layout_overlay = findView(R.id.layout_overlay);
        tvTitle = findView(R.id.tvTitle);
        containerRetry = findView(R.id.container_retry);
        tvLocationMessage = findView(R.id.tv_locationMessage);
        layout_mapView = findView(R.id.layout_mapView);
        layout_floor = (ViewGroup) findViewById(R.id.layout_floor);

        map_location = findView(R.id.map_location);
        layout_zoom = findView(R.id.layout_zoom);

        searchPanel = findView(R.id.layout_search);
        startEndPoiChooseView = findView(R.id.startEndPoiChooseView);
        navigationTipPanelView = findView(R.id.navigationTipPanelView);

        compassView.setOnClickListener(this);
        mapLocation.setOnClickListener(this);

//        tvTitle.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View v) {
//                usePDR = !usePDR;
//                if (usePDR) {
//                    showMessage("添加惯导");
//                } else {
//                    showMessage("取消惯导");
//                }
//                return true;
//            }
//        });

        startEndPoiChooseView.setOnBackClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goBack();
            }
        });

        Switch switch_test_location = findView(R.id.switch_test_location);
        switch_test_location.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Config.isTestLocation = isChecked;
                startService(new Intent(self, LampSiteLocationService.class));
            }
        });
        initLayoutPoiMenu();
    }

    /**
     * 延迟刷新地图覆盖物
     */
    private void postRefreshOverlayer() {
        postRefreshOverlayer(50);
    }

    private void postRefreshOverlayer(long time) {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                mapView.getOverlayController().refresh();
            }
        }, time);
    }

    private void showPoiMark(long locationId) {
        presenter.refreshPoiMark(locationId);
        showPoiMenu(new PoiModel(getMapView().selectFeature(locationId)), PalmapViewState.Select);
    }

    private void initLayoutPoiMenu() {
        poiMenuLayout = (PoiMenuLayout) findViewById(R.id.layout_poiMenu2);
        poiMenu = poiMenuLayout;

        poiMenuLayout.setViewHandler(new PoiMenuLayout.ViewHandler() {
            @Override
            public void onGoHereClick() {
                if (presenter.startMarkFromLocation()) {
                    presenter.endMark(poiMenuLayout.getPoiModel());
                } else {
                    //showMessage("当前没有定位点");
                    //presenter.setPalmapViewState(PalmapViewState.END_SET);
                    presenter.endMark(poiMenuLayout.getPoiModel());
                }
            }

            @Override
            public void onMockNaviClick() {
                presenter.beginMockNavigate();
            }

            @Override
            public void onStartNaviClick() {
                if (presenter.getUserCoordinate() == null) {
                    showMessage(getString(R.string.ngr_cannot_find_location_tip));
                } else {
                    presenter.beginNavigate();
                }
            }

            @Override
            public void onExitNaviClick(boolean isInterrupt) {
                if (isInterrupt) {
                    showExitNavigationTipDialog();
                } else {
                    presenter.exitNavigate();
                    changePalmapViewWidget(Normal);
                }
            }

            @Override
            public void onSearchGoHereClick() {
                PoiModel poiModel = poiMenuLayout.getPoiModel();
                presenter.changeFloorAddMark((long) poiModel.getZ(), poiModel.getId(), true);
            }

            @Override
            public void onSearchItemClick() {
                PoiModel poiModel = poiMenuLayout.getPoiModel();
                presenter.changeFloorAddMark((long) poiModel.getZ(), poiModel.getId(), false);
            }

        });
    }

    @Override
    public void readExitNavigate() {
        hideNavigationTipPanel(true);
    }

    @Override
    public void readNaviComplete() {
        navigationTipPanelView.setNavigationTip(getString(R.string.ngr_arrive_destination));
        poiMenuLayout.refreshView(presenter.getState());
        changePalmapViewWidget(presenter.getState());
    }

    @Override
    public void showSearchResultView(List<PoiModel> poiModels) {
        poiMenu.refreshView(poiModels, PalmapViewState.Search);
        changePalmapViewWidget(PalmapViewState.Search);
        if (!poiModels.isEmpty()) {
            PoiModel poiModel = poiModels.get(0);
            if (poiModel != null) {
                presenter.changeFloorAddMark((long) poiModel.getZ(), poiModel.getId(), false);
            }
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.map_location) {
            locationClick();
        } else if (i == R.id.compassView) {
            compassViewClick();
        }
    }

    private void changeFloor(int floorId) {
        if (floorId == presenter.getCurrentFloorId()) {
            return;
        }
        //TODO 准备切换楼层 先去除所有覆盖层
        mapView.removeAllOverlay();
        //TODO 自动手动切换了楼层 就取消自动切换楼层的支持了
        presenter.setCanAutoChangeFloor(false);
        presenter.changeFloor(floorId);
    }

    @Override
    public void showMessage(final String message) {
        showAlertMsg(message, null);
    }

    @Override
    public void showErrorMessage(String message) {
        showAlertMsg(message, null);
    }

    public void showAlertMsg(final String msg, final String subMsg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                toastDelegate.get().showMsg(msg);
            }
        });
    }

    @Override
    public List<ExFloorModel> floorModels() {
        return floorModelList;
    }

    @Override
    public String getFloorNameById(long floorId) {
        if (floorModelList == null || floorModelList.size() == 0) {
            return null;
        }
        for (ExFloorModel m : floorModelList) {
            if (m.getId() == floorId) return m.getName();
        }
        return null;
    }

    public void goToSearch(View view) {
        getNavigator().toSearchViewForResult(
                this, presenter.getBuildingId(), floorModelList, CODE_SEARCH_REQUEST);
    }

    public void mapZoomInClick(View view) {
        if (mapView != null) {
            mapView.zoomIn();
            delayedDoCollisionDetection();
            refreshScaleView();
        }
    }

    public void mapZoomOutClick(View view) {
        if (mapView != null) {
            mapView.zoomOut();
            delayedDoCollisionDetection();
            refreshScaleView();
        }
    }

    //菜单栏返回按钮点击
    public void onBackClick(View view) {
        goBack();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            goBack();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private void goBack() {
        switch (presenter.getState()) {
            case Search:
            case END_SET:
            case RoutePlanning:
            case NaviComplete: {
                presenter.resetState();
                changePalmapViewWidget(Normal);
                break;
            }
            case Navigating: {
                showExitNavigationTipDialog();
                break;
            }
            case Normal:
            case Select:
            default:
                onBackPressed();
                break;
        }
    }

    private void changePalmapViewWidget(PalmapViewState state) {
        switch (state) {
            case Normal: {
                searchPanel.setVisibility(View.VISIBLE);
                layout_floor.setVisibility(View.VISIBLE);
                hideNavigationTipPanel(true);
                break;
            }
            case Select: {
                // TODO: 2017/7/3 是否需要隐藏
//                searchPanel.setVisibility(View.GONE);
                break;
            }
            case Search: {
                searchPanel.setVisibility(View.GONE);
                layout_floor.setVisibility(View.GONE);
                break;
            }
            case END_SET: {
                showStartEndPoiChoosePanel(true);
                layout_floor.setVisibility(View.VISIBLE);
                break;
            }
            case RoutePlanning: {
                break;
            }
            case Navigating: {
                layout_floor.setVisibility(View.GONE);
                break;
            }
            case NaviComplete: {
                break;
            }
            default:
                break;
        }
    }

    private void showExitNavigationTipDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.ngr_progressDialogTitle))
                .setMessage(getString(R.string.ngr_exit_navigation_or_not))
                .setPositiveButton(R.string.ngr_dlg_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        presenter.exitNavigate();
                        changePalmapViewWidget(Normal);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.ngr_dlg_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }


}