package pxprpcapi.androidhelper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import project.xplat.launcher.ApiServer;
import pxprpc.extend.AsyncReturn;
import xplatj.gdxplat.pursuer.utils.Env;

import java.util.Random;

public class MediaProjection2 {
    public static final int ServiceBinderCode=1;
    public static final String PxprpcNamespace="AndroidHelper.MediaProjection";
    public MediaProjectionManager mpm;
    public MediaProjection mp;
    public MediaProjection2(){
        mpm=(MediaProjectionManager) ApiServer.defaultAndroidContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }
    public Intent mediaProjectionToken;
    public boolean mediaProjectionRequest(Parcel param,Parcel result){
        int resultCode = param.readInt();
        this.mediaProjectionToken=param.readParcelable(ApiServer.defaultAndroidContext.getClassLoader());
        this.mp=mpm.getMediaProjection(resultCode,mediaProjectionToken);
        result.writeString(null);
        return true;
    }
    public void startScreenCapture(SurfaceManager.SurfaceWrap sur){
        DisplayMetrics dm = ApiServer.defaultAndroidContext.getResources().getDisplayMetrics();
        this.mp.createVirtualDisplay("pxprpc_virtual_display", dm.widthPixels,dm.heightPixels,dm.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,sur.androidSurface,null,null);
    }

    public boolean requestScreenCapture(final AsyncReturn<Boolean> ret){
        int reqCode= Env.i(Random.class).nextInt(0xffffff);
        ApiServer.onActivityResultCallback.put(reqCode,(param)->{
            if((Integer)param[1]==Activity.RESULT_OK){
                mediaProjectionToken=(Intent)param[2];
                Parcel data=Parcel.obtain();
                Parcel result=Parcel.obtain();
                data.writeInt((Integer)param[1]);
                data.writeParcelable(mediaProjectionToken, 0);
                try {
                    ApiServer.serviceBinder.transact(reqCode,data,result,0);
                    String errInfo=result.readString();
                    if(errInfo==null){
                        ret.resolve(true);
                    }else{
                        ret.reject(new Exception(errInfo));
                    }
                } catch (RemoteException e) {
                    ret.reject(e);
                }
            }else{
                ret.resolve(false);
            }
            return true;
        });
        ((Activity)ApiServer.defaultAndroidContext).startActivityForResult(mpm.createScreenCaptureIntent(),reqCode);
        return false;
    }
}
