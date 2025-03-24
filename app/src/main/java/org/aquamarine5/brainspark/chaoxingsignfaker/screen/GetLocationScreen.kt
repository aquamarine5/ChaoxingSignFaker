/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import android.graphics.Color
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.baidu.mapapi.SDKInitializer
import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.BaiduMapOptions
import com.baidu.mapapi.map.BitmapDescriptorFactory
import com.baidu.mapapi.map.CircleOptions
import com.baidu.mapapi.map.MapPoi
import com.baidu.mapapi.map.MapStatus
import com.baidu.mapapi.map.MapStatusUpdateFactory
import com.baidu.mapapi.map.Marker
import com.baidu.mapapi.map.MarkerOptions
import com.baidu.mapapi.map.MyLocationData
import com.baidu.mapapi.map.TextureMapView
import com.baidu.mapapi.model.CoordUtil
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.search.core.SearchResult
import com.baidu.mapapi.search.geocode.GeoCodeResult
import com.baidu.mapapi.search.geocode.GeoCoder
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.UMengHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CenterCircularProgressIndicator
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingLocationSignEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingLocationSigner


@Serializable
data class GetLocationDestination(
    val activeId: Long,
    val classId: Int,
    val courseId: Int,
    val extContent: String
) {
    companion object {
        fun parseFromSignActivityEntity(activityEntity: ChaoxingSignActivityEntity): GetLocationDestination {
            return GetLocationDestination(
                activityEntity.id,
                activityEntity.course.classId,
                activityEntity.course.courseId,
                activityEntity.ext
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun GetLocationPage(
    destination: GetLocationDestination,
    navToCourseDetailDestination: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    LocalContext.current.let { context ->
        ChaoxingHttpClient.CheckInstance()
        Log.d("GetLocationPage", "GetLocationPage: $destination")
        val locationClient by remember {
            mutableStateOf(LocationClient(context.applicationContext).apply {
                locOption = LocationClientOption().apply {
                    setCoorType("bd09ll")
                    setFirstLocType(LocationClientOption.FirstLocType.SPEED_IN_FIRST_LOC)
                    locationMode = LocationClientOption.LocationMode.Battery_Saving
                    setScanSpan(20000)
                    setIsNeedAddress(true)
                    setNeedNewVersionRgc(true)
                }
            })
        }
        var isAlreadySigned by remember { mutableStateOf<Boolean?>(null) }
        val signer = ChaoxingLocationSigner(ChaoxingHttpClient.instance!!, destination)
        LaunchedEffect(Unit) {
            isAlreadySigned = signer.preSign()
            locationClient.start()
        }
        var isShowDialog by remember { mutableStateOf(false) }
        Scaffold(
            floatingActionButton = {
                if (isAlreadySigned == false) {
                    Column {
                        FloatingActionButton(onClick = {
                            isShowDialog = true
                        }) {
                            Icon(painterResource(R.drawable.ic_edit), contentDescription = "定位")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        FloatingActionButton(onClick = {
                            locationClient.start()
                        }) {
                            Icon(
                                painterResource(R.drawable.ic_locate_fixed),
                                contentDescription = "定位"
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                val locationPermissionsState = rememberMultiplePermissionsState(
                    listOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
                if (locationPermissionsState.allPermissionsGranted) {
                    when (isAlreadySigned) {
                        true -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_clipboard_check),
                                    contentDescription = "已签到"
                                )
                                Text("当前签到活动已经签到，不能重复签到。")
                                Button(onClick = {
                                    navToCourseDetailDestination()
                                }) {
                                    Text("返回")
                                }
                            }
                        }

                        false -> {
                            SDKInitializer.initialize(context.applicationContext)
                            var isSignSuccess by remember { mutableStateOf(false) }
                            var marker by remember { mutableStateOf<Marker?>(null) }
                            var isNeedLocationDescribe by remember { mutableStateOf(false) }
                            var clickedPosition by remember { mutableStateOf(LatLng(0.0, 0.0)) }
                            var locationRange by remember { mutableStateOf<Int?>(null) }
                            var locationPosition by remember { mutableStateOf<LatLng?>(null) }
                            var clickedName by remember { mutableStateOf("未指定") }
                            if (isShowDialog) {
                                AlertDialog(onDismissRequest = {
                                    isShowDialog = false
                                }, confirmButton = {
                                    Button(onClick = {
                                        isShowDialog = false
                                    }) {
                                        Text("OK")
                                    }
                                }, text = {
                                    TextField(value = clickedName, onValueChange = {
                                        clickedName = it
                                    }, label = {
                                        Text("位置描述")
                                    })
                                })
                            }
                            val geoCoder = GeoCoder.newInstance().apply {
                                setOnGetGeoCodeResultListener(object : OnGetGeoCoderResultListener {
                                    override fun onGetGeoCodeResult(p0: GeoCodeResult?) {}

                                    override fun onGetReverseGeoCodeResult(p0: ReverseGeoCodeResult?) {
                                        Log.d("GetLocationPage", "ReverseGeoCodeResult: $p0")
                                        if (p0 == null || p0.error != SearchResult.ERRORNO.NO_ERROR) {
                                            Log.w(
                                                "GetLocationPage",
                                                "ReverseGeoCodeResult error: ${p0?.error}"
                                            )
                                            return
                                        }
                                        if (isNeedLocationDescribe) {
                                            clickedName = p0.address + clickedName
                                            isNeedLocationDescribe = false
                                        } else {
                                            clickedName = p0.poiList?.get(0)?.address ?: p0.address
                                        }
                                    }
                                })
                            }
                            val mapView = TextureMapView(context, BaiduMapOptions().apply {
                                rotateGesturesEnabled(false)
                                overlookingGesturesEnabled(false)
                                compassEnabled(false)
                                zoomControlsEnabled(false)
                            }).apply {
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
                                            map.animateMapStatus(
                                                MapStatusUpdateFactory.newLatLngZoom(
                                                    LatLng(
                                                        it.latitude,
                                                        it.longitude
                                                    ), 18f
                                                ), 1000
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
                                            }
                                            clickedPosition = LatLng(it.latitude, it.longitude)
                                            clickedName = it.addrStr?.removePrefix("中国") ?: ""

                                        }
                                    }
                                })
                                map.setOnMapClickListener(object : BaiduMap.OnMapClickListener {
                                    override fun onMapClick(p0: LatLng?) {
                                        Log.d("GetLocationPage", "onMapClick: $p0")
                                        p0?.let {
                                            clickedPosition = it
                                            geoCoder.reverseGeoCode(
                                                ReverseGeoCodeOption()
                                                    .location(it)
                                                    .newVersion(1)
                                                    .radius(500)
                                            )
                                            if (marker == null) {
                                                val icon =
                                                    BitmapDescriptorFactory.fromResource(R.drawable.ic_geo_alt_fill)
                                                marker = map.addOverlay(
                                                    MarkerOptions()
                                                        .position(it)
                                                        .icon(icon)
                                                        .draggable(true)
                                                ) as Marker
                                            } else {
                                                marker!!.position = it
                                            }
                                        }
                                    }

                                    override fun onMapPoiClick(p0: MapPoi?) {
                                        Log.d("GetLocationPage", "onMapPoiClick: $p0")
                                        p0?.let {
                                            clickedPosition = it.position
                                            clickedName = it.name
                                            isNeedLocationDescribe = true
                                            geoCoder.reverseGeoCode(
                                                ReverseGeoCodeOption()
                                                    .location(it.position)
                                                    .newVersion(1)
                                                    .pageSize(2)
                                                    .radius(500)
                                            )
                                            if (marker == null) {
                                                marker = map.addOverlay(
                                                    MarkerOptions()
                                                        .position(it.position)
                                                        .draggable(true)
                                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_geo_alt_fill))
                                                ) as Marker
                                            } else {
                                                marker!!.position = it.position
                                            }
                                        }
                                    }
                                })

                                map.setOnMarkerDragListener(object : BaiduMap.OnMarkerDragListener {
                                    override fun onMarkerDrag(p0: Marker?) {}

                                    override fun onMarkerDragEnd(p0: Marker?) {
                                        Log.d("GetLocationPage", "onMarkerDragEnd: $p0")
                                        p0?.let {
                                            clickedPosition = it.position
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
                            }

                            DisposableEffect(Unit) {
                                onDispose {
                                    mapView.onDestroy()
                                    locationClient.stop()
                                    mapView.map.isMyLocationEnabled = false
                                    geoCoder.destroy()
                                }
                            }
                            LaunchedEffect(Unit) {
                                val signInfo =
                                    ChaoxingLocationSigner.getLocationSignInfo(destination.activeId)
                                if (signInfo.isAvailable()) {
                                    locationRange = signInfo.locationRange

                                    locationPosition =
                                        LatLng(signInfo.latitude!!, signInfo.longitude!!)
                                    mapView.map.setMapStatus(
                                        MapStatusUpdateFactory.newLatLng(
                                            locationPosition
                                        )
                                    )
                                    mapView.map.addOverlay(
                                        CircleOptions()
                                            .center(locationPosition)
                                            .radius(signInfo.locationRange!!)
                                            .fillColor(Color.argb(128, 255, 0, 0))
                                    )
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(6.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "经度: ${
                                            "%.5f".format(clickedPosition.longitude)
                                        },纬度: ${
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
                                Button(onClick = {
                                    if (marker == null) {
                                        Toast.makeText(
                                            context,
                                            "请先点击地图选择位置",
                                            Toast.LENGTH_SHORT
                                        )
                                            .show()
                                        return@Button
                                    }
                                    if (locationRange != null && locationPosition != null) {
                                        if (CoordUtil.getDistance(
                                                CoordUtil.ll2point(clickedPosition),
                                                CoordUtil.ll2point(locationPosition)
                                            ) > locationRange!!
                                        ) {
                                            Toast.makeText(
                                                context,
                                                "位置超出范围",
                                                Toast.LENGTH_SHORT
                                            )
                                                .show()
                                            return@Button
                                        }
                                    }
                                    val postLocationEntity = ChaoxingLocationSignEntity(
                                        clickedPosition.latitude,
                                        clickedPosition.longitude,
                                        clickedName
                                    )
                                    coroutineScope.launch {
                                        try {
                                            if (isAlreadySigned == true) {
                                                Toast.makeText(
                                                    context,
                                                    "当前签到活动已经签到，不能重复签到",
                                                    Toast.LENGTH_SHORT
                                                )
                                                    .show()
                                                return@launch
                                            } else {
                                                signer.sign(postLocationEntity)
                                                isSignSuccess = true
                                            }
                                        } catch (e: ChaoxingLocationSigner.ChaoxingLocationSignException) {
                                            Toast.makeText(context, e.message, Toast.LENGTH_SHORT)
                                                .show()
                                            return@launch
                                        }
                                    }.invokeOnCompletion {
                                        if (isSignSuccess) {
                                            Toast.makeText(context, "签到成功", Toast.LENGTH_SHORT)
                                                .show()
                                            UMengHelper.onSignLocationEvent(
                                                context,
                                                postLocationEntity
                                            )
                                            navToCourseDetailDestination()
                                        }
                                    }
                                }, enabled = !isSignSuccess, modifier = Modifier.width(80.dp)) {
                                    Text("签到")
                                }
                            }
                            AndroidView(
                                factory = {
                                    mapView
                                })
                        }

                        null -> {
                            CenterCircularProgressIndicator()
                        }
                    }

                } else {
                    val allPermissionsRevoked =
                        locationPermissionsState.permissions.size ==
                                locationPermissionsState.revokedPermissions.size

                    val textToShow = if (!allPermissionsRevoked) {
                        "我们需要更加精确的位置信息。"
                    } else if (locationPermissionsState.shouldShowRationale) {
                        "我们真的需要你的位置信息。"
                    } else {
                        "我们需要你的位置信息。"
                    }

                    val buttonText = if (!allPermissionsRevoked) {
                        "授予精准位置权限"
                    } else {
                        "授予位置权限"
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = textToShow)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { locationPermissionsState.launchMultiplePermissionRequest() },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(buttonText)
                        }
                    }
                }
            }
        }
    }
}