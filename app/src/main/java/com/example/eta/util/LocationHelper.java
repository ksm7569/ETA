package com.example.eta.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class LocationHelper implements LocationListener {

    private static final String TAG = "LocationHelper";
    private Context context;
    private LocationManager locationManager;
    private LocationResultListener listener;
    private boolean isTracking = false;

    private static final int MIN_TIME_BETWEEN_UPDATES = 1000; // 1초
    private static final float MIN_DISTANCE_CHANGE = 10f; // 10미터

    public LocationHelper(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public boolean hasLocationPermissions() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
    }

    public void requestLocationPermissions(Activity activity, int requestCode) {
        ActivityCompat.requestPermissions(activity,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }, requestCode);
    }

    public void startLocationTracking(LocationResultListener listener) {
        this.listener = listener;

        if (!hasLocationPermissions()) {
            if (listener != null) {
                listener.onPermissionNeeded();
            }
            return;
        }

        try {
            // GPS 사용 가능한지 확인
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BETWEEN_UPDATES,
                        MIN_DISTANCE_CHANGE,
                        this
                );
                isTracking = true;
                Log.d(TAG, "GPS 위치 추적 시작");
            }
            // 네트워크 사용 가능한지 확인
            else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_BETWEEN_UPDATES,
                        MIN_DISTANCE_CHANGE,
                        this
                );
                isTracking = true;
                Log.d(TAG, "네트워크 위치 추적 시작");
            } else {
                if (listener != null) {
                    listener.onLocationFailure("GPS와 네트워크 모두 비활성화되어 있습니다");
                }
                return;
            }

            // 마지막으로 알려진 위치 가져오기
            Location lastKnownLocation = getLastKnownLocation();
            if (lastKnownLocation != null && listener != null) {
                listener.onLocationSuccess(lastKnownLocation);
            }

        } catch (SecurityException e) {
            Log.e(TAG, "위치 권한 오류", e);
            if (listener != null) {
                listener.onLocationFailure("위치 권한이 없습니다: " + e.getMessage());
            }
        }
    }

    public void stopLocationTracking() {
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
                isTracking = false;
                Log.d(TAG, "위치 추적 중지");
            } catch (SecurityException e) {
                Log.e(TAG, "위치 추적 중지 중 권한 오류", e);
            }
        }
    }

    public void getCurrentLocation(LocationResultListener listener) {
        this.listener = listener;

        if (!hasLocationPermissions()) {
            if (listener != null) {
                listener.onPermissionNeeded();
            }
            return;
        }

        try {
            Location location = getLastKnownLocation();
            if (location != null && listener != null) {
                listener.onLocationSuccess(location);
            } else if (listener != null) {
                listener.onLocationFailure("위치를 찾을 수 없습니다");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "현재 위치 가져오기 오류", e);
            if (listener != null) {
                listener.onLocationFailure("위치 권한이 없습니다: " + e.getMessage());
            }
        }
    }

    private Location getLastKnownLocation() throws SecurityException {
        Location gpsLocation = null;
        Location networkLocation = null;

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }

        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        // 더 정확한 위치 선택
        if (gpsLocation != null && networkLocation != null) {
            return gpsLocation.getTime() > networkLocation.getTime() ? gpsLocation : networkLocation;
        } else if (gpsLocation != null) {
            return gpsLocation;
        } else {
            return networkLocation;
        }
    }

    public boolean isTracking() {
        return isTracking;
    }

    // LocationListener 구현
    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "위치 변경됨: " + location.getLatitude() + ", " + location.getLongitude());
        if (listener != null) {
            listener.onLocationSuccess(location);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d(TAG, "위치 제공자 상태 변경: " + provider + ", 상태: " + status);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, "위치 제공자 활성화: " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, "위치 제공자 비활성화: " + provider);
        if (listener != null) {
            listener.onLocationFailure(provider + "가 비활성화되었습니다");
        }
    }
}
