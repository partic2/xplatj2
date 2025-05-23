package pxprpcapi.androidhelper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.Settings;
import android.webkit.MimeTypeMap;
import de.cketti.fileprovider.PublicFileProvider;
import project.xplat.launcher.ApiServer;
import pxprpc.extend.AsyncReturn;
import xplatj.gdxplat.partic2.utils.Env;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;

public class Intent2 implements Closeable {
    public static final String PxprpcNamespace="AndroidHelper.Intent";
    public static Intent2 i;
    public Intent2(){
        i=this;
    }
    public void requestInstallApk(String apkPath){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(Uri.parse(getUriForFile(apkPath)), "application/vnd.android.package-archive");
        ApiServer.defaultAndroidContext.startActivity(intent);
    }
    public void requestOpenTelephone(String tel){
        Uri uri=Uri.parse("tel:"+tel);
        Intent intent=new Intent(Intent.ACTION_DIAL,uri);
        ApiServer.defaultAndroidContext.startActivity(intent);
    }
    public void requestSendShortMessage(String tel,String body){
        Uri uri=Uri.parse("smsto:"+tel);
        Intent intent=new Intent(Intent.ACTION_SENDTO,uri);
        intent.putExtra("sms_body",body);
        ApiServer.defaultAndroidContext.startActivity(intent);
    }
    public void requestSendOthers(String filePath,String mime,String chooserTitle){
        Uri uri = Uri.parse(getUriForFile(filePath));
        if(mime==""){
            mime=getMimeTypeFromUri(uri.toString());
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri.toString());
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Share");
        ApiServer.defaultAndroidContext.startActivity(Intent.createChooser(shareIntent, chooserTitle));
    }
    public void requestOpenByDefaultHandler(String uris){
        Uri uri=Uri.parse(uris);
        Intent intent=new Intent(Intent.ACTION_VIEW,uri);
        ApiServer.defaultAndroidContext.startActivity(intent);
    }
    public void requestOpenSetting(String setting){
        Intent intent=new Intent("android.settings."+setting);
        ApiServer.defaultAndroidContext.startActivity(intent);
    }
    public String[] getSettingProviderList(){
        return new String[]{
                Settings.ACTION_ACCESSIBILITY_SETTINGS,
                Settings.ACTION_HOME_SETTINGS,
                Settings.ACTION_NETWORK_OPERATOR_SETTINGS,
                Settings.ACTION_WIFI_SETTINGS,
                Settings.ACTION_WIFI_IP_SETTINGS,
                Settings.ACTION_BLUETOOTH_SETTINGS,
                Settings.ACTION_DATE_SETTINGS,
                Settings.ACTION_DISPLAY_SETTINGS
        };
    }
    public void requestOpenApplication(String packageName,String componentName,String action){
        Intent intent=new Intent();
        ComponentName cn=new ComponentName(packageName,componentName);
        intent.setComponent(cn);
        if("".equals(action)){
            action="android.intent.action.MAIN";
        }
        intent.setAction(action);
        ApiServer.defaultAndroidContext.startActivity(intent);
    }
    @SuppressLint("MissingPermission")
    public void requestEnableBluetooth(){
        Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        ApiServer.defaultAndroidContext.startActivity(intent);
    }
    @SuppressLint("MissingPermission")
    public void requestBluetoothDicoverable(int durationSec){
        Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,durationSec);
        ApiServer.defaultAndroidContext.startActivity(intent);
    }
    public int requestImageCapture(final AsyncReturn<Integer> ret,String imagePath){
        Uri uri= Uri.parse(getUriForFile(imagePath));
        Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,uri);
        int reqCode= Env.i(Random.class).nextInt(0xffffff);
        if(ApiServer.defaultAndroidContext instanceof Activity){
            ApiServer.onActivityResultCallback.put(reqCode,(param)->{
                ApiServer.onActivityResultCallback.remove(reqCode);
                ApiServer.resolveAsync(ret,(Integer)param[1]);
                return true;
            });
            ((Activity) ApiServer.defaultAndroidContext).startActivityForResult(intent,reqCode);
        }else{
            ApiServer.defaultAndroidContext.startActivity(intent);
            ApiServer.resolveAsync(ret,(Integer)Activity.RESULT_OK);
        }
        return 0;
    }
    public String getContentUriForFile(String path){
        return PublicFileProvider.getUriForFile(ApiServer.defaultAndroidContext,
                ApiServer.defaultAndroidContext.getPackageName()+".publicfileprovider", new File(path)).toString();
    }
    public String getUriForFile(String path){
        File file = new File(path);
        if(Build.VERSION.SDK_INT>Build.VERSION_CODES.M){
            Uri apkUri = PublicFileProvider.getUriForFile(ApiServer.defaultAndroidContext,
                    ApiServer.defaultAndroidContext.getPackageName()+".publicfileprovider", file);
            return apkUri.toString();
        }else{
            return Uri.fromFile(file).toString();
        }
    }

    public boolean requestSystemAlertWindowPermission(AsyncReturn<Boolean> ret){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(ApiServer.defaultAndroidContext.checkSelfPermission("SYSTEM_OVERLAY_WINDOW")==PackageManager.PERMISSION_GRANTED){
                ApiServer.resolveAsync(ret,true);
                return true;
            }
            Intent intent=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:"+ApiServer.defaultAndroidContext.getPackageName()));
            int reqCode= Env.i(Random.class).nextInt(0xffffff);
            ApiServer.onActivityResultCallback.put(reqCode,(args)->{
                ApiServer.resolveAsync(ret,args[1].equals(Activity.RESULT_OK));
                return true;
            });
            ((Activity)ApiServer.defaultAndroidContext).startActivityForResult(intent,reqCode);
        }else{
            ApiServer.resolveAsync(ret,true);
        }
        return false;
    }
    public void requestOpenUniversalTypeFile(String path) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        Uri uri = Uri.parse(getUriForFile(path));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(uri, getMimeTypeFromUri(uri.toString()));
        ApiServer.defaultAndroidContext.startActivity(intent);
    }

    public String getMimeTypeFromUri(String uri) {
        String type=null;
        type=MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri));
        if (type == null) {
            type = "application/octet-stream";
        }
        return type;
    }

    @Override
    public void close() throws IOException {
        if(i==this)i=null;
    }
}
