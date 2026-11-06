package tech.vladr.aegis.ui.map

import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import tech.vladr.aegis.data.DeviceStatus
import tech.vladr.aegis.ui.components.statusColor
import tech.vladr.aegis.ui.theme.Background
import tech.vladr.aegis.ui.theme.Teal

/** OpenStreetMap data drawn by OpenFreeMap: free, no account or key. */
private const val STYLE = "https://tiles.openfreemap.org/styles/dark"

/**
 * MapLibre's map, which is an Android View, inside Compose. The view must
 * hear about the screen's lifecycle - started, paused, destroyed - to load
 * tiles only while it is shown and to free its memory after.
 */
@Composable
fun MapLibreView(state: MapUiState, onDeviceClick: (Long) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val onClick by rememberUpdatedState(onDeviceClick)
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }
    var framed by remember { mutableStateOf(false) }

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            getMapAsync { loaded ->
                map = loaded
                loaded.uiSettings.isRotateGesturesEnabled = false
                loaded.setStyle(Style.Builder().fromUri(STYLE)) { ready ->
                    addLayers(ready)
                    style = ready
                }
                loaded.addOnMapClickListener { point ->
                    // The dots are small: anything within a fingertip of the tap counts.
                    val at = loaded.projection.toScreenLocation(point)
                    val area = RectF(at.x - TOUCH_SLOP, at.y - TOUCH_SLOP, at.x + TOUCH_SLOP, at.y + TOUCH_SLOP)
                    val tapped = loaded.queryRenderedFeatures(area, DEVICE_DOTS)
                    tapped.firstOrNull()?.getNumberProperty("id")?.toLong()?.let { onClick(it) } != null
                }
            }
        }
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    // New data: replace what the two sources hold, and frame it the first time.
    LaunchedEffect(state, style) {
        val ready = style ?: return@LaunchedEffect
        ready.getSourceAs<GeoJsonSource>(ROUTE)?.setGeoJson(routeFeatures(state.route))
        ready.getSourceAs<GeoJsonSource>(DEVICES)?.setGeoJson(deviceFeatures(state.markers))
        if (!framed && (state.route.isNotEmpty() || state.markers.isNotEmpty())) {
            map?.let { frame(it, state) }
            framed = true
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private const val ROUTE = "route"
private const val DEVICES = "devices"
private const val DEVICE_DOTS = "device-dots"
private const val TOUCH_SLOP = 24f

private fun addLayers(style: Style) {
    style.addSource(GeoJsonSource(ROUTE))
    style.addSource(GeoJsonSource(DEVICES))
    style.addLayer(
        LineLayer("route-line", ROUTE).withProperties(
            PropertyFactory.lineColor(Teal.toArgb()),
            PropertyFactory.lineWidth(3f),
            PropertyFactory.lineDasharray(arrayOf(1f, 2f)),
        ),
    )
    style.addLayer(
        CircleLayer(DEVICE_DOTS, DEVICES).withProperties(
            // Each point carries its colour; unsafe networks get a warning ring.
            PropertyFactory.circleColor(Expression.get("color")),
            PropertyFactory.circleRadius(7f),
            PropertyFactory.circleStrokeColor(Expression.get("ring")),
            PropertyFactory.circleStrokeWidth(2.5f),
            // Where markers overlap, the ones that matter are drawn on top.
            PropertyFactory.circleSortKey(Expression.get("rank")),
        ),
    )
}

private fun routeFeatures(route: List<MapPoint>): FeatureCollection =
    if (route.size < 2) {
        FeatureCollection.fromFeatures(emptyList())
    } else {
        FeatureCollection.fromFeature(Feature.fromGeometry(LineString.fromLngLats(route.map { Point.fromLngLat(it.longitude, it.latitude) })))
    }

private fun deviceFeatures(markers: List<DeviceMarker>): FeatureCollection =
    FeatureCollection.fromFeatures(
        markers.map { marker ->
            Feature.fromGeometry(Point.fromLngLat(marker.at.longitude, marker.at.latitude)).apply {
                addNumberProperty("id", marker.deviceId)
                addStringProperty("color", hex(statusColor(marker.status).toArgb()))
                addStringProperty("ring", hex(if (marker.unsafe) 0xFFF5B83D.toInt() else Background.toArgb()))
                addNumberProperty("rank", rank(marker))
            }
        },
    )

private fun hex(argb: Int) = "#%06X".format(argb and 0xFFFFFF)

/** Suspicious devices above all, then your own, then unsafe networks, then everything else. */
private fun rank(marker: DeviceMarker): Int = when (marker.status) {
    DeviceStatus.SUSPICIOUS -> 3
    DeviceStatus.TRUSTED, DeviceStatus.HOME -> 2
    DeviceStatus.UNKNOWN -> if (marker.unsafe) 1 else 0
}

/** Street level: closer than this, one scan's markers fill the screen and the streets vanish. */
private const val MAX_FRAMING_ZOOM = 17.0

/** Moves the camera so that the whole route and every marker are in view. */
private fun frame(map: MapLibreMap, state: MapUiState) {
    val points = state.route.map { LatLng(it.latitude, it.longitude) } + state.markers.map { LatLng(it.at.latitude, it.at.longitude) }
    if (points.size == 1) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(points.single(), MAX_FRAMING_ZOOM))
    } else {
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.Builder().includes(points).build(), 120))
        if (map.cameraPosition.zoom > MAX_FRAMING_ZOOM) map.moveCamera(CameraUpdateFactory.zoomTo(MAX_FRAMING_ZOOM))
    }
}
