package io.worldsup.worldsup.state;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.util.Objects;

/**
 * Created by andrewma on 1/19/2018.
 */

public class MapStateManager {
    public static final String STATE_PREFERENCES_NAME="mapStateToReserve";
    private SharedPreferences mapStatePrefs;

    public MapStateManager(Context context) {
        Objects.requireNonNull(context);
        mapStatePrefs = context.getSharedPreferences(STATE_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }
    public void saveMapState(GoogleMap map){
        SharedPreferences.Editor editor = mapStatePrefs.edit();
        CameraPosition cameraPosition = map.getCameraPosition();

        editor.putString("latitude",""+cameraPosition.target.latitude);
        editor.putString("longitude",""+cameraPosition.target.longitude);
        editor.putFloat("zoom",cameraPosition.zoom);
        editor.putFloat("tilt",cameraPosition.tilt);
        editor.putFloat("bearing",cameraPosition.bearing);
        editor.putInt("mapType",map.getMapType());

        editor.commit();
    }
    public CameraPosition getCameraPosition(){
        String lat = mapStatePrefs.getString("latitude",null);
        if(lat==null)return null;
        String lng = mapStatePrefs.getString("longitude",null);
        LatLng latLng = new LatLng(Double.valueOf(lat),Double.valueOf(lng));
        CameraPosition cameraPosition = new CameraPosition(latLng,mapStatePrefs.getFloat("zoom",0f),mapStatePrefs.getFloat("tilt",0f),mapStatePrefs.getFloat("bearing",0));
        return cameraPosition;
    }
    public int getMapType(){
        int mapType = mapStatePrefs.getInt("mapType",0);
        return mapType;
    }
}
