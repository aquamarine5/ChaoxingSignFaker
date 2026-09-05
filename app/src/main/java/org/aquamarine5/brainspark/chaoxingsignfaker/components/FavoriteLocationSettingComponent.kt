/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import android.Manifest
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.baidu.mapapi.SDKInitializer
import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.BaiduMapOptions
import com.baidu.mapapi.map.BitmapDescriptorFactory
import com.baidu.mapapi.map.MapPoi
import com.baidu.mapapi.map.MapStatus
import com.baidu.mapapi.map.MapStatusUpdateFactory
import com.baidu.mapapi.map.MapView
import com.baidu.mapapi.map.Marker
import com.baidu.mapapi.map.MyLocationData
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.search.core.SearchResult
import com.baidu.mapapi.search.geocode.GeoCodeResult
import com.baidu.mapapi.search.geocode.GeoCoder
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingLocation
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.MARKER_BUNDLE_ADDRESS
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.MARKER_BUNDLE_LABEL
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.MARKER_BUNDLE_TYPE
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.MarkerBundleType
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.MARKER_TITLE_VISIBLE_ZOOM
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.addFavoriteLocationMarker
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.addOrUpdateLocationMarker
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.displaySnackbar
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.updateMarkerTitlesVisibility

@Serializable
object FavoriteLocationSettingDestination

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FavoriteLocationSettingComponent(modifier: Modifier = Modifier) {
    val hapticFeedback = LocalHapticFeedback.current
    val snackbarHost = LocalSnackbarHostState.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val locationPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    var isShowFavoriteLocationDialog by remember { mutableStateOf(false) }
    val favoriteLocations = remember { mutableStateListOf<ChaoxingLocation>() }
    val favoriteLocationMarkers = remember { mutableListOf<Marker>() }
    var isMarkerTitleVisible = remember { true }
    var isNeedLocationDescribe = remember { false }
    var clickedPosition by remember { mutableStateOf(LatLng(0.0, 0.0)) }
    var clickedName by remember { mutableStateOf("未指定") }
    var clickedLabel by remember { mutableStateOf<String?>(null) }
    val geoCoder = remember {
        GeoCoder.newInstance().apply {
            setOnGetGeoCodeResultListener(object : OnGetGeoCoderResultListener {
                override fun onGetGeoCodeResult(p0: GeoCodeResult?) {}

                override fun onGetReverseGeoCodeResult(p0: ReverseGeoCodeResult?) {
                    if (p0 == null || p0.error != SearchResult.ERRORNO.NO_ERROR) {
                        Log.w(
                            "GetLocationPage",
                            "ReverseGeoCodeResult error: ${p0?.error}"
                        )
                        return
                    }
                    if (isNeedLocationDescribe) {
                        clickedName = p0.address
                        isNeedLocationDescribe = false
                    } else {
                        clickedName = p0.address
                        clickedLabel = p0.poiList?.firstOrNull()?.name?.takeIf { it.isNotBlank() }
                            ?: "自定义位置"
                    }
                }
            })
        }
    }
    if (locationPermissionsState.allPermissionsGranted) {
        val isInitialized = remember { SDKInitializer.isInitialized() }
        if (!isInitialized) {
            SDKInitializer.initialize(context.applicationContext)
        }
        val locationClient = remember {
            LocationClient(context).apply {
                locOption = LocationClientOption().apply {
                    setCoorType("bd09ll")
                    setFirstLocType(LocationClientOption.FirstLocType.SPEED_IN_FIRST_LOC)
                    setIsNeedAddress(true)
                    setNeedNewVersionRgc(true)
                }
            }
        }
        var mapType = remember { BaiduMap.MAP_TYPE_NORMAL }
        var clickedMarker by remember { mutableStateOf<Marker?>(null) }
        val clickPointBitmap =
            remember { BitmapDescriptorFactory.fromResource(R.drawable.ic_geo_alt_fill) }
        val starBitmap = remember { BitmapDescriptorFactory.fromResource(R.drawable.ic_map_star) }
        val baiduMap = remember {
            MapView(context, BaiduMapOptions().apply {
                zoomControlsEnabled(false)
                scaleControlEnabled(false)
                compassEnabled(false)
                rotateGesturesEnabled(false)
            }).apply {
                val setMarkerPositionOrCreate = { position: LatLng ->
                    clickedMarker = map.addOrUpdateLocationMarker(
                        clickedMarker,
                        position,
                        clickPointBitmap
                    )
                }
                isClickable = true
                map.setMapStatus(
                    MapStatusUpdateFactory.newMapStatus(
                        MapStatus.Builder()
                            .zoom(18f)
                            .build()
                    )
                )
                map.isMyLocationEnabled = true
                locationClient.registerLocationListener(object :
                    BDAbstractLocationListener() {
                    override fun onReceiveLocation(location: BDLocation?) {
                        Log.d("GetLocationPage", "onReceiveLocation: $location")
                        location?.let {
                            locationClient.stop()
                            map.setMyLocationData(
                                MyLocationData.Builder()
                                    .accuracy(it.radius)
                                    .direction(it.direction)
                                    .latitude(it.latitude)
                                    .longitude(it.longitude)
                                    .build()
                            )

                            if (clickedName == "未指定") {
                                map.setMapStatus(
                                    MapStatusUpdateFactory.newLatLng(
                                        LatLng(
                                            it.latitude,
                                            it.longitude
                                        )
                                    )
                                )
                                clickedPosition = LatLng(it.latitude, it.longitude)
                                clickedName = it.addrStr?.removePrefix("中国") ?: ""
                                clickedLabel = null
                            } else {
                                map.animateMapStatus(
                                    MapStatusUpdateFactory.newLatLngZoom(
                                        LatLng(
                                            it.latitude,
                                            it.longitude
                                        ), 18f
                                    ), 1000
                                )
                            }
                        }
                    }
                })
                map.setOnMapClickListener(object : BaiduMap.OnMapClickListener {
                    override fun onMapClick(p0: LatLng?) {
                        p0?.let {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                            clickedPosition = it
                            clickedLabel = null
                            geoCoder.reverseGeoCode(
                                ReverseGeoCodeOption()
                                    .location(it)
                                    .newVersion(1)
                                    .radius(500)
                            )
                            setMarkerPositionOrCreate(it)
                        }
                    }

                    override fun onMapPoiClick(p0: MapPoi?) {
                        p0?.let {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                            clickedPosition = it.position
                            clickedName = it.name
                            clickedLabel = it.name
                            isNeedLocationDescribe = true
                            geoCoder.reverseGeoCode(
                                ReverseGeoCodeOption()
                                    .location(it.position)
                                    .newVersion(1)
                                    .pageSize(2)
                                    .radius(500)
                            )
                            setMarkerPositionOrCreate(it.position)
                        }
                    }
                })
                map.setOnMarkerClickListener { p0 ->
                    p0?.let { favoriteMarker ->
                        favoriteMarker.extraInfo.let {
                            if (it.getString(MARKER_BUNDLE_TYPE) != MarkerBundleType.FAVORITE.toString()) {
                                return@setOnMarkerClickListener true
                            }
                            clickedPosition = favoriteMarker.position
                            clickedLabel = it.getString(MARKER_BUNDLE_LABEL)
                            clickedName =
                                it.getString(MARKER_BUNDLE_ADDRESS) ?: run {
                                    isNeedLocationDescribe = true
                                    geoCoder.reverseGeoCode(
                                        ReverseGeoCodeOption()
                                            .location(clickedPosition)
                                            .newVersion(1)
                                            .pageSize(2)
                                            .radius(500)
                                    )
                                    "加载中..."
                                }
                            setMarkerPositionOrCreate(clickedPosition)
                        }
                    }
                    true
                }
                map.setOnMarkerDragListener(object : BaiduMap.OnMarkerDragListener {
                    override fun onMarkerDrag(p0: Marker?) {}

                        override fun onMarkerDragEnd(p0: Marker?) {
                            Log.d("GetLocationPage", "onMarkerDragEnd: $p0")
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                            p0?.let {
                                clickedPosition = it.position
                                clickedLabel = null
                                geoCoder.reverseGeoCode(
                                ReverseGeoCodeOption()
                                    .location(it.position)
                                    .newVersion(1)
                                    .radius(500)
                            )
                        }
                    }

                    override fun onMarkerDragStart(p0: Marker?) {}
                })
                map.setOnMapStatusChangeListener(object :
                    BaiduMap.OnMapStatusChangeListener {
                    override fun onMapStatusChangeStart(p0: MapStatus?) {}

                    override fun onMapStatusChangeStart(p0: MapStatus?, p1: Int) {}

                    override fun onMapStatusChange(p0: MapStatus?) {}

                    override fun onMapStatusChangeFinish(p0: MapStatus?) {
                        val isTitleVisible =
                            (p0?.zoom ?: 0f) >= MARKER_TITLE_VISIBLE_ZOOM
                        if (isMarkerTitleVisible != isTitleVisible) {
                            isMarkerTitleVisible = isTitleVisible
                            map.updateMarkerTitlesVisibility(
                                favoriteLocationMarkers,
                                null,
                                isTitleVisible
                            )
                        }
                    }
                })
            }
        }
        LaunchedEffect(Unit) {
            locationClient.start()
            context.chaoxingDataStore.data.first().let { data ->
                favoriteLocations.addAll(data.locationsList)
                favoriteLocations.forEach {
                    favoriteLocationMarkers.add(
                        baiduMap.map.addFavoriteLocationMarker(it, starBitmap)
                    )
                }
            }
            baiduMap.map.updateMarkerTitlesVisibility(
                favoriteLocationMarkers,
                null,
                isMarkerTitleVisible
            )
        }
        if (isShowFavoriteLocationDialog) {
            FavoriteLocationSettingDialog(
                favoriteLocations,
                onDismiss = {
                    isShowFavoriteLocationDialog = false
                },
                onSelectLocation = { target ->
                    LatLng(target.latitude, target.longitude).let { position ->
                        clickedPosition = position
                        clickedName = target.address
                        clickedLabel = target.label
                        clickedMarker = baiduMap.map.addOrUpdateLocationMarker(
                            clickedMarker,
                            position,
                            clickPointBitmap
                        )
                        baiduMap.map.animateMapStatus(
                            MapStatusUpdateFactory.newLatLngZoom(position, 18f)
                        )
                    }
                },
                onDeleteLocation = { target ->
                    removeFavoriteLocation(
                        context,
                        coroutineScope,
                        target,
                        favoriteLocations,
                        favoriteLocationMarkers
                    )
                },
                favoriteLocationMarkers = favoriteLocationMarkers,
                selectedLocation = favoriteLocations.find {
                    it.latitude == clickedPosition.latitude && it.longitude == clickedPosition.longitude
                }
            )
        }
        Column(modifier = modifier) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(6.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "经度: ${
                            "%.5f".format(clickedPosition.longitude)
                        }, 纬度: ${
                            "%.5f".format(
                                clickedPosition.latitude
                            )
                        }",
                        maxLines = 1
                    )
                    Text(
                        "位置: $clickedName",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .zIndex(1f)
                        .padding(22.dp)
                ) {
                    val satelliteTooltipState = rememberTooltipState(isPersistent = true)
                    LaunchedEffect(Unit) {
                        context.chaoxingDataStore.data.first().let {
                            if (it.learntTooltips.mapSupportNormalSatelliteSwitch.not())
                                satelliteTooltipState.show()
                        }
                    }

                    FloatingActionButton(onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        isShowFavoriteLocationDialog = true
                    }) {
                        Icon(
                            painterResource(R.drawable.ic_map_pinned),
                            contentDescription = "管理收藏的签到位置"
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    FloatingActionButton(onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        if (clickedName == "未指定") {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
                            snackbarHost.displaySnackbar("请先点击地图选择要收藏的位置", coroutineScope)
                            return@FloatingActionButton
                        }
                        if (favoriteLocations.any {
                                it.latitude == clickedPosition.latitude && it.longitude == clickedPosition.longitude
                            }) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
                            snackbarHost.displaySnackbar("该位置已经收藏过了", coroutineScope)
                            return@FloatingActionButton
                        }
                        val newFavoriteLocation = ChaoxingLocation.newBuilder()
                            .setAddress(clickedName)
                            .setLabel(clickedLabel ?: clickedName)
                            .setLatitude(clickedPosition.latitude)
                            .setLongitude(clickedPosition.longitude)
                            .build()
                        coroutineScope.launch(Dispatchers.IO) {
                            context.chaoxingDataStore.updateData {
                                it.toBuilder().addLocations(newFavoriteLocation).build()
                            }
                        }
                        favoriteLocations.add(newFavoriteLocation)
                        favoriteLocationMarkers.add(
                            baiduMap.map.addFavoriteLocationMarker(newFavoriteLocation, starBitmap)
                        )
                        baiduMap.map.updateMarkerTitlesVisibility(
                            favoriteLocationMarkers,
                            null,
                            isMarkerTitleVisible
                        )
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                        snackbarHost.displaySnackbar("已添加收藏位置：$clickedName", coroutineScope)
                    }) {
                        Icon(
                            painterResource(R.drawable.ic_map_pin_check_inside),
                            contentDescription = "添加收藏位置"
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TooltipBox(
                        onDismissRequest = {},
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Start,
                            spacingBetweenTooltipAndAnchor = 12.dp
                        ),
                        hasAction = true,
                        tooltip = {
                            RichTooltip(
                                maxWidth = 200.dp, caretShape = TooltipDefaults.caretShape(
                                    DpSize(14.dp, 7.dp)
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(2.dp, 6.dp, 0.dp, 6.dp)
                                ) {
                                    Text(
                                        "现在可以点击按钮来切换平面地图/卫星地图了。",
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                            satelliteTooltipState.dismiss()
                                            coroutineScope.launch(Dispatchers.IO) {
                                                context.chaoxingDataStore.updateData {
                                                    it.toBuilder().setLearntTooltips(
                                                        it.learntTooltips.toBuilder()
                                                            .setMapSupportNormalSatelliteSwitch(
                                                                true
                                                            ).build()
                                                    ).build()
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.ic_x),
                                            contentDescription = "关闭提示"
                                        )
                                    }
                                }

                            }
                        },
                        state = satelliteTooltipState,
                    ) {
                        FloatingActionButton(onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                            mapType = if (mapType == BaiduMap.MAP_TYPE_NORMAL) {
                                BaiduMap.MAP_TYPE_SATELLITE
                            } else {
                                BaiduMap.MAP_TYPE_NORMAL
                            }
                            baiduMap.map.mapType = mapType
                        }) {
                            Icon(
                                painterResource(R.drawable.ic_map),
                                contentDescription = null
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FloatingActionButton(onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        locationClient.start()
                    }) {
                        Icon(
                            painterResource(R.drawable.ic_locate_fixed),
                            contentDescription = "定位"
                        )
                    }
                }
                AndroidView(
                    factory = {
                        baiduMap
                    }, modifier = Modifier.zIndex(0f), onRelease = {
                        runCatching {
                            it.onDestroy()
                            it.map.isMyLocationEnabled = false
                        }
                        it.removeAllViews()
                        locationClient.stop()
                        geoCoder.destroy()
                    }, onReset = {
                        it.onResume()
                    }
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("需要位置权限才能设置收藏的签到位置。")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                locationPermissionsState.launchMultiplePermissionRequest()
            }) {
                Text("授予位置权限")
            }
        }
    }
}