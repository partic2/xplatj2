package project.xplat.webapp;

import android.graphics.PixelFormat;
import android.media.ImageReader;
import pxprpcapi.androidhelper.DisplayManager2;
import pxprpcapi.androidhelper.MediaProjection2;
import pxprpcapi.androidhelper.SurfaceManager;
import xplatj.javaplat.partic2.pxprpc.AsyncFuncChainPxprpcAdapter;
import xplatj.javaplat.partic2.util.AsyncFuncChain;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;



public class TestCode {
    public static void do2(){
        viewshot();
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
            saveToFile("/sdcard/test1.png",pngData[0]);
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
            saveToFile("/sdcard/test1.png",pngData[0]);
        }).step();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }
        new AsyncFuncChain().then((ctl)->{
            MediaProjection2.i.takeScreenShot(new AsyncFuncChainPxprpcAdapter<>(pngData,ctl));
        }).then((ctl)->{
            saveToFile("/sdcard/test2.png",pngData[0]);
        }).step();

    }
    public static void saveToFile(String fileName,ByteBuffer b){
        try(RandomAccessFile raf=new RandomAccessFile(fileName,"rw")){
            raf.write(b.array());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
