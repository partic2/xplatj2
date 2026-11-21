package pxprpcapi.androidhelper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.Settings;
import android.webkit.MimeTypeMap;
import de.cketti.fileprovider.PublicFileProvider;
import partic2.pxseedloader.android.launcher.ApiServer;
import pxprpc.extend.AsyncReturn;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
        if(!(ApiServer.defaultAndroidContext instanceof Activity)){
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        ApiServer.defaultAndroidContext.startActivity(intent);
    }
    public void requestSendShortMessage(String tel,String body){
        Uri uri=Uri.parse("smsto:"+tel);
        Intent intent=new Intent(Intent.ACTION_SENDTO,uri);
        if(!(ApiServer.defaultAndroidContext instanceof Activity)){
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        intent.putExtra("sms_body",body);
        ApiServer.defaultAndroidContext.startActivity(intent);
    }
    public void requestSendOthers(String filePath,String mime,String chooserTitle){
        Uri uri = Uri.parse(getUriForFile(filePath));
        if(mime==null || mime.equals("")){
            mime=getMimeTypeFromUri(uri.toString());
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(mime);
        //Must be the Uri object (not its string form) so the receiver gets a
        //content grant; the clip data makes the grant survive the chooser.
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Share");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.setClipData(ClipData.newRawUri(null, uri));
        Intent chooser=Intent.createChooser(shareIntent, chooserTitle);
        chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if(!(ApiServer.defaultAndroidContext instanceof Activity)){
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        ApiServer.defaultAndroidContext.startActivity(chooser);
    }
    public void requestOpenByDefaultHandler(String uris){
        Uri uri=Uri.parse(uris);
        Intent intent=new Intent(Intent.ACTION_VIEW,uri);
        if(!(ApiServer.defaultAndroidContext instanceof Activity)){
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        ApiServer.defaultAndroidContext.startActivity(intent);
    }
    public void requestOpenSetting(String setting){
        Intent intent=new Intent("android.settings."+setting);
        if(!(ApiServer.defaultAndroidContext instanceof Activity)){
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        ApiServer.defaultAndroidContext.startActivity(intent);
    }
    public List<String> getSettingProviderList(){
        return new ArrayList<String>(Arrays.asList(
                Settings.ACTION_ACCESSIBILITY_SETTINGS,
                Settings.ACTION_HOME_SETTINGS,
                Settings.ACTION_NETWORK_OPERATOR_SETTINGS,
                Settings.ACTION_WIFI_SETTINGS,
                Settings.ACTION_WIFI_IP_SETTINGS,
                Settings.ACTION_BLUETOOTH_SETTINGS,
                Settings.ACTION_DATE_SETTINGS,
                Settings.ACTION_DISPLAY_SETTINGS
        ));
    }
    public void requestOpenApplication(String packageName,String componentName,String action){
        Intent intent;
        if(packageName.length()>0 && (componentName==null || componentName.length()==0)){
            //No component given: launch the package's default launcher activity.
            intent=ApiServer.defaultAndroidContext.getPackageManager().getLaunchIntentForPackage(packageName);
            if(intent==null){
                throw new IllegalArgumentException("Package "+packageName+" has no launchable activity");
            }
        }else{
            intent=new Intent();
            intent.setComponent(new ComponentName(packageName,componentName));
        }
        if(action!=null && action.length()>0){
            intent.setAction(action);
        }else if(intent.getAction()==null){
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
        }
        if(!(ApiServer.defaultAndroidContext instanceof Activity)){
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        ApiServer.defaultAndroidContext.startActivity(intent);
    }
    public void requestStartService(String packageName,String componentName,String action){
        Intent intent=new Intent();
        ComponentName cn=new ComponentName(packageName,componentName);
        intent.setComponent(cn);
        if(action!=null && action.length()>0){
            intent.setAction(action);
        }
        ApiServer.defaultAndroidContext.startService(intent);
    }
    @SuppressLint("MissingPermission")
    public void requestEnableBluetooth(){
        Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        if(!(ApiServer.defaultAndroidContext instanceof Activity)){
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        ApiServer.defaultAndroidContext.startActivity(intent);
    }
    @SuppressLint("MissingPermission")
    public void requestBluetoothDicoverable(int durationSec){
        Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        if(!(ApiServer.defaultAndroidContext instanceof Activity)){
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,durationSec);
        ApiServer.defaultAndroidContext.startActivity(intent);
    }
    protected static final AtomicInteger requestCodeGen=new AtomicInteger(0x3000);
    public int requestImageCapture(final AsyncReturn<Integer> ret,String imagePath){
        Uri uri= Uri.parse(getUriForFile(imagePath));
        Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,uri);
        //The camera app writes into our FileProvider file, it needs a write grant.
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Context ctx=ApiServer.defaultAndroidContext;
        if(ctx instanceof Activity){
            final int reqCode=requestCodeGen.incrementAndGet();
            //ApiServer removes the callback before dispatching, so it is one-shot.
            ApiServer.onActivityResultCallback.put(reqCode,(param)->{
                ApiServer.resolveAsync(ret,(Integer)param[1]);
                return true;
            });
            ((Activity) ctx).startActivityForResult(intent,reqCode);
        }else{
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
            //No Activity result is delivered in this case; report launch success.
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
        Context ctx=ApiServer.defaultAndroidContext;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //checkSelfPermission("SYSTEM_OVERLAY_WINDOW") does not reflect the
            //overlay grant; Settings.canDrawOverlays is the correct check.
            if(Settings.canDrawOverlays(ctx)){
                ApiServer.resolveAsync(ret,true);
                return true;
            }
            if(!(ctx instanceof Activity)){
                //Only an Activity can show the settings screen.
                ApiServer.resolveAsync(ret,false);
                return false;
            }
            Intent intent=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:"+ctx.getPackageName()));
            final int reqCode=requestCodeGen.incrementAndGet();
            final Context checkCtx=ctx;
            ApiServer.onActivityResultCallback.put(reqCode,(args)->{
                //The result code of this settings screen is unreliable, re-check the grant.
                ApiServer.resolveAsync(ret,Settings.canDrawOverlays(checkCtx));
                return true;
            });
            ((Activity)ctx).startActivityForResult(intent,reqCode);
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

    public void startActivity(String activityName,String packageName) throws ClassNotFoundException {
        Intent intent = new Intent();
        if(!(ApiServer.defaultAndroidContext instanceof Activity)){
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        if(packageName.equals("")) {
            intent.setClass(ApiServer.defaultAndroidContext,Class.forName(activityName));
            ApiServer.defaultAndroidContext.startActivity(intent);
        }else{
            intent.setClassName(packageName,activityName);
            ApiServer.defaultAndroidContext.startActivity(intent);
        }
    }

    public void openHttpUrl(String url,String flags) throws ClassNotFoundException {
        Intent intent = new Intent();
        Class<?> webviewClass=Class.forName("partic2.pxseedloader.android.webapp.MainActivity");
        if(flags.contains("use-gecko")){
            webviewClass=Class.forName("partic2.pxseedloader.android.webapp.GeckoViewActivity");
        }
        if(!(ApiServer.defaultAndroidContext instanceof Activity)){
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        intent.setClass(ApiServer.defaultAndroidContext,webviewClass);
        intent.putExtra("url",url);
        ApiServer.defaultAndroidContext.startActivity(intent);
    }

    public void openStorageManagePermissionSetting(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent intent;
            try {
                intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + ApiServer.defaultAndroidContext.getPackageName()));
                ApiServer.defaultAndroidContext.startActivity(intent);
            } catch (Exception e) {
                intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                ApiServer.defaultAndroidContext.startActivity(intent);
            }
        }
    }
    @Override
    public void close() throws IOException {
        if(i==this)i=null;
    }
}
