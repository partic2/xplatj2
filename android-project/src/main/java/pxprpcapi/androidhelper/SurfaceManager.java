package pxprpcapi.androidhelper;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.opengl.GLES20;
import android.os.Handler;
import android.view.Surface;
import project.xplat.launcher.ApiServer;
import pxprpc.extend.AsyncReturn;
import pxprpc.extend.BuiltInFuncList;
import pxprpc.extend.TableSerializer;

import java.nio.ByteBuffer;
import java.util.List;

public class SurfaceManager {
    public static final String PxprpcNamespace="AndroidHelper.SurfaceManager";
    public static class SurfaceWrap implements SurfaceTexture.OnFrameAvailableListener {
        public Surface androidSurface;
        //prevent reclaimed
        public SurfaceTexture tex;
        public int texName;

        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            surfaceTexture.updateTexImage();
        }
    }
    public SurfaceWrap newSurface(final AsyncReturn<SurfaceWrap> ret,int width,int height){
        new Handler(ApiServer.defaultAndroidContext.getMainLooper()).post(()->{
            int[] gltex=new int[1];
            GLES20.glGenTextures(1,gltex,0);
            SurfaceTexture tex = new SurfaceTexture(gltex[0]);
            SurfaceWrap r = new SurfaceWrap();
            r.androidSurface=new Surface(tex);
            r.tex=tex;
            r.texName=gltex[0];
            r.tex.setDefaultBufferSize(width,height);
            r.tex.setOnFrameAvailableListener(r,new Handler(ApiServer.defaultAndroidContext.getMainLooper()));
            ret.resolve(r);
        });
        return null;
    }
    public int getOpenglTexName(SurfaceWrap sur){
        return sur.texName;
    }
    public ByteBuffer listImageFormatConst() throws IllegalAccessException {
        return new BuiltInFuncList().listStaticConstField(ImageFormat.class);
    }
    public ImageReader newImageReader(int width,int height,int format){
        return ImageReader.newInstance(width,height,format,2);
    }
    public ByteBuffer getImageInfo(Image img){
        return new TableSerializer().setHeader(null,new String[]{"format","width","height"}).addRow(new Object[]{
                img.getFormat(),img.getWidth(),img.getHeight()
        }).build();
    }
    public SurfaceWrap newSurfaceFromImageReader(ImageReader reader){
        SurfaceWrap sur=new SurfaceWrap();
        sur.androidSurface=reader.getSurface();
        return sur;
    }


    public Image accuireLastestImage(ImageReader reader){
        return reader.acquireLatestImage();
    }
    public Image acquireNextImage(ImageReader reader){
        return reader.acquireNextImage();
    }

    public ByteBuffer describePlanesInfo(Image img){
        TableSerializer ser = new TableSerializer().setHeader("ii",new String[]{"pixelStride","rowStride"});
        for(Image.Plane e:img.getPlanes()){
            ser.addRow(new Object[]{
                    e.getPixelStride()
                    ,e.getRowStride()});
        }
        return ser.build();
    }

    public ByteBuffer getPlaneBufferData(Image img,int planeIndex){
        ByteBuffer buf1 = img.getPlanes()[planeIndex].getBuffer();
        return buf1;
    }
    public ByteBuffer packPlaneData(List<Image.Plane> planes){
        TableSerializer ser = new TableSerializer().setHeader("iib",new String[]{"pixelStride","rowStride","buffer"});
        for(Image.Plane e:planes){
            ser.addRow(new Object[]{
                    e.getPixelStride()
                    ,e.getRowStride()
                    ,e.getBuffer()});
        }
        return ser.build();
    }


}
