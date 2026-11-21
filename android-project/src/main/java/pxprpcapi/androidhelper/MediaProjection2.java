package pxprpcapi.androidhelper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import partic2.pxseedloader.android.launcher.ApiServer;
import pxprpc.extend.AsyncReturn;
import xplatj.javaplat.partic2.pxprpc.AsyncFuncChainPxprpcAdapter;
import xplatj.javaplat.partic2.util.AsyncFuncChain;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class MediaProjection2 implements Closeable {
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
    protected VirtualDisplay virtualDisplay;

    //Since Android 14 a MediaProjection.Callback must be registered before
    //createVirtualDisplay, otherwise it throws SecurityException.
    protected final MediaProjection.Callback projectionCallback=new MediaProjection.Callback(){
        @Override
        public void onStop() {
            //The system stopped the projection (user revoked it, display changed...).
            releaseVirtualDisplay();
            mp=null;
        }
    };

    protected DisplayMetrics getRealDisplayMetrics(){
        DisplayMetrics metrics=new DisplayMetrics();
        WindowManager wm=(WindowManager)ApiServer.defaultAndroidContext.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getRealMetrics(metrics);
        return metrics;
    }

    public void startScreenCapture(SurfaceManager.SurfaceWrap sur){
        if(sur==null){
            throw new IllegalArgumentException("SurfaceWrap is null");
        }
        if(mp!=null){
            throw new IllegalStateException("Screen capture is already running, call stopScreenCapture first.");
        }
        if(mediaProjectionToken==null){
            throw new IllegalStateException("No screen capture permission, call requestScreenCapture first.");
        }
        MediaProjection newMp=mpm.getMediaProjection(requestResultCode,mediaProjectionToken);
        if(newMp==null){
            throw new IllegalStateException("getMediaProjection returned null, permission was denied?");
        }
        //A capture token is single-use (enforced since Android 14); force a new
        //consent flow the next time screen capture is requested.
        mediaProjectionToken=null;
        newMp.registerCallback(projectionCallback,ApiServer.getHandler());
        DisplayMetrics dm=getRealDisplayMetrics();
        try{
            this.mp=newMp;
            this.virtualDisplay=newMp.createVirtualDisplay("pxprpc_virtual_display",dm.widthPixels,dm.heightPixels,dm.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,sur.androidSurface,null,null);
        }catch(RuntimeException ex){
            this.mp=null;
            releaseVirtualDisplay();
            try{
                newMp.unregisterCallback(projectionCallback);
                newMp.stop();
            }catch(Exception ignored){}
            throw ex;
        }
    }

    protected void releaseVirtualDisplay(){
        if(this.virtualDisplay!=null){
            this.virtualDisplay.release();
            this.virtualDisplay=null;
        }
    }

    public void stopScreenCapture(){
        releaseVirtualDisplay();
        MediaProjection cur=this.mp;
        this.mp=null;
        if(cur!=null){
            try{
                cur.unregisterCallback(projectionCallback);
            }catch(Exception ignored){}
            cur.stop();
        }
    }

    public ByteBuffer takeScreenShot(AsyncReturn<ByteBuffer> retPngData){
        final ImageReader[] reader=new ImageReader[1];
        final ByteBuffer[] r=new ByteBuffer[1];
        final SurfaceManager.ImageOrBitmap[] image=new SurfaceManager.ImageOrBitmap[1];
        new AsyncFuncChain().then((ctl)->{
            if(mediaProjectionToken==null){
                requestScreenCapture(new AsyncFuncChainPxprpcAdapter<>(null,ctl));
            }else{
                ctl.next();
            }
        }).then((ctl)->{
            if(mp!=null){
                ctl.throw2(new IllegalStateException(
                        "Other application is using MediaProjection ScreenCapture, stopScreenCapture before call this."));
                return;
            }
            Object[] size = DisplayManager2.i.getCurrentDisplaySize();
            reader[0] = SurfaceManager.i.newImageReader((Integer) size[0], (Integer) size[1], PixelFormat.RGBA_8888);
            //Register the listener before the first frame can arrive.
            SurfaceManager.i.waitForImageAvailable(new AsyncFuncChainPxprpcAdapter<>(null,ctl),reader[0]);
            startScreenCapture(SurfaceManager.i.newSurfaceFromImageReader(reader[0]));
        }).then((ctl)->{
            image[0] = SurfaceManager.i.accuireLastestImage(reader[0]);
            SurfaceManager.i.toPNG(new AsyncFuncChainPxprpcAdapter<>(r,ctl),image[0],85);
        }).then((ctl)->{
            ApiServer.closeQuietly(image[0]);
            if(reader[0]!=null)reader[0].close();
            stopScreenCapture();
            ApiServer.resolveAsync(retPngData,r[0]);
        }).catch2((ex)->{
            if(image[0]!=null){
                ApiServer.closeQuietly(image[0]);
            }
            if(reader[0]!=null)reader[0].close();
            stopScreenCapture();
            ApiServer.rejectAsync(retPngData,ex);
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
            ApiServer.closeQuietly(img[0]);
            ApiServer.resolveAsync(aret,buffer[0]);
        }).catch2((err)->{
            if(img[0]!=null)ApiServer.closeQuietly(img[0]);
            ApiServer.rejectAsync(aret,err);
        }).step();
        return null;
    }
    protected static final AtomicInteger requestCodeGen=new AtomicInteger(0x2000);
    public boolean requestScreenCapture(final AsyncReturn<Boolean> ret){
        if(!(ApiServer.defaultAndroidContext instanceof Activity)){
            ApiServer.resolveAsync(ret,false);
            return false;
        }
        final int reqCode=requestCodeGen.incrementAndGet();
        //ApiServer removes the callback before dispatching, so it is one-shot.
        ApiServer.onActivityResultCallback.put(reqCode,(param)->{
            this.requestResultCode=(Integer)param[1];
            if(requestResultCode==Activity.RESULT_OK){
                this.mediaProjectionToken=(Intent)param[2];
                ApiServer.resolveAsync(ret,true);
            }else{
                ApiServer.resolveAsync(ret,false);
            }
            return true;
        });
        ((Activity)ApiServer.defaultAndroidContext).startActivityForResult(mpm.createScreenCaptureIntent(),reqCode);
        return false;
    }

    @Override
    public void close() throws IOException {
        stopScreenCapture();
        if(i==this)i=null;
    }
}
