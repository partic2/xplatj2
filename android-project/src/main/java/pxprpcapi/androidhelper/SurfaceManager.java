package pxprpcapi.androidhelper;

import android.annotation.TargetApi;
import android.graphics.*;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Handler;
import android.view.Surface;
import project.xplat.launcher.ApiServer;
import pxprpc.extend.AsyncReturn;
import pxprpc.extend.BuiltInFuncList;
import pxprpc.extend.MethodTypeDecl;
import pxprpc.extend.TableSerializer;
import xplatj.gdxconfig.core.PlatCoreConfig;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.List;

public class SurfaceManager implements Closeable{
    public static final String PxprpcNamespace="AndroidHelper.SurfaceManager";
    public static SurfaceManager i;
    public SurfaceManager(){
        super();
        i=this;
    }

    @Override
    public void close() throws IOException {
        if(i==this)i=null;
    }

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
    public SurfaceWrap newSurface(final AsyncReturn<SurfaceWrap> ret, int width, int height){
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
    public ByteBuffer listPixelFormatConst() throws IllegalAccessException {
        return new BuiltInFuncList().listStaticConstField(PixelFormat.class);
    }
    public ImageReader newImageReader(int width,int height,int format){
        return ImageReader.newInstance(width,height,format,2);
    }
    @MethodTypeDecl("o->iiis")
    public Object[] getImageInfo2(ImageOrBitmap img){
        int format=0;
        int width=0;
        int height=0;
        String androidClass="";
        if(img.image!=null){
            format=img.image.getFormat();
            width=img.image.getWidth();
            height=img.image.getHeight();
            androidClass="Image";
        }else if(img.bitmap!=null){
            Bitmap.Config bitConfig = img.bitmap.getConfig();
            if(bitConfig==Bitmap.Config.ARGB_8888){
                format=PixelFormat.RGBA_8888;
            }else if(bitConfig==Bitmap.Config.RGB_565){
                format=PixelFormat.RGB_565;
            }else if(bitConfig==Bitmap.Config.ARGB_4444){
                format=PixelFormat.RGBA_4444;
            };
            width=img.bitmap.getWidth();
            height=img.bitmap.getHeight();
            androidClass="Bitmap";
        }
        return new Object[]{format,width,height,androidClass};
    }
    public ByteBuffer getImageInfo(ImageOrBitmap img){
        return new TableSerializer().setHeader(
                null,new String[]{"format","width","height","androidClass"})
                .addRow(this.getImageInfo2(img)).build();
    }
    public SurfaceWrap newSurfaceFromImageReader(ImageReader reader){
        SurfaceWrap sur=new SurfaceWrap();
        sur.androidSurface=reader.getSurface();
        return sur;
    }

    public static class ImageOrBitmap implements Closeable {
        Image image;
        Bitmap bitmap;
        @Override
        public void close() throws IOException {
            if(image!=null){
                image.close();
            }
            if(bitmap!=null){
                bitmap.recycle();
            }
        }
    }

    public Boolean waitForImageAvailable(AsyncReturn<Boolean> aret,ImageReader reader){
        reader.setOnImageAvailableListener((reader2)->{
            reader2.setOnImageAvailableListener(null,null);
            aret.resolve(true);
        },ApiServer.getHandler());
        return false;
    }
    public ImageOrBitmap accuireLastestImage(ImageReader reader){
        final ImageOrBitmap r = new ImageOrBitmap();
        r.image=reader.acquireLatestImage();
        if(r.image==null){
            return null;
        }
        return r;
    }
    public ImageOrBitmap acquireNextImage(ImageReader reader){
        ImageOrBitmap r = new ImageOrBitmap();
        r.image=reader.acquireNextImage();
        if(r.image==null){
            return null;
        }
        return r;
    }

    public ByteBuffer describePlanesInfo(ImageOrBitmap img){
        TableSerializer ser = new TableSerializer().setHeader("ii",
                new String[]{"pixelStride","rowStride"});
        if(img.image!=null){
            for(Image.Plane e:img.image.getPlanes()){
                ser.addRow(new Object[]{
                        e.getPixelStride()
                        ,e.getRowStride()});
            }
        }else if(img.bitmap!=null){
            Bitmap.Config bcfg = img.bitmap.getConfig();
            int pixelStride = 0;
            if(bcfg==Bitmap.Config.ARGB_8888){
                pixelStride=32;
            }else if(bcfg==Bitmap.Config.RGB_565 || bcfg==Bitmap.Config.ARGB_4444){
                pixelStride=16;
            }
            ser.addRow(new Object[]{pixelStride,img.bitmap.getWidth()*pixelStride});
        }
        return ser.build();
    }

    public ByteBuffer getPlaneBufferData(ImageOrBitmap img,int planeIndex){
        ByteBuffer buf1 = img.image.getPlanes()[planeIndex].getBuffer();
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

    public ByteBuffer toPNG(AsyncReturn<ByteBuffer> pngData,ImageOrBitmap img,int quality){
        PlatCoreConfig.get().executor.execute(new Runnable() {
            @Override
            public void run() {
                if(img.bitmap==null && img.image!=null){
                    if(img.image.getFormat()==PixelFormat.RGBA_8888){
                        Image.Plane[] planes=img.image.getPlanes();
                        int pixelStride = planes[0].getPixelStride();
                        int rowStride = planes[0].getRowStride();
                        int rowPadding = rowStride - pixelStride * img.image.getWidth();

                        img.bitmap = Bitmap.createBitmap(img.image.getWidth() + rowPadding / pixelStride, img.image.getHeight(),
                                Bitmap.Config.ARGB_8888);
                        img.bitmap.copyPixelsFromBuffer(planes[0].getBuffer());
                    }else if(img.image.getFormat()==ImageFormat.YUV_420_888){
                        Image.Plane[] planes=img.image.getPlanes();
                        ByteBuffer yp = planes[0].getBuffer();
                        int ystride=planes[0].getRowStride();
                        ByteBuffer up = planes[1].getBuffer();
                        int ustride=planes[1].getRowStride();
                        int upstride=planes[1].getPixelStride();
                        ByteBuffer vp = planes[2].getBuffer();
                        int vstride=planes[2].getRowStride();
                        int vpstride=planes[2].getPixelStride();
                        int w=img.image.getWidth();
                        int h=img.image.getHeight();
                        //Caution: pixels is in SRGB, different from PixelFormat.ARGB8888
                        int[] pixels=new int[w*h];
                        try{
                            for(int y1=0;y1<h;y1++){
                                for(int x1=0;x1<w;x1++){
                                    int y2=(yp.get(yp.position()+x1+y1*ystride)&0xff);
                                    int u2=(up.get(up.position()+x1*upstride/2+(y1/2)*ustride)&0xff);
                                    int v2=(vp.get(vp.position()+x1*vpstride/2+(y1/2)*vstride)&0xff);
                                    int r=(298*(y2-16)+409*(v2-128))>>8;
                                    int g=(298*(y2-16)-100*(u2-128)-208*(v2-128))>>8;
                                    int b=(298*(y2-16)+517*(u2-128))>>8;
                                    if(r>255)r=255;
                                    if(g>255)g=255;
                                    if(b>255)b=255;
                                    if(r<0)r=0;
                                    if(g<0)g=0;
                                    if(b<0)b=0;
                                    pixels[y1*w+x1]=(0xff << 24) | (r<< 16) | (g<< 8) | b;
                                }
                            }
                        }catch(IndexOutOfBoundsException e){
                            /* UV Buffer size is smaller than image size on XiaoMi 8 (WHAT?).
                                In this case, we can only discard the last pixel. */
                        }
                        img.bitmap = Bitmap.createBitmap(pixels,w , h, Bitmap.Config.ARGB_8888);
                    }else{
                        throw new Error("only RGBA_8888 or YUV_420_888 Image are supported");
                    }
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                img.bitmap.compress(Bitmap.CompressFormat.PNG,quality,out);
                pngData.resolve(ByteBuffer.wrap(out.toByteArray()));
            }
        });
        return null;
    }
}
