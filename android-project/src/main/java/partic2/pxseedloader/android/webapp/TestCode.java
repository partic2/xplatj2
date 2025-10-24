package partic2.pxseedloader.android.webapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.hardware.camera2.CameraAccessException;
import android.view.Surface;
import android.view.TextureView;
import partic2.pxseedloader.android.launcher.ApiServer;
import pxprpcapi.androidhelper.*;
import xplatj.javaplat.partic2.util.PlatCoreConfig;
import xplatj.javaplat.partic2.pxprpc.AsyncFuncChainPxprpcAdapter;
import xplatj.javaplat.partic2.util.AsyncFuncChain;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;


public class TestCode {
    public static void do2(){
        cameraCapture();
    }
    public static void viewshot(){
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
        ByteBuffer[] pngData = new ByteBuffer[]{null};
        new AsyncFuncChain().then((ctl)->{
            MediaProjection2.i.takeMainViewShot(new AsyncFuncChainPxprpcAdapter<>(pngData,ctl));
        }).then((ctl)->{
            saveToFile("/sdcard/Download/test1.png",pngData[0]);
        }).step();
    }
    public static void screenshot(){
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }
        ByteBuffer[] pngData = new ByteBuffer[]{null};
        SurfaceManager.ImageOrBitmap[] img=new SurfaceManager.ImageOrBitmap[1];
        new AsyncFuncChain().then((ctl)->{
            DisplayManager2.i.viewReadPixels(new AsyncFuncChainPxprpcAdapter<>(img,ctl),DisplayManager2.i.getCurrentMainView());
        }).then((ctl)->{
            saveToFile("/sdcard/Download/test1.png",pngData[0]);
        }).step();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }
        new AsyncFuncChain().then((ctl)->{
            MediaProjection2.i.takeScreenShot(new AsyncFuncChainPxprpcAdapter<>(pngData,ctl));
        }).then((ctl)->{
            saveToFile("/sdcard/Download/test1.png",pngData[0]);
        }).step();

    }
    public static void saveToFile(String fileName,ByteBuffer b){
        try(RandomAccessFile raf=new RandomAccessFile(fileName,"rw")){
            raf.setLength(0);
            raf.write(b.array());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void showDialog(){
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
        }
        //while(AndroidUIBase.i==null){}
        AlertDialog[] dialog=new AlertDialog[1];
        new AsyncFuncChain().then((ctl)->{
            AndroidUIBase.i.dialogNew(new AsyncFuncChainPxprpcAdapter<>(dialog,ctl),"","","","");
        }).then((ctl)->{
            AndroidUIBase.i.dialogSet(dialog[0],"Hello",true);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
            }
            AndroidUIBase.i.dialogSet(dialog[0],"Hello",true);
        }).step();

    }
    public static void cameraCapture(){
        AndroidCamera2.CameraWrap1[] openCam=new AndroidCamera2.CameraWrap1[1];
        ByteBuffer[] pngData = new ByteBuffer[1];
        TextureView[] camOutput=new TextureView[1];
        AndroidUIBase.i.mainTaskQueue.post(()->{
            Activity acti=((Activity) ApiServer.defaultAndroidContext);
            TextureView cameraOutput=new TextureView(acti);
            camOutput[0]=cameraOutput;
            acti.setContentView(cameraOutput);
        });
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
        new AsyncFuncChain().then((ctl)->{
            try {
                String[] idlist = AndroidCamera2.i.camSrv.getCameraIdList();
                AndroidCamera2.i.openCamera(new AsyncFuncChainPxprpcAdapter<>(openCam,ctl),idlist[0]);
            } catch (CameraAccessException e) {
                ctl.throw2(e);
            }
        }).then((ctl)->{
            try {
                AndroidCamera2.i.setCaptureConfig1(openCam[0],640,480,-1,-1);
                SurfaceManager.SurfaceWrap sur = new SurfaceManager.SurfaceWrap();
                sur.tex=camOutput[0].getSurfaceTexture();
                sur.tex.setDefaultBufferSize(640,480);
                sur.androidSurface=new Surface(camOutput[0].getSurfaceTexture());
                AndroidCamera2.i.setRenderTarget(openCam[0],sur);
                AndroidCamera2.i.requestContinuousCapture(new AsyncFuncChainPxprpcAdapter<>(null,ctl),openCam[0]);
            } catch (CameraAccessException e) {
                ctl.throw2(e);
            }
        }).then((ctl)->{
            PlatCoreConfig.get().executor.schedule(()->ctl.next(),2000, TimeUnit.MILLISECONDS);
        }).then((ctl)->{
            try {
                AndroidCamera2.i.requestAutoFocusAndAdjust(new AsyncFuncChainPxprpcAdapter<>(null,ctl),openCam[0],2000,2000,100,100);
            } catch (CameraAccessException e) {
                ctl.throw2(e);
            }
        }).then((ctl)->{
            PlatCoreConfig.get().executor.schedule(()->ctl.next(),3000, TimeUnit.MILLISECONDS);
        }).then((ctl)->{
            try {
                SurfaceManager.ImageOrBitmap img = AndroidCamera2.i.accuireLastestImageData(openCam[0]);
                if(img!=null)img.close();
                img = AndroidCamera2.i.accuireLastestImageData(openCam[0]);
                if(img!=null)img.close();
                img = AndroidCamera2.i.accuireLastestImageData(openCam[0]);
                if(img!=null)img.close();
                img = AndroidCamera2.i.accuireLastestImageData(openCam[0]);
                if(img!=null)img.close();
            } catch (IOException e) {
            }
            SurfaceManager.i.waitForImageAvailable(new AsyncFuncChainPxprpcAdapter<>(null,ctl),openCam[0].imgRead);
        }).then(ctl->{
            SurfaceManager.ImageOrBitmap img = AndroidCamera2.i.accuireLastestImageData(openCam[0]);
            SurfaceManager.i.toPNG(new AsyncFuncChainPxprpcAdapter<>(pngData,ctl),img,80);
        }).then((ctl)->{
            if(openCam[0]!=null){
                ApiServer.closeQuietly(openCam[0]);
                openCam[0]=null;
            }
            saveToFile("/sdcard/Download/test1.png",pngData[0]);
            Intent2.i.requestOpenUniversalTypeFile("/sdcard/Download/test1.png");
        }).catch2((e)->{
            e.printStackTrace();
            if(openCam[0]!=null){
                ApiServer.closeQuietly(openCam[0]);
                openCam[0]=null;
            }
        }).step();

    }
}
