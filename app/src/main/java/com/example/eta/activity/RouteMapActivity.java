package com.example.eta.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.eta.R;
import com.example.eta.util.Keyholder;
import com.example.eta.util.LocationHelper;
import com.example.eta.util.LocationResultListener;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public class RouteMapActivity extends AppCompatActivity implements LocationResultListener {

    private static final String TAG = "RouteMapActivity";
    private static final String ROUTE_API_BASE = "https://apis.openapi.sk.com";
    private static final int APP_LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // UI 컴포넌트
    private FrameLayout mapContainer;
    private TextView textRouteInfo;
    private TMapView tMapView;

    // 데이터
    private String startAddr;
    private String endAddr;
    private String nickname;
    private String userId;
    private TMapPoint startPoint;
    private TMapPoint endPoint;

    // 위치 관련
    private LocationHelper locationHelper;
    private TMapMarkerItem markergps = new TMapMarkerItem();

    // 네트워크
    private RouteService routeService;

    // 수정된 RouteService 인터페이스 - 동적 헤더 사용
    interface RouteService {
        @Headers({
                "Accept: application/json",
                "Content-Type: application/json"
        })
        @POST("/transit/routes")
        Call<JsonObject> getRoute(@Header("appKey") String apiKey, @Body JsonObject body);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_map);

        // 액션바 설정
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("경로 안내");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initViews();
        initLocationHelper();
        initTMap();
        initRetrofit();
        getIntentData();

        if (startAddr != null && endAddr != null) {
            calculateRoute();
        }
    }

    private void initViews() {
        mapContainer = findViewById(R.id.tmapcon);
        textRouteInfo = findViewById(R.id.text_route_info);

        // 다크 테마 적용
        textRouteInfo.setTextColor(getResources().getColor(R.color.text_primary));
        textRouteInfo.setBackgroundColor(getResources().getColor(R.color.surface_color));
    }

    private void initLocationHelper() {
        locationHelper = new LocationHelper(this);
        setupCurrentLocationMarker();
        startLocationUpdates();
    }

    private void setupCurrentLocationMarker() {
        Drawable drawable = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_mylocation);
        if (drawable != null) {
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();
            drawable.setBounds(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
            markergps.setIcon(bitmap);
            markergps.setName("현위치");
        }
    }

    private void initTMap() {
        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey(Keyholder.getAppKey());
        mapContainer.addView(tMapView);
    }

    private void initRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ROUTE_API_BASE)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        routeService = retrofit.create(RouteService.class);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        endAddr = intent.getStringExtra("end");
        startAddr = intent.getStringExtra("start");
        nickname = intent.getStringExtra("nickname");
        userId = intent.getStringExtra("userId");

        Log.d(TAG, "받은 데이터 - 출발지: " + startAddr + ", 도착지: " + endAddr);
    }

    private void calculateRoute() {
        if (TextUtils.isEmpty(startAddr) || !startAddr.contains(",") ||
                TextUtils.isEmpty(endAddr) || !endAddr.contains(",")) {
            Log.e(TAG, "잘못된 주소 형식: " + startAddr + " / " + endAddr);
            Toast.makeText(this, "주소 형식이 잘못되었습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String[] startCoords = startAddr.split(",");
            double startLat = Double.parseDouble(startCoords[0].trim());
            double startLon = Double.parseDouble(startCoords[1].trim());

            String[] endCoords = endAddr.split(",");
            double endLat = Double.parseDouble(endCoords[0].trim());
            double endLon = Double.parseDouble(endCoords[1].trim());

            Log.d(TAG, "출발지 좌표: " + startLat + "," + startLon);
            Log.d(TAG, "도착지 좌표: " + endLat + "," + endLon);

            startPoint = new TMapPoint(startLat, startLon);
            endPoint = new TMapPoint(endLat, endLon);

            requestRoute(startLon, startLat, endLon, endLat);

        } catch (NumberFormatException e) {
            Log.e(TAG, "좌표 파싱 실패", e);
            Toast.makeText(this, "좌표 형식이 잘못되었습니다", Toast.LENGTH_SHORT).show();
        }
    }

    // 수정된 requestRoute 메소드 - API 키를 파라미터로 전달
    private void requestRoute(double startLon, double startLat, double endLon, double endLat) {
        JsonObject body = new JsonObject();
        body.addProperty("startX", String.valueOf(startLon));
        body.addProperty("startY", String.valueOf(startLat));
        body.addProperty("endX", String.valueOf(endLon));
        body.addProperty("endY", String.valueOf(endLat));
        body.addProperty("count", 1);
        body.addProperty("lang", 0);
        body.addProperty("format", "json");

        // API 키를 파라미터로 전달
        routeService.getRoute(Keyholder.getAppKey(), body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    drawOnMap(startPoint, endPoint, response.body());
                } else {
                    Log.e(TAG, "Route API 실패: " + response.code() + " " + response.message());
                    Toast.makeText(RouteMapActivity.this, "경로 계산에 실패했습니다", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Route API 오류: " + t.getMessage());
                Toast.makeText(RouteMapActivity.this, "네트워크 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 나머지 메소드들은 동일...
    private void drawOnMap(TMapPoint start, TMapPoint end, JsonObject response) {
        // 마커 추가
        tMapView.addMarkerItem("start", createMarker(start, "출발지"));
        tMapView.addMarkerItem("end", createMarker(end, "도착지"));
        tMapView.setCenterPoint(start.getLongitude(), start.getLatitude());

        // 폴리라인 그리기
        TMapPolyLine poly = new TMapPolyLine();
        poly.setLineColor(getResources().getColor(R.color.button_primary));
        poly.setLineWidth(8);

        try {
            JsonObject itinerary = response.getAsJsonObject("metaData")
                    .getAsJsonObject("plan")
                    .getAsJsonArray("itineraries")
                    .get(0)
                    .getAsJsonObject();

            int totalTime = itinerary.get("totalTime").getAsInt();
            JsonArray legs = itinerary.getAsJsonArray("legs");

            for (JsonElement legElement : legs) {
                JsonObject leg = legElement.getAsJsonObject();
                String lineString = null;

                if (leg.has("passShape")) {
                    lineString = leg.getAsJsonObject("passShape").get("linestring").getAsString();
                } else if (leg.has("steps")) {
                    JsonArray steps = leg.getAsJsonArray("steps");
                    for (JsonElement stepElement : steps) {
                        JsonObject step = stepElement.getAsJsonObject();
                        if (step.has("linestring")) {
                            lineString = step.get("linestring").getAsString();
                            break;
                        }
                    }
                }

                if (lineString != null) {
                    for (String coordinate : lineString.split(" ")) {
                        String[] coords = coordinate.split(",");
                        if (coords.length >= 2) {
                            double lon = Double.parseDouble(coords[0].trim());
                            double lat = Double.parseDouble(coords[1].trim());
                            poly.addLinePoint(new TMapPoint(lat, lon));
                        }
                    }
                }
            }

            tMapView.addTMapPolyLine("route", poly);

            // 시간 포맷팅
            String timeFormatted = String.format("%02d:%02d:%02d",
                    totalTime / 3600, (totalTime % 3600) / 60, totalTime % 60);

            // 경로 안내 텍스트 생성
            List<String> instructions = generateTextInstructions(response);
            instructions.add(0, "총 소요 시간: " + timeFormatted);

            textRouteInfo.setText(TextUtils.join("\n", instructions));

        } catch (Exception e) {
            Log.e(TAG, "지도 그리기 실패", e);
            Toast.makeText(this, "경로 표시에 실패했습니다", Toast.LENGTH_SHORT).show();
        }
    }

    private TMapMarkerItem createMarker(TMapPoint point, String title) {
        TMapMarkerItem marker = new TMapMarkerItem();
        Drawable drawable = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_mylocation);
        if (drawable != null) {
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();
            drawable.setBounds(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
            marker.setIcon(bitmap);
        }
        marker.setTMapPoint(point);
        marker.setName(title);
        return marker;
    }

    // 나머지 메소드들은 기존과 동일...
    private static class Segment {
        String mode, start, end, route;
        int distance, time;

        Segment(String mode, String start, String end, int distance, int time, String route) {
            this.mode = mode;
            this.start = start;
            this.end = end;
            this.distance = distance;
            this.time = time;
            this.route = route;
        }
    }

    private List<String> generateTextInstructions(JsonObject response) {
        try {
            JsonObject plan = response.getAsJsonObject("metaData")
                    .getAsJsonObject("plan")
                    .getAsJsonArray("itineraries")
                    .get(0)
                    .getAsJsonObject();

            JsonArray legs = plan.getAsJsonArray("legs");
            JsonObject first = legs.get(0).getAsJsonObject();

            Segment current = new Segment(
                    first.get("mode").getAsString(),
                    first.getAsJsonObject("start").get("name").getAsString(),
                    first.getAsJsonObject("end").get("name").getAsString(),
                    first.get("distance").getAsInt(),
                    first.get("sectionTime").getAsInt(),
                    first.has("route") ? first.get("route").getAsString() : null
            );

            List<Segment> segments = new ArrayList<>();

            for (int i = 1; i < legs.size(); i++) {
                JsonObject leg = legs.get(i).getAsJsonObject();
                String mode = leg.get("mode").getAsString();

                if (mode.equals(current.mode)) {
                    current.distance += leg.get("distance").getAsInt();
                    current.time += leg.get("sectionTime").getAsInt();
                    current.end = leg.getAsJsonObject("end").get("name").getAsString();
                } else {
                    segments.add(current);
                    current = new Segment(
                            mode,
                            leg.getAsJsonObject("start").get("name").getAsString(),
                            leg.getAsJsonObject("end").get("name").getAsString(),
                            leg.get("distance").getAsInt(),
                            leg.get("sectionTime").getAsInt(),
                            leg.has("route") ? leg.get("route").getAsString() : null
                    );
                }
            }
            segments.add(current);

            List<String> instructions = new ArrayList<>();
            for (Segment seg : segments) {
                String transport;
                switch (seg.mode) {
                    case "WALK":
                        transport = "도보";
                        break;
                    case "BUS":
                        transport = "버스(" + seg.route + ")";
                        break;
                    case "SUBWAY":
                        transport = "지하철(" + seg.route + ")";
                        break;
                    default:
                        transport = seg.mode;
                }

                String distance = formatDistance(seg.distance);
                String time = formatDuration(seg.time);
                instructions.add(transport + ": " + seg.start + " → " + seg.end +
                        ", 거리 " + distance + ", 소요시간 " + time);
            }

            return instructions;
        } catch (Exception e) {
            Log.e(TAG, "경로 안내 생성 실패", e);
            List<String> errorList = new ArrayList<>();
            errorList.add("경로 안내 정보를 생성할 수 없습니다.");
            return errorList;
        }
    }

    private String formatDistance(int meters) {
        if (meters >= 1000) {
            return String.format("%.1fkm", meters / 1000f);
        } else {
            return meters + "m";
        }
    }

    private String formatDuration(int seconds) {
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h).append("시간 ");
        if (m > 0) sb.append(m).append("분 ");
        if (s > 0) sb.append(s).append("초");
        return sb.toString().trim();
    }

    // LocationResultListener 구현
    @Override
    public void onLocationSuccess(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        TMapPoint gps = new TMapPoint(latitude, longitude);
        markergps.setTMapPoint(gps);
        tMapView.removeMarkerItem("markergps");
        tMapView.addMarkerItem("markergps", markergps);
    }

    @Override
    public void onLocationFailure(String errorMessage) {
        Toast.makeText(this, "위치 오류: " + errorMessage, Toast.LENGTH_LONG).show();
        if (locationHelper.isTracking()) {
            stopLocationUpdates();
        }
    }

    @Override
    public void onPermissionNeeded() {
        Toast.makeText(this, "위치 권한이 필요합니다", Toast.LENGTH_SHORT).show();
        locationHelper.requestLocationPermissions(this, APP_LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void startLocationUpdates() {
        if (!locationHelper.hasLocationPermissions()) {
            locationHelper.requestLocationPermissions(this, APP_LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            locationHelper.startLocationTracking(this);
            Toast.makeText(this, "위치 추적을 시작합니다", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopLocationUpdates() {
        locationHelper.stopLocationTracking();
        Toast.makeText(this, "위치 추적을 중지합니다", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == APP_LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "위치 권한이 허용되었습니다", Toast.LENGTH_SHORT).show();
                startLocationUpdates();
            } else {
                Toast.makeText(this, "위치 권한이 거부되었습니다", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (locationHelper != null && locationHelper.isTracking()) {
            stopLocationUpdates();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationHelper != null) {
            locationHelper.stopLocationTracking();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
