package pxprpcapi.androidhelper;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.camera2.*;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.util.Size;
import android.view.Surface;
import partic2.pxseedloader.android.launcher.ApiServer;
import pxprpc.base.Utils;
import pxprpc.extend.AsyncReturn;
import pxprpc.extend.BuiltInFuncList;
import pxprpc.extend.EventDispatcher;
import pxprpc.extend.TableSerializer;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/* bugly on some Android 5 Device, maybe fallback to Camera is unavoidable */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class AndroidCamera2 implements Closeable{
    public static final String PxprpcNamespace="AndroidHelper.Camera2";
    public static AndroidCamera2 i;
    public CameraManager camSrv;
    public String uid="";
    //Camera callbacks run on the ApiServer handler thread while close() may run on
    //a transport thread, so use a copy-on-write list for the opened resources.
    public List<Closeable> openedResource=new CopyOnWriteArrayList<Closeable>();
    
    public AndroidCamera2() {
    	camSrv=(CameraManager)ApiServer.defaultAndroidContext.getSystemService(Context.CAMERA_SERVICE);
        i=this;
    }
    public void closeAllOpenedResource() {
        //close() removes the resource from the list, iterate a snapshot.
    	for(Closeable cam:new ArrayList<Closeable>(openedResource)) {
    		ApiServer.closeQuietly(cam);
    	}
        openedResource.clear();
    }


    public ByteBuffer getBaseCamerasInfo() throws CameraAccessException {
        TableSerializer ser=new TableSerializer().setColumnsInfo("ssss",
                new String[]{"id","features","outputSizes","sensorActiveSize"});
        for(String id:camSrv.getCameraIdList()){
            CameraCharacteristics info = camSrv.getCameraCharacteristics(id);
            Object[] row = new Object[4];
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
            if(Boolean.TRUE.equals(info.get(CameraCharacteristics.FLASH_INFO_AVAILABLE))){
                features.add("flash");
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(Boolean.TRUE.equals(info.get(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE))){
                    features.add("ae_lock");
                }
                if(Boolean.TRUE.equals(info.get(CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE))){
                    features.add("af_lock");
                }
            }
            row[1]= Utils.stringJoin(" ",features);
            StreamConfigurationMap sscm = info.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizes = sscm==null?null:sscm.getOutputSizes(ImageFormat.YUV_420_888);
            if(sizes==null){
                row[2]="";
            }else{
                String[] sizeStr=new String[sizes.length];
                for(int i1=0;i1<sizes.length;i1++){
                    sizeStr[i1]=sizes[i1].getWidth()+"x"+sizes[i1].getHeight();
                }
                row[2]=Utils.stringJoin(",", Arrays.asList(sizeStr));
            }
            Rect size = info.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            row[3]=size==null?"":(size.right+"x"+size.bottom);
            ser.addRow(row);
        }
        return ser.build();
    }

    @Override
    public void close() throws IOException {
        if(i==this)i=null;
        this.closeAllOpenedResource();
    }

    public static class CameraWrap1 extends EventDispatcher implements Closeable{
        public CameraDevice wrapped;
        public CameraCaptureSession capSess;
        public CameraCaptureSession.CaptureCallback sessionEventHandler=new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                CameraWrap1.this.fireEvent("session.onCaptureStarted");
                super.onCaptureStarted(session,request,timestamp,frameNumber);
            }
            @Override
            public void onReadoutStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                CameraWrap1.this.fireEvent("session.onReadoutStarted");
                super.onReadoutStarted(session, request, timestamp, frameNumber);
            }
            @Override
            public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
                CameraWrap1.this.fireEvent("session.onCaptureProgressed");
                super.onCaptureProgressed(session, request, partialResult);
            }
            @Override
            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                CameraWrap1.this.fireEvent("session.onCaptureCompleted");
                super.onCaptureCompleted(session, request, result);
            }
            @Override
            public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
                CameraWrap1.this.fireEvent("session.onCaptureFailed");
                super.onCaptureFailed(session, request, failure);
            }
            @Override
            public void onCaptureSequenceCompleted(CameraCaptureSession session, int sequenceId, long frameNumber) {
                CameraWrap1.this.fireEvent("session.onCaptureSequenceCompleted");
                super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
            }
            @Override
            public void onCaptureSequenceAborted(CameraCaptureSession session, int sequenceId) {
                CameraWrap1.this.fireEvent("session.onCaptureSequenceAborted");
                super.onCaptureSequenceAborted(session, sequenceId);
            }
            @Override
            public void onCaptureBufferLost(CameraCaptureSession session, CaptureRequest request, Surface target, long frameNumber) {
                CameraWrap1.this.fireEvent("session.onCaptureBufferLost");
                super.onCaptureBufferLost(session, request, target, frameNumber);
            }
        };
        public SurfaceManager.SurfaceWrap renderTarget;
        public ImageReader imgRead;
        public int imageWidth;
        public int imageHeight;
        //-1 mean use template default value.
        public int flashMode=-1;
        public int autoFocusMode=-1;
        public CameraWrap1(CameraDevice wrapped){
            this.wrapped=wrapped;
            try {
                CameraCharacteristics info = AndroidCamera2.i.camSrv.getCameraCharacteristics(this.wrapped.getId());
                StreamConfigurationMap sscm = info.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Size[] sizes = sscm==null?null:sscm.getOutputSizes(ImageFormat.YUV_420_888);
                if(sizes!=null && sizes.length>0){
                    this.imageWidth=sizes[0].getWidth();
                    this.imageHeight=sizes[0].getHeight();
                }
            } catch (CameraAccessException e) {
            }
            if(AndroidCamera2.i!=null){
                AndroidCamera2.i.openedResource.add(this);
            }
            setEventType(String.class);
        }
        @Override
        public void close() throws IOException {
            CameraCaptureSession capSess2=this.capSess;
            ImageReader imgRead2=this.imgRead;
            CameraDevice dev=this.wrapped;
            this.capSess=null;
            this.imgRead=null;
            this.wrapped=null;
            if(dev!=null){
                if(capSess2!=null){
                    capSess2.close();
                }
                if(imgRead2!=null){
                    imgRead2.close();
                }
                dev.close();
            }else{
                if(capSess2!=null){
                    capSess2.close();
                }
                if(imgRead2!=null){
                    imgRead2.close();
                }
            }
            if(AndroidCamera2.i!=null){
                AndroidCamera2.i.openedResource.remove(this);
            }
        }
    }
    @SuppressLint("MissingPermission")
    public CameraWrap1 openCamera(final AsyncReturn<CameraWrap1> aret, String id) {
        try {
            final AtomicBoolean finished=new AtomicBoolean(false);
            camSrv.openCamera(id, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    if(!finished.compareAndSet(false,true)){
                        camera.close();
                        return;
                    }
                    aret.resolve(new CameraWrap1(camera));
                }

                @Override
                public void onDisconnected(CameraDevice camera) {
                    if(finished.compareAndSet(false,true)){
                        aret.reject(new IOException("Camera " + camera.getId() + " disconnected before it was opened"));
                    }
                }

                @Override
                public void onError(CameraDevice camera, int error) {
                    if(finished.compareAndSet(false,true)){
                        aret.reject(new Exception("Android Camera2 Error:" + error));
                    }
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

    /*
        //-1 mean use template default value
    */
    public void setCaptureConfig1(CameraWrap1 camWrap,
                                  int imageWidth,int imageHeight,
                                  int flashMode,int autoFocusMode) {
        if(imageWidth!=-1)camWrap.imageWidth=imageWidth;
        if(imageHeight!=-1)camWrap.imageHeight=imageHeight;
        camWrap.flashMode=flashMode;
        camWrap.autoFocusMode=autoFocusMode;
    }

    public void setRenderTarget(CameraWrap1 cam, SurfaceManager.SurfaceWrap sur){
        cam.renderTarget=sur;
    }
    public ByteBuffer getCaptureConfigKeyConst() throws IllegalAccessException {
        return new BuiltInFuncList().listStaticConstField(CaptureRequest.class);
    }
    public ByteBuffer getCaptureConfigValueConst() throws IllegalAccessException {
        return new BuiltInFuncList().listStaticConstField(CameraMetadata.class);
    }
    protected void prepareCaptureRequest(CameraWrap1 camWrap,CaptureRequest.Builder capReq){
        if(camWrap.flashMode!=-1)capReq.set(CaptureRequest.FLASH_MODE, camWrap.flashMode);
        if(camWrap.autoFocusMode!=-1)capReq.set(CaptureRequest.CONTROL_AF_MODE, camWrap.autoFocusMode);
        if(camWrap.renderTarget!=null){
            capReq.addTarget(camWrap.renderTarget.androidSurface);
        }
        if(camWrap.imgRead!=null){
            capReq.addTarget(camWrap.imgRead.getSurface());
        }
    }
    protected void ensureSession(CameraWrap1 cam){
        if(cam==null || cam.wrapped==null){
            throw new IllegalStateException("Camera is not opened");
        }
        if(cam.capSess==null){
            throw new IllegalStateException("Capture session is not configured, start a capture first");
        }
    }
    public void requestAutoFocusAndAdjust(final AsyncReturn<Object> aret, final CameraWrap1 camWrap,int x,int y,int width,int height) throws CameraAccessException {
        ensureSession(camWrap);
        MeteringRectangle focusWhere=new MeteringRectangle(x,y,width,height,0);
        CaptureRequest.Builder capReq = camWrap.wrapped.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        capReq.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_START);
        capReq.set(CaptureRequest.CONTROL_AF_REGIONS,new MeteringRectangle[]{focusWhere});
        capReq.set(CaptureRequest.CONTROL_AE_REGIONS,new MeteringRectangle[]{focusWhere});
        prepareCaptureRequest(camWrap,capReq);
        camWrap.capSess.capture(capReq.build(),null,ApiServer.getHandler());
        aret.resolve(null);
    }
    public void requestDigitScale(final AsyncReturn<Object> aret, final CameraWrap1 camWrap,int l,int t,int r,int b) throws CameraAccessException {
        ensureSession(camWrap);
        Rect rc=new Rect(l,t,r,b);
        CaptureRequest.Builder capReq = camWrap.wrapped.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        capReq.set(CaptureRequest.SCALER_CROP_REGION,rc);
        prepareCaptureRequest(camWrap,capReq);
        camWrap.capSess.capture(capReq.build(),null,ApiServer.getHandler());
        aret.resolve(null);
    }
    public void requestContinuousCapture(final AsyncReturn<Object> aret, final CameraWrap1 camWrap) throws CameraAccessException {
        if(camWrap==null || camWrap.wrapped==null){
            aret.reject(new IllegalStateException("Camera is not opened"));
            return;
        }
        final CameraDevice camDev = camWrap.wrapped;
        ImageReader imgReader = ImageReader.newInstance(camWrap.imageWidth, camWrap.imageHeight, ImageFormat.YUV_420_888, 2);
        if(camWrap.imgRead!=null)camWrap.imgRead.close();
        camWrap.imgRead=imgReader;
        try {
            List<Surface> tarSurf = new ArrayList<Surface>();
            tarSurf.add(imgReader.getSurface());
            if(camWrap.renderTarget!=null){
                tarSurf.add(camWrap.renderTarget.androidSurface);
            }
            camDev.createCaptureSession(tarSurf, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try{
                        if(camWrap.capSess!=null)camWrap.capSess.close();
                        CaptureRequest.Builder capReq = camWrap.wrapped.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        prepareCaptureRequest(camWrap,capReq);
                        session.setRepeatingRequest(capReq.build(),camWrap.sessionEventHandler,ApiServer.getHandler());
                        camWrap.capSess=session;
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
    }

    public void stopContinuousCapture(CameraWrap1 camWrap) throws CameraAccessException {
        if(camWrap==null || camWrap.capSess==null){
            return;
        }
        camWrap.capSess.stopRepeating();
    }
    public void requestOnceCapture(final AsyncReturn<Object> aret, final CameraWrap1 camWrap) throws CameraAccessException {
        if(camWrap==null || camWrap.wrapped==null){
            aret.reject(new IllegalStateException("Camera is not opened"));
            return;
        }
        final CameraDevice camDev = camWrap.wrapped;
        ImageReader imgReader = ImageReader.newInstance(camWrap.imageWidth, camWrap.imageHeight, ImageFormat.YUV_420_888, 2);
        if(camWrap.imgRead!=null)camWrap.imgRead.close();
        camWrap.imgRead=imgReader;
        try {
            List<Surface> tarSurf = new ArrayList<Surface>();
            tarSurf.add(imgReader.getSurface());
            if(camWrap.renderTarget!=null){
                tarSurf.add(camWrap.renderTarget.androidSurface);
            }
            camDev.createCaptureSession(tarSurf, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try{
                        if(camWrap.capSess!=null)camWrap.capSess.close();
                        CaptureRequest.Builder capReq = camWrap.wrapped.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                        prepareCaptureRequest(camWrap,capReq);
                        session.capture(capReq.build(), camWrap.sessionEventHandler, ApiServer.getHandler());
                        camWrap.capSess=session;
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
    }

    public SurfaceManager.ImageOrBitmap accuireLastestImageData(CameraWrap1 camDev){
        if(camDev==null || camDev.imgRead==null){
            throw new IllegalStateException("No ImageReader, start a capture first");
        }
        return SurfaceManager.i.accuireLastestImage(camDev.imgRead);
    }

    public ByteBuffer describePlanesInfo(SurfaceManager.ImageOrBitmap img){
        if(SurfaceManager.i==null){
            throw new IllegalStateException("SurfaceManager is not available");
        }
        return SurfaceManager.i.describePlanesInfo(img);
    }

    public Boolean waitForImageAvailable(AsyncReturn<Boolean> aret,CameraWrap1 camDev){
        if(camDev==null || camDev.imgRead==null){
            aret.reject(new IllegalStateException("No ImageReader, start a capture first"));
            return false;
        }
        SurfaceManager.i.waitForImageAvailable(aret,camDev.imgRead);
        return false;
    }

    public ByteBuffer getPlaneBufferData(SurfaceManager.ImageOrBitmap img, int planeIndex){
        return SurfaceManager.i.getPlaneBufferData(img,planeIndex);
    }

    public ByteBuffer packPlaneData(Image img){
        TableSerializer ser = new TableSerializer().setColumnsInfo("iib",new String[]{"pixelStride","rowStride","buffer"});
        for(Image.Plane e:img.getPlanes()){
            ser.addRow(new Object[]{
                    e.getPixelStride()
                    ,e.getRowStride()
            //Duplicate: putBytes consumes the buffer position, which would
            //disturb the camera's plane buffers.
            ,e.getBuffer().duplicate()});
        }
        return ser.build();
    }

}
