package project.xplat.launcher.pxprpcapi.androidhelper;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.*;
import android.hardware.Camera;
import project.xplat.launcher.pxprpcapi.ApiServer;
import pxprpc.base.Serializer2;
import pxprpc.base.Utils;
import pxprpc.extend.AsyncReturn;
import pxprpc.extend.TableSerializer;
import xplatj.gdxconfig.core.PlatCoreConfig;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class Misc2 {
	public static final String PxprpcNamespace="AndroidHelper-Misc";

	Vibrator vb;
	ClipboardManager cbm;
	AudioManager am;
	LocationManager lm;
	NotificationManager nm;

	public static class Light2 {
		public int id;
		public String desc;
		public String camId;
		public Camera1Wrap cam1dev;
	}

	protected ArrayList<Light2> lights;

	public Misc2() {
		vb = (Vibrator) ApiServer.defaultAndroidContext.getSystemService(Service.VIBRATOR_SERVICE);
		cbm = (ClipboardManager) ApiServer.defaultAndroidContext.getSystemService(Service.CLIPBOARD_SERVICE);
		am = (AudioManager) ApiServer.defaultAndroidContext.getSystemService(Service.AUDIO_SERVICE);
		lm = (LocationManager) ApiServer.defaultAndroidContext.getSystemService(Service.LOCATION_SERVICE);
		nm=(NotificationManager)ApiServer.defaultAndroidContext.getSystemService((Service.NOTIFICATION_SERVICE));
		lights = new ArrayList<Light2>();
		initCameraFlashLight();
	}

	protected void initCameraFlashLight() {
		CameraManager camSrv = ApiServer.androidcamera2.camSrv;
		String[] camList;
		try {
			camList = camSrv.getCameraIdList();
			for (String camId : camList) {
				CameraCharacteristics info = camSrv.getCameraCharacteristics(camId);
				if (info.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)) {
					Light2 tl = new Light2();
					tl.id = lights.size();
					tl.desc = "flash for cameara " + camId;
					tl.camId = camId;
					lights.add(tl);
				}
			}
		} catch (CameraAccessException e) {
			e.printStackTrace();
		}

	}

	public String notificationChannelId="pxprpc";
	public void init() {
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
			this.notificationChannelId=ApiServer.defaultAndroidContext.getPackageName() + ":pxprpc";
			NotificationChannel chan = new NotificationChannel(this.notificationChannelId, "pxprpc channel",
					NotificationManager.IMPORTANCE_UNSPECIFIED);
			nm.createNotificationChannel(chan);
		}
	}

	public void deinit() {
	}

	public boolean hasVibrator() {
		return vb.hasVibrator();
	}

	public void vibrate(int ms, int amplitude) {
		// amplitude:-1 default, range 0-255
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			vb.vibrate(VibrationEffect.createOneShot(ms, amplitude));
		} else {
			vb.vibrate(ms);
		}
	}

	public String getClipboardText() {
		ClipData cb = cbm.getPrimaryClip();
		if (cb.getItemCount() > 0) {
			return cb.getItemAt(0).getText().toString();
		}
		return null;
	}

	public void setClipboardText(String text) {
		cbm.setPrimaryClip(ClipData.newPlainText("pxprpc", text));
	}

	public int getDefaultAudioVolume() {
		return am.getStreamVolume(AudioManager.STREAM_MUSIC);
	}

	public void setDefaultAudioVolume(int vol) {
		am.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0);
	}

	public LocationListener lastLocationListener = null;

	@SuppressLint("MissingPermission")
	public ByteBuffer getGpsLocationInfo(final AsyncReturn<ByteBuffer> ret, final boolean msgpackMode) {
		if (this.lastLocationListener != null)
			this.cancelGetGpsLocationInfo();
		this.lastLocationListener = new LocationListener() {
			@Override
			public void onLocationChanged(Location location) {
				if (msgpackMode) {
					ret.resolve(new TableSerializer().setHeader("dddddd", new String[]{
							"latitude", "longitude", "speed", "bearing", "altitude", "accuracy"})
									.addRow(new Object[]{
											location.getLatitude(),location.getLongitude(),location.getSpeed(),
											location.getBearing(),location.getAltitude(),location.getAccuracy()
									}).build());
				} else {
					Serializer2 ser=new Serializer2();
					ser.putDouble(location.getLatitude());
					ser.putDouble(location.getLongitude());
					ser.putDouble(location.getSpeed());
					ser.putDouble(location.getBearing());
					ser.putDouble(location.getAltitude());
					ser.putDouble(location.getAccuracy());
					ret.resolve(ser.build());
				}
			}

			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
			}

			@Override
			public void onProviderEnabled(String provider) {
			}

			@Override
			public void onProviderDisabled(String provider) {
				Misc2.this.cancelGetGpsLocationInfo();
				ret.reject(new IOException("User disable gps provider"));
			}
		};
		lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, this.lastLocationListener,
				ApiServer.getHandler().getLooper());
		return null;
	}

	public void cancelGetGpsLocationInfo() {
		lm.removeUpdates(this.lastLocationListener);
	}

	public ByteBuffer getLightsInfo() {
		TableSerializer ser = new TableSerializer().setHeader("is", new String[]{"id", "desc"});
		for (Light2 tl : this.lights) {
			ser.addRow(new Object[]{tl.id,tl.desc});
		}
		return ser.build();
	}

	public static class Camera1Wrap implements Closeable {
		public Camera wrap;
		public Camera1Wrap(Camera wrap){
			this.wrap=wrap;
			ApiServer.androidcamera2.openedResource.add(this);
		}
		@Override
		public void close() throws IOException {
			wrap.release();
			ApiServer.androidcamera2.openedResource.remove(this);
		}
	}
	public void turnOnLight(int id) throws CameraAccessException {
		final Light2 l = this.lights.get(id);
		final CameraManager camSrv = ApiServer.androidcamera2.camSrv;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			camSrv.setTorchMode(l.camId, true);
		} else {
			//we suppose cameraid is just string format of id of old camera api, It's seemed work.
			Camera dev = Camera.open(Integer.parseInt(l.camId));
			l.cam1dev=new Camera1Wrap(dev);
			Camera.Parameters p = dev.getParameters();
			p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
			dev.setParameters(p);
			dev.startPreview();
		}

	}

	public void turnOffLight(int id) throws CameraAccessException {
		final Light2 l = this.lights.get(id);
		CameraManager camSrv = ApiServer.androidcamera2.camSrv;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			camSrv.setTorchMode(l.camId, false);
		} else if(l.cam1dev!=null){
			l.cam1dev.wrap.stopPreview();
			Camera.Parameters p = l.cam1dev.wrap.getParameters();
			p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			l.cam1dev.wrap.setParameters(p);
			ApiServer.closeQuietly(l.cam1dev);
			l.cam1dev=null;
		}
	}

	public void postNotification(int notifyId,String title,String content){
		Notification.Builder nb;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			nb = new Notification.Builder(ApiServer.defaultAndroidContext,this.notificationChannelId);
		}else{
			nb = new Notification.Builder(ApiServer.defaultAndroidContext);
		}
		Notification noti=nb.setDefaults(Notification.DEFAULT_ALL).setContentTitle(title).setContentTitle(content).
				setTicker(title).setWhen(System.currentTimeMillis()).setSmallIcon(android.R.drawable.alert_light_frame).build();
		this.nm.notify(notifyId,noti);
	}
}
