package project.xplat.launcher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import pxprpc.base.Serializer2;
import xplatj.javaplat.partic2.io.stream.StreamTransmit;
import xplatj.javaplat.partic2.util.CloseableGroup;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;

public class MainSysShareReceiver extends Activity {

    public static class Item{
        String uri;
        String fileName;
    }
    void saveFile(Uri uri){
        CloseableGroup toClose=new CloseableGroup();
        try {
            InputStream s1 = getContentResolver().openInputStream(uri);
            toClose.add(s1);
            File receiveDir = new File(getFilesDir(), "__receive");
            if(!receiveDir.exists()){
                receiveDir.mkdirs();
            }
            String filename="";
            for(int i=0;i<5000;i++) {
                filename = Integer.toString(new Random().nextInt(0x7fffffff),16);
                if(!new File(receiveDir,filename+".data").exists()){
                    break;
                }
            }
            FileOutputStream s2 = new FileOutputStream(receiveDir + File.separator + filename + ".data");
            new StreamTransmit().start(null,s1,s2,0x10000000,0x400,null);
            ByteBuffer meta = new Serializer2().prepareSerializing(64)
                    .putBytes(ByteBuffer.allocate(0))
                    .putString(Intent.ACTION_SEND)
                    .putString(uri.toString())
                    .putString("[END]")
                    .build();
            FileOutputStream s3=new FileOutputStream(receiveDir+File.separator+filename+".meta");
            toClose.add(s3);
            s3.write(meta.array(),meta.position(),meta.remaining());
        } catch (IOException e) {
            toClose.closeQuietly();
        }
    }
    public void showSavedMessage(){
        String lang = getResources().getConfiguration().locale.getLanguage();
        if(lang.equals("zh")){
            Toast.makeText(this,"文件已接收",Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this,"File received",Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if(intent.getAction()==Intent.ACTION_SEND){
            Uri uri = (Uri)intent.getExtras().get(Intent.EXTRA_STREAM);
            if(uri!=null){
                saveFile(uri);
                showSavedMessage();
            }
        }else if(intent.getAction()==Intent.ACTION_SEND_MULTIPLE){
            ArrayList<Uri> uris = (ArrayList<Uri>)intent.getExtras().get(Intent.EXTRA_STREAM);
            if(uris!=null){
                for(Uri uri:uris){
                    saveFile(uri);
                    showSavedMessage();
                }
            }
        }
        finish();
    }

}
