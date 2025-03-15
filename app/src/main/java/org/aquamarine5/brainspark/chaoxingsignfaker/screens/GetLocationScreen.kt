package org.aquamarine5.brainspark.chaoxingsignfaker.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.setValue
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
import com.baidu.mapapi.map.BitmapDescriptorFactory
import com.baidu.mapapi.map.CircleOptions
import com.baidu.mapapi.map.MapPoi
import com.baidu.mapapi.map.MapStatus
import com.baidu.mapapi.map.MapStatusUpdateFactory
import com.baidu.mapapi.map.MapView
import com.baidu.mapapi.map.Marker
import com.baidu.mapapi.map.MarkerOptions
import com.baidu.mapapi.map.MyLocationData
import com.baidu.mapapi.model.CoordUtil
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.search.geocode.GeoCodeResult
import com.baidu.mapapi.search.geocode.GeoCoder
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingLocationDetailEntity


typealias GetLocationDestination = ChaoxingLocationDetailEntity

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun GetLocationPage(
    position: ChaoxingLocationDetailEntity,
    navToSignDestination: (LocationSignDestination) -> Unit
) {
    LocalContext.current.let { context->
        SDKInitializer.initialize(context.applicationContext)
        var clickedPosition by remember { mutableStateOf(position.toLatLng()) }
        var clickedName by remember { mutableStateOf("") }
        var isOutRange by remember { mutableStateOf(false) }
        val geoCoder = GeoCoder.newInstance().apply {
            setOnGetGeoCodeResultListener(object : OnGetGeoCoderResultListener {
                override fun onGetGeoCodeResult(p0: GeoCodeResult?) {
                }

                override fun onGetReverseGeoCodeResult(p0: ReverseGeoCodeResult?) {
                    clickedName = p0!!.address
                }
            })
        }
        val mapView = MapView(LocalContext.current, BaiduMapOptions().apply {
            rotateGesturesEnabled(false)
            overlookingGesturesEnabled(false)
            compassEnabled(false)
        }).apply {
            isClickable = true
            map.let { map->
                map.setMapStatus(
                    MapStatusUpdateFactory.newMapStatus(
                        MapStatus.Builder()
                            .zoom(18f)
                            .build()
                    )
                )
                map.isMyLocationEnabled = true
                val locationClient = LocationClient(context.applicationContext).apply {
                    locOption = LocationClientOption().apply {
                        setCoorType("bd09ll") // 设置坐标类型
                        setScanSpan(1000)
                    }
                    registerLocationListener(object: BDAbstractLocationListener() {
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
                            }
                        }

                    })
                }
                locationClient.start()
                if (position.isShow()) {
                    map.addOverlay(
                        CircleOptions()
                            .center(LatLng(position.latitude, position.longitude))
                            .radius(position.locationRange.toInt())
                            .fillColor(0xAA0000FF.toInt())
                    )
                    map.addOverlay(
                        MarkerOptions()
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_map_pin))
                            .position(position.toLatLng())
                            .draggable(true)
                    )
                    map.setOnMarkerDragListener(object : BaiduMap.OnMarkerDragListener {
                        override fun onMarkerDragEnd(p0: Marker?) {
                            clickedPosition = p0!!.position
                            if (CoordUtil.getDistance(
                                    CoordUtil.ll2point(clickedPosition),
                                    CoordUtil.ll2point(position.toLatLng())
                                ) > position.locationRange.toInt()
                            ) {
                                isOutRange = true
                            }
                            geoCoder.reverseGeoCode(
                                ReverseGeoCodeOption()
                                    .location(clickedPosition)
                                    .newVersion(1)
                                    .radius(500)
                            )
                        }

                        override fun onMarkerDragStart(p0: Marker?) {

                        }

                        override fun onMarkerDrag(p0: Marker?) {
                        }
                    })
                } else {
                    map.setOnMapClickListener(object : BaiduMap.OnMapClickListener {
                        override fun onMapClick(p0: LatLng?) {
                            clickedPosition = p0!!
                            if (CoordUtil.getDistance(
                                    CoordUtil.ll2point(clickedPosition),
                                    CoordUtil.ll2point(position.toLatLng())
                                ) > position.locationRange.toInt()
                            ) {
                                isOutRange = true
                            }
                            geoCoder.reverseGeoCode(
                                ReverseGeoCodeOption()
                                    .location(clickedPosition)
                                    .newVersion(1)
                                    .radius(500)
                            )
                        }

                        override fun onMapPoiClick(p0: MapPoi?) {
                        }
                    })
                }
            }
        }

        Scaffold { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                val locationPermissionsState= rememberMultiplePermissionsState(listOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ))
                if(locationPermissionsState.allPermissionsGranted){
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
                            if (isOutRange) {
                                Toast.makeText(this@apply, "超出范围", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            navToSignDestination(
                                LocationSignDestination(
                                    clickedPosition.latitude,
                                    clickedPosition.longitude,
                                    clickedName
                                )
                            )
                        }) {
                            Text("签到")
                        }
                    }
                    AndroidView(
                        factory = {
                            mapView
                        })
                }else{
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

                    Text(text = textToShow)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { locationPermissionsState.launchMultiplePermissionRequest() }) {
                        Text(buttonText)
                    }
                }

            }
            DisposableEffect(Unit) {
                onDispose {
                    mapView.onDestroy()
                    mapView.map.isMyLocationEnabled=false
                    geoCoder.destroy()
                }
            }
        }
    }

}