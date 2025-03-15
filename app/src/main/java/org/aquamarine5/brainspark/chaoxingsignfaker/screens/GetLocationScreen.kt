package org.aquamarine5.brainspark.chaoxingsignfaker.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.baidu.mapapi.SDKInitializer
import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.BaiduMapOptions
import com.baidu.mapapi.map.CircleOptions
import com.baidu.mapapi.map.MapStatus
import com.baidu.mapapi.map.MapStatusUpdateFactory
import com.baidu.mapapi.map.MapView
import com.baidu.mapapi.map.Marker
import com.baidu.mapapi.map.MarkerOptions
import com.baidu.mapapi.model.CoordUtil
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.search.geocode.GeoCodeResult
import com.baidu.mapapi.search.geocode.GeoCoder
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingLocationDetailEntity

typealias GetLocationDestination = ChaoxingLocationDetailEntity

@Composable
fun GetLocationPage(
    navController: NavController,
    position: ChaoxingLocationDetailEntity
) {
    LocalContext.current.apply {
        SDKInitializer.initialize(this)
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
            map.apply {
                setMapStatus(
                    MapStatusUpdateFactory.newMapStatus(
                        MapStatus.Builder()
                            .zoom(18f)
                            .build()
                    )
                )
                addOverlay(
                    CircleOptions()
                        .center(LatLng(position.latitude, position.longitude))
                        .radius(position.locationRange.toInt())
                        .fillColor(0xAA0000FF.toInt())
                )
                addOverlay(
                    MarkerOptions()
                        .position(position.toLatLng())
                        .draggable(true)
                )
                setOnMarkerDragListener(object : BaiduMap.OnMarkerDragListener {
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
            }
        }

        Scaffold { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
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
                        navController.navigate(
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
            }
            DisposableEffect(Unit) {
                onDispose {
                    mapView.onDestroy()
                    geoCoder.destroy()
                }
            }
        }
    }

}