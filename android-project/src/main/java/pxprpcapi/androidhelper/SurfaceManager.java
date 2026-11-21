package pxprpcapi.androidhelper;

import android.graphics.*;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.GLES20;
import android.os.Handler;
import android.view.Surface;
import partic2.pxseedloader.android.launcher.ApiServer;
import pxprpc.base.Utils;
import pxprpc.extend.AsyncReturn;
import pxprpc.extend.BuiltInFuncList;
import pxprpc.extend.MethodTypeDecl;
import pxprpc.extend.TableSerializer;
import pxprpc.runtimebridge.NativeHelper;
import xplatj.javaplat.partic2.util.PlatCoreConfig;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
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
            ApiServer.resolveAsync(ret,r);
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
    //ImageReader only implements AutoCloseable, so freeing its pxprpc reference
    //does not close it. Call this explicitly to release camera/display buffers.
    public void closeImageReader(ImageReader reader){
        if(reader!=null){
            reader.close();
        }
    }
    @MethodTypeDecl("o->iiis")
    public Object[] getImageInfo2(ImageOrBitmap img){
        int format=0;
        int width=0;
        int height=0;
        String androidClass="";
        if(img==null){
            return new Object[]{format,width,height,androidClass};
        }
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
        return new TableSerializer().setColumnsInfo(
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
        if(reader==null){
            aret.reject(new IllegalStateException("ImageReader is null"));
            return false;
        }
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
        TableSerializer ser = new TableSerializer().setColumnsInfo("iiilii",
                new String[]{"pixelStride","rowStride","planeDataSize",
                        "buffer.base","buffer.offset","buffer.capacity"});
        if(img==null){
            throw new IllegalArgumentException("image is null");
        }
        if(img.image!=null){
            for(Image.Plane e:img.image.getPlanes()){
                long base=0;
                ByteBuffer planeBuf=e.getBuffer();
                if(planeBuf.isDirect()){
                    base=NativeHelper.directBufferProperty(planeBuf,0);
                }
                //'l' columns require Long objects; ByteBuffer.remaining()/getByteCount() are int.
                ser.addRow(new Object[]{
                        e.getPixelStride(),e.getRowStride(),(long)planeBuf.remaining()
                        ,base,planeBuf.position(),planeBuf.capacity()});
            }
        }else if(img.bitmap!=null){
            Bitmap.Config bcfg = img.bitmap.getConfig();
            int bytesPerPixel = 4;
            if(bcfg==Bitmap.Config.RGB_565 || bcfg==Bitmap.Config.ARGB_4444){
                bytesPerPixel=2;
            }
            ser.addRow(new Object[]{bytesPerPixel,img.bitmap.getRowBytes(),(long)img.bitmap.getByteCount(),
                    0L,0,0});
        }
        return ser.build();
    }

    public ByteBuffer getPlaneBufferData(ImageOrBitmap img,int planeIndex){
        if(img==null){
            throw new IllegalArgumentException("image is null");
        }
        if(img.image!=null){
            ByteBuffer buf1 = img.image.getPlanes()[planeIndex].getBuffer();
            return buf1;
        }else if(img.bitmap!=null){
            ByteBuffer buf1=ByteBuffer.allocate(img.bitmap.getByteCount());
            img.bitmap.copyPixelsToBuffer(buf1);
            Utils.flip(buf1);
            return buf1;
        }else{
            throw new RuntimeException("Unsupported image");
        }
    }
    //return buffer.base,buffer.offset,buffer.capacity
    @MethodTypeDecl("oi->lii")
    public Object[] getPlaneBufferDataProps(ImageOrBitmap img,int planeIndex){
        if(img==null || img.image==null){
            return new Object[]{0L,0,0};
        }
        Image.Plane[] planes=img.image.getPlanes();
        if(planeIndex<0 || planeIndex>=planes.length){
            return new Object[]{0L,0,0};
        }
        ByteBuffer buf1 = planes[planeIndex].getBuffer();
        if(buf1.isDirect()){
            return new Object[]{NativeHelper.directBufferProperty(buf1,0),buf1.position(),buf1.capacity()};
        }else{
            return new Object[]{(long)0,0,0};
        }
    }
    public ByteBuffer getPlaneBufferDataRange(ImageOrBitmap img,int planeIndex,int offset,int len){
        if(img==null || img.image==null){
            throw new IllegalStateException("Not an Image");
        }
        Image.Plane[] planes=img.image.getPlanes();
        if(planeIndex<0 || planeIndex>=planes.length){
            throw new IndexOutOfBoundsException("planeIndex:"+planeIndex);
        }
        ByteBuffer buf1=planes[planeIndex].getBuffer().duplicate();
        int base=buf1.position();
        int avail=buf1.remaining();
        if(offset<0)offset=0;
        if(offset>avail)offset=avail;
        if(len<0)len=0;
        if(offset+len>avail)len=avail-offset;
        buf1.position(base+offset);
        buf1.limit(base+offset+len);
        return buf1;
    }
    public ByteBuffer packPlaneData(List<Image.Plane> planes){
        TableSerializer ser = new TableSerializer().setColumnsInfo("iib",new String[]{"pixelStride","rowStride","buffer"});
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
                try{
                    if(img==null || (img.bitmap==null && img.image==null)){
                        throw new IllegalStateException("No image data");
                    }
                    if(img.bitmap==null){
                        img.bitmap=imageToBitmap(img.image);
                    }
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    if(!img.bitmap.compress(Bitmap.CompressFormat.PNG,quality,out)){
                        throw new IOException("Bitmap.compress failed");
                    }
                    pngData.resolve(ByteBuffer.wrap(out.toByteArray()));
                }catch(Exception ex){
                    pngData.reject(ex);
                }
            }
        });
        return null;
    }
    protected Bitmap imageToBitmap(Image image){
        int format=image.getFormat();
        int w=image.getWidth();
        int h=image.getHeight();
        if(format==PixelFormat.RGBA_8888 || format==ImageFormat.FLEX_RGBA_8888){
            Image.Plane[] planes=image.getPlanes();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * w;
            //Duplicate so the camera plane's position is not consumed.
            ByteBuffer src=planes[0].getBuffer().duplicate();
            src.rewind();
            Bitmap padded = Bitmap.createBitmap(w + rowPadding / pixelStride, h, Bitmap.Config.ARGB_8888);
            padded.copyPixelsFromBuffer(src);
            if(rowPadding==0){
                return padded;
            }
            Bitmap cropped=Bitmap.createBitmap(padded,0,0,w,h);
            padded.recycle();
            return cropped;
        }else if(format==ImageFormat.YUV_420_888){
            Image.Plane[] planes=image.getPlanes();
            ByteBuffer yp = planes[0].getBuffer().duplicate();
            int ybase=yp.position();
            int ystride=planes[0].getRowStride();
            ByteBuffer up = planes[1].getBuffer().duplicate();
            int ubase=up.position();
            int ustride=planes[1].getRowStride();
            int upstride=planes[1].getPixelStride();
            ByteBuffer vp = planes[2].getBuffer().duplicate();
            int vbase=vp.position();
            int vstride=planes[2].getRowStride();
            int vpstride=planes[2].getPixelStride();
            //Caution: pixels is in SRGB, different from PixelFormat.ARGB8888
            int[] pixels=new int[w*h];
            try{
                for(int y1=0;y1<h;y1++){
                    for(int x1=0;x1<w;x1++){
                        int y2=(yp.get(ybase+x1+y1*ystride)&0xff);
                        int u2=(up.get(ubase+(x1>>1)*upstride+(y1/2)*ustride)&0xff);
                        int v2=(vp.get(vbase+(x1>>1)*vpstride+(y1/2)*vstride)&0xff);
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
            return Bitmap.createBitmap(pixels,w , h, Bitmap.Config.ARGB_8888);
        }else{
            throw new IllegalArgumentException("only RGBA_8888 or YUV_420_888 Image are supported");
        }
    }
}
