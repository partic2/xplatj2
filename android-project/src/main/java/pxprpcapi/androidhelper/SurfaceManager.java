package pxprpcapi.androidhelper;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.os.Handler;
import android.view.Surface;
import project.xplat.launcher.ApiServer;
import pxprpc.extend.AsyncReturn;

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
}
