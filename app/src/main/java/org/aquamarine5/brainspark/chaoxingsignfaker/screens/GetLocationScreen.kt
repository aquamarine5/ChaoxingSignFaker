package org.aquamarine5.brainspark.chaoxingsignfaker.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.baidu.mapapi.SDKInitializer
import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.BaiduMapOptions
import com.baidu.mapapi.map.MapPoi
import com.baidu.mapapi.map.MapStatus
import com.baidu.mapapi.map.MapStatusUpdateFactory
import com.baidu.mapapi.map.MapView
import com.baidu.mapapi.map.MyLocationConfiguration
import com.baidu.mapapi.map.MyLocationData
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.search.geocode.GeoCodeResult
import com.baidu.mapapi.search.geocode.GeoCoder
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingPostLocationEntity
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
    LocalContext.current.let { context ->
        SDKInitializer.initialize(context.applicationContext)
        var isSignSuccess by remember { mutableStateOf(false) }
        var clickedPosition by remember { mutableStateOf(LatLng(0.0, 0.0)) }
        var clickedName by remember { mutableStateOf("未指定") }
        val coroutineScope = rememberCoroutineScope()
        val geoCoder = GeoCoder.newInstance().apply {
            setOnGetGeoCodeResultListener(object : OnGetGeoCoderResultListener {
                override fun onGetGeoCodeResult(p0: GeoCodeResult?) {
                }

                override fun onGetReverseGeoCodeResult(p0: ReverseGeoCodeResult?) {
                    clickedName = p0!!.address
                }
            })
        }
        val locationClient = LocationClient(context.applicationContext).apply {
            locOption = LocationClientOption().apply {
                setCoorType("bd09ll")
                setOpenAutoNotifyMode()
                setIsNeedLocationDescribe(true)
                setNeedNewVersionRgc(true)
            }
        }
        val mapView = MapView(LocalContext.current, BaiduMapOptions().apply {
            rotateGesturesEnabled(false)
            overlookingGesturesEnabled(false)
            compassEnabled(false)
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

            map.setMyLocationConfiguration(
                MyLocationConfiguration.Builder(
                    MyLocationConfiguration.LocationMode.NORMAL,
                    true
                ).build()
            )
            locationClient.registerLocationListener(object : BDAbstractLocationListener() {
                override fun onReceiveLocation(location: BDLocation?) {
                    location?.let {
                        map.setMyLocationData(
                            MyLocationData.Builder()
                                .accuracy(it.radius)
                                .direction(it.direction)
                                .latitude(it.latitude)
                                .longitude(it.longitude)
                                .build()
                        )
                        if(clickedName=="未指定"){
                            map.animateMapStatus(MapStatusUpdateFactory.newLatLng(LatLng(it.latitude, it.longitude)))
                        }
                        clickedPosition = LatLng(it.latitude, it.longitude)
                        clickedName = it.locationDescribe
                    }
                }
            })

            locationClient.start()
            map.setOnMapClickListener(object : BaiduMap.OnMapClickListener {
                override fun onMapClick(p0: LatLng?) {
                    p0?.let {
                        clickedPosition = it
                        geoCoder.reverseGeoCode(
                            ReverseGeoCodeOption()
                                .location(p0)
                                .newVersion(1)
                                .radius(500)
                        )
                    }
                }

                override fun onMapPoiClick(p0: MapPoi?) {
                    p0?.let {
                        clickedPosition = it.position
                        clickedName = it.name
                    }
                }
            })
        }
        ChaoxingHttpClient.CheckInstance()
        Scaffold { innerPadding ->
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
                    Row {
                        Column {
                            Text(
                                "经度: ${
                                    "%.6f".format(clickedPosition.longitude)
                                },纬度: ${
                                    "%.6f".format(
                                        clickedPosition.latitude
                                    )
                                }"
                            )
                            Text("位置: $clickedName")
                        }
                        Button(onClick = {
                            if (clickedName == "未指定") {
                                Toast.makeText(context, "请先点击地图选择位置", Toast.LENGTH_SHORT)
                                    .show()
                                return@Button
                            }
                            coroutineScope.launch {
                                try{
                                    ChaoxingLocationSigner(
                                        destination,
                                        ChaoxingPostLocationEntity(
                                            clickedPosition.latitude,
                                            clickedPosition.longitude,
                                            clickedName
                                        )
                                    ).sign()
                                    isSignSuccess=true
                                }
                                catch (e:ChaoxingLocationSigner.ChaoxingLocationSignException){
                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                            }.invokeOnCompletion {
                                Toast.makeText(context, "签到成功", Toast.LENGTH_SHORT).show()
                                //navToCourseDetailDestination()
                            }
                        }, enabled = !isSignSuccess) {
                            Text("签到")
                        }
                    }
                    AndroidView(
                        factory = {
                            mapView
                        })
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
            DisposableEffect(Unit) {
                onDispose {
                    mapView.onDestroy()
                    locationClient.stop()
                    mapView.map.isMyLocationEnabled = false
                    geoCoder.destroy()
                }
            }
        }
    }
}