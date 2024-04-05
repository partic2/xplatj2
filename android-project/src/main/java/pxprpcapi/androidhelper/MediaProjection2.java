package pxprpcapi.androidhelper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import project.xplat.launcher.ApiServer;
import pxprpc.extend.AsyncReturn;
import xplatj.gdxplat.partic2.utils.Env;
import xplatj.javaplat.partic2.pxprpc.AsyncFuncChainPxprpcAdapter;
import xplatj.javaplat.partic2.util.AsyncFuncChain;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Random;

public class MediaProjection2 {
    public static final int ServiceBinderCode=1000;
    public static final String PxprpcNamespace="AndroidHelper.MediaProjection";
    public static MediaProjection2 i;
    public MediaProjectionManager mpm;
    public MediaProjection mp;
    public MediaProjection2(){
        mpm=(MediaProjectionManager) ApiServer.defaultAndroidContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        i=this;
    }
    public Intent mediaProjectionToken;
    public int requestResultCode;
    public boolean mediaProjectionRequest(Parcel param,Parcel result){
        requestResultCode = param.readInt();
        this.mediaProjectionToken=param.readParcelable(ApiServer.defaultAndroidContext.getClassLoader());
        result.writeString(null);
        return true;
    }
    public void startScreenCapture(SurfaceManager.SurfaceWrap sur){
        this.mp=mpm.getMediaProjection(requestResultCode,mediaProjectionToken);
        DisplayMetrics dm = ApiServer.defaultAndroidContext.getResources().getDisplayMetrics();
        this.mp.createVirtualDisplay("pxprpc_virtual_display", dm.widthPixels,dm.heightPixels,dm.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,sur.androidSurface,null,null);
    }
    public void stopScreenCapture(){
        if(this.mp!=null){
            this.mp.stop();
        }
        this.mp=null;
    }

    public ByteBuffer takeScreenShot(AsyncReturn<ByteBuffer> retPngData){
        final ImageReader[] reader=new ImageReader[1];
        final ByteBuffer[] r=new ByteBuffer[1];
        new AsyncFuncChain().then((ctl)->{
            if(mediaProjectionToken==null){
                requestScreenCapture(new AsyncFuncChainPxprpcAdapter<>(null,ctl));
            }else{
                ctl.next();
            }
        }).then((ctl)->{
            Object[] size = DisplayManager2.i.getCurrentDisplaySize();
            reader[0] = SurfaceManager.i.newImageReader((Integer) size[0], (Integer) size[1], PixelFormat.RGBA_8888);
            if(mp!=null){
                ctl.throw2(new IllegalStateException(
                        "Other application is using MediaProjection ScreenCapture, stopScreenCapture before call this."));
                return;
            }
            startScreenCapture(SurfaceManager.i.newSurfaceFromImageReader(reader[0]));
            SurfaceManager.i.waitForImageAvailable(new AsyncFuncChainPxprpcAdapter<>(null,ctl),reader[0]);
        }).then((ctl)->{
            SurfaceManager.ImageOrBitmap image = SurfaceManager.i.accuireLastestImage(reader[0]);
            SurfaceManager.i.toPNG(new AsyncFuncChainPxprpcAdapter<>(r,ctl),image,85);
        }).then((ctl)->{
            stopScreenCapture();
            retPngData.resolve(r[0]);
        }).catch2((ex)->{
            stopScreenCapture();
            retPngData.reject(ex);
            return true;
        }).step();
        return null;
    }
    public ByteBuffer takeMainViewShot(AsyncReturn<ByteBuffer> aret){
        SurfaceManager.ImageOrBitmap[] img=new SurfaceManager.ImageOrBitmap[1];
        ByteBuffer[] buffer=new ByteBuffer[1];
        new AsyncFuncChain().then((ctl)->{
            DisplayManager2.i.viewReadPixels(
                    new AsyncFuncChainPxprpcAdapter<>(img,ctl),DisplayManager2.i.getCurrentMainView());
        }).then((ctl->{
            SurfaceManager.i.toPNG(new AsyncFuncChainPxprpcAdapter<>(buffer,ctl),img[0],85);
        })).then((ctl)->{
            aret.resolve(buffer[0]);
        }).catch2((err)->{
            aret.reject(err);
            return true;
        }).step();
        return null;
    }
    public boolean requestScreenCapture(final AsyncReturn<Boolean> ret){
        int reqCode= Env.i(Random.class).nextInt(0xffffff);
        ApiServer.onActivityResultCallback.put(reqCode,(param)->{
            requestResultCode=(Integer)param[1];
            if(requestResultCode==Activity.RESULT_OK){
                mediaProjectionToken=(Intent)param[2];
                Parcel param2=Parcel.obtain();
                Parcel result=Parcel.obtain();
                param2.writeInt(MediaProjection2.ServiceBinderCode);
                param2.writeInt(requestResultCode);
                param2.writeParcelable(mediaProjectionToken, 0);
                try {
                    ApiServer.serviceBinder.transact(reqCode,param2,result,0);
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
