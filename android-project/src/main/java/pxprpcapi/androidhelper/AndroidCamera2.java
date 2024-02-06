package pxprpcapi.androidhelper;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.util.Size;
import android.view.Surface;
import project.xplat.launcher.ApiServer;
import pxprpc.base.Utils;
import pxprpc.extend.AsyncReturn;
import pxprpc.extend.EventDispatcher;
import pxprpc.extend.TableSerializer;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class AndroidCamera2 {
    public static final String PxprpcNamespace="AndroidHelper.Camera2";

    public CameraManager camSrv;
    public String uid="";
    public ArrayList<Closeable> openedResource=new ArrayList<Closeable>();
    
    public AndroidCamera2() {
    	camSrv=(CameraManager)ApiServer.defaultAndroidContext.getSystemService(Context.CAMERA_SERVICE);
    }
    public void init() {}

    public void deinit() {
    	closeAllOpenedResource();
    }

    public void closeAllOpenedResource() {
    	for(Closeable cam:openedResource) {
    		ApiServer.closeQuietly(cam);
    	}
    }


    public ByteBuffer getBaseCamerasInfo() throws CameraAccessException {
        TableSerializer ser=new TableSerializer().setHeader("isb",new String[]{"id","features","outputSizes"});
        for(String id:camSrv.getCameraIdList()){
            CameraCharacteristics info = camSrv.getCameraCharacteristics(id);
            Object[] row = new Object[3];
            row[0]=id;
            ArrayList<String> features=new ArrayList<String>();
            int face=info.get(CameraCharacteristics.LENS_FACING);
            switch(face){
                case CameraCharacteristics.LENS_FACING_FRONT:
                    features.add("facing_front");
                    break;
                case CameraCharacteristics.LENS_FACING_BACK:
                    features.add("facing_back");
                    break;
            }
            if(info.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)){
                features.add("flash");
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(info.get(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE)){
                    features.add("ae_lock");
                }
                if(info.get(CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE)){
                    features.add("af_lock");
                }
            }
            row[1]= Utils.stringJoin(" ",features);
            TableSerializer ser2=new TableSerializer().setHeader("ii",new String[]{"w","h"});
            StreamConfigurationMap sscm = info.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizes = sscm.getOutputSizes(ImageFormat.YUV_420_888);
            for(Size e : sizes){
                ser2.addRow(new Object[]{e.getWidth(),e.getHeight()});
            }
            row[2]=Utils.toBytes(ser2.build());
            ser.addRow(row);
        }
        return ser.build();
    }

    public static class CameraWrap1 extends EventDispatcher implements Closeable{
        public CameraDevice wrapped;
        AndroidCamera2 ctx;
        CameraCaptureSession capSess;
        ImageReader imgRead;
        int imageWidth;
        int imageHeight;
        
        int flashMode=CaptureRequest.FLASH_MODE_OFF;
        int autoFocusMode=CaptureRequest.CONTROL_AF_MODE_AUTO;
        public CameraWrap1(AndroidCamera2 ctx, CameraDevice wrapped){
            this.ctx=ctx;
            this.wrapped=wrapped;
            ctx.openedResource.add(this);
        }
        @Override
        public void close() throws IOException {
            if(wrapped!=null){
                if(capSess!=null){
                    capSess.close();
                }
                if(imgRead!=null){
                    imgRead.close();
                }
                wrapped.close();
                wrapped=null;
            }
            this.ctx.openedResource.remove(this);
        }
    }
    @SuppressLint("MissingPermission")
    public Object openCamera(final AsyncReturn<Object> aret, String id) {
        try {
            camSrv.openCamera(id, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    CameraWrap1 c = new CameraWrap1(AndroidCamera2.this, camera);
                    aret.resolve(c);
                }

                @Override
                public void onDisconnected(CameraDevice camera) {
                }

                @Override
                public void onError(CameraDevice camera, int error) {
                    aret.reject(new Exception("Android Camera2 Error:" + error));
                }

            }, ApiServer.getHandler());
        }catch(Exception e){
            aret.reject(e);
        }
        return null;
    }
    public void closeCamera(CameraWrap1 cam){
        ApiServer.closeQuietly(cam);
    }

    //-1 means not set and use defualt value.
    public void setCaptureConfig1(CameraWrap1 camWrap,int imageWidth,int imageHeight,int flashMode,int autoFocusMode) {
    	if(imageWidth>=0) camWrap.imageWidth=imageWidth;
    	if(imageHeight>=0) camWrap.imageHeight=imageHeight;
    	camWrap.flashMode=flashMode;
    	camWrap.flashMode=autoFocusMode;
    }
    public Object requestContinuousCapture(final AsyncReturn<Object> aret, final CameraWrap1 camWrap) throws CameraAccessException {
        CameraDevice camDev = camWrap.wrapped;
        ImageReader imgReader = ImageReader.newInstance(camWrap.imageWidth, camWrap.imageHeight, ImageFormat.YUV_420_888, 2);
        if(camWrap.imgRead!=null)camWrap.imgRead.close();
        camWrap.imgRead=imgReader;
        
        try {
            List<Surface> tarSurf = new ArrayList<Surface>();
            tarSurf.add(imgReader.getSurface());
            camDev.createCaptureSession(tarSurf, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try{
                        if(camWrap.capSess!=null)camWrap.capSess.close();
                        CaptureRequest.Builder capReq = camWrap.wrapped.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        if(camWrap.flashMode>=0)capReq.set(CaptureRequest.FLASH_MODE, camWrap.flashMode);
                        if(camWrap.autoFocusMode>=0)capReq.set(CaptureRequest.CONTROL_AF_MODE, camWrap.autoFocusMode);
                        capReq.addTarget(camWrap.imgRead.getSurface());
                        session.setRepeatingRequest(capReq.build(),null,ApiServer.handler);
                        aret.resolve(null);
                    }catch (Exception e) {
                        aret.reject(e);
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    aret.reject(new RuntimeException("createCaptureSession failed."));
                }
            },ApiServer.getHandler());
        } catch (Exception e) {
            aret.reject(e);
        }
        return null;
    }

    public void stopContinuousCapture(CameraWrap1 camWrap) throws CameraAccessException {
        camWrap.capSess.stopRepeating();
        camWrap.capSess=null;
    }

    public Object requestOnceCapture(final AsyncReturn<Object> aret, final CameraWrap1 camWrap) throws CameraAccessException {
        final CameraDevice camDev = camWrap.wrapped;
        ImageReader imgReader = ImageReader.newInstance(camWrap.imageWidth, camWrap.imageHeight, ImageFormat.YUV_420_888, 2);
        if(camWrap.imgRead!=null)camWrap.imgRead.close();
        camWrap.imgRead=imgReader;
        try {
            List<Surface> tarSurf = new ArrayList<Surface>();
            tarSurf.add(imgReader.getSurface());
            camDev.createCaptureSession(tarSurf, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try{
                        if(camWrap.capSess!=null)camWrap.capSess.close();
                        CaptureRequest.Builder capReq = camWrap.wrapped.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                        if(camWrap.flashMode>=0)capReq.set(CaptureRequest.FLASH_MODE, camWrap.flashMode);
                        if(camWrap.autoFocusMode>=0)capReq.set(CaptureRequest.CONTROL_AF_MODE, camWrap.autoFocusMode);
                        capReq.addTarget(camWrap.imgRead.getSurface());
                        session.capture(capReq.build(), null, ApiServer.handler);
                        aret.resolve(null);
                    }catch (Exception e) {
                        aret.reject(e);
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    aret.reject(new RuntimeException("createCaptureSession failed."));
                }
            },ApiServer.getHandler());
        } catch (Exception e) {
            aret.reject(e);
        }
        return null;
    }

    public Image accuireLastestImageData(CameraWrap1 camDev){
        return camDev.imgRead.acquireLatestImage();
    }

    public ByteBuffer describePlanesInfo(Image img){
        return ApiServer.surfaceManager.describePlanesInfo(img);
    }

    public ByteBuffer getPlaneBufferData(Image img,int planeIndex){
        return ApiServer.surfaceManager.getPlaneBufferData(img,planeIndex);
    }

    public ByteBuffer packPlaneData(Image img){
        TableSerializer ser = new TableSerializer().setHeader("ii",new String[]{"pixelStride","rowStride","buffer"});
        for(Image.Plane e:img.getPlanes()){
            ser.addRow(new Object[]{
                    e.getPixelStride()
                    ,e.getRowStride()
            ,e.getBuffer()});
        }
        return ser.build();
    }

}
