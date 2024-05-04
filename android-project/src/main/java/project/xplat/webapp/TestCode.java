package project.xplat.webapp;

import android.app.AlertDialog;
import android.graphics.PixelFormat;
import android.hardware.camera2.CameraAccessException;
import android.media.ImageReader;
import android.util.Log;
import project.xplat.launcher.ApiServer;
import pxprpcapi.androidhelper.*;
import xplatj.javaplat.partic2.pxprpc.AsyncFuncChainPxprpcAdapter;
import xplatj.javaplat.partic2.util.AsyncFuncChain;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;



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
                AndroidCamera2.i.requestOnceCapture(new AsyncFuncChainPxprpcAdapter<>(null,ctl),openCam[0]);
            } catch (CameraAccessException e) {
                ctl.throw2(e);
            }
        }).then((ctl)->{
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            SurfaceManager.ImageOrBitmap img = AndroidCamera2.i.accuireLastestImageData(openCam[0]);
            SurfaceManager.i.toPNG(new AsyncFuncChainPxprpcAdapter<>(pngData,ctl),img,80);
        }).then((ctl)->{
            if(openCam[0]!=null){
                ApiServer.closeQuietly(openCam[0]);
                openCam[0]=null;
            }
            saveToFile("/sdcard/Download/test1.png",pngData[0]);
        }).catch2((e)->{
            e.printStackTrace();
            if(openCam[0]!=null){
                ApiServer.closeQuietly(openCam[0]);
                openCam[0]=null;
            }
        }).step();

    }
}
