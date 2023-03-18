package project.xplat.launcher.pxprpcapi.androidhelper;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.Settings;
import de.cketti.fileprovider.PublicFileProvider;
import project.xplat.launcher.AssetsCopy;
import project.xplat.launcher.pxprpcapi.ApiServer;
import pursuer.patchedmsgpack.value.ValueFactory;

import java.io.File;

public class Intent2 {
    public static final String PxprpcNamespace="AndroidHelper-Intent";
    public void requestInstallApk(String apkPath){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        File file = new File(apkPath);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri apkUri = PublicFileProvider.getUriForFile(ApiServer.defaultAndroidContext,
                    AssetsCopy.packageName+".publicfileprovider", file);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        } else {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Uri uri = Uri.fromFile(file);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
        }
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
    public void requestEnableBluetooth(){
        Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        ApiServer.defaultAndroidContext.startActivity(intent);
    }
    public void requestBluetoothDicoverable(int durationSec){
        Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,durationSec);
        ApiServer.defaultAndroidContext.startActivity(intent);
    }
    public void requestImageCapture(String imagePath){
        String uri=getContentUriForFile(imagePath);
        Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,uri);
        ApiServer.defaultAndroidContext.startActivity(intent);
    }
    public String getContentUriForFile(String path){
        return PublicFileProvider.getUriForFile(ApiServer.defaultAndroidContext,
                AssetsCopy.packageName+".publicfileprovider", new File(path)).toString();
    }

}
