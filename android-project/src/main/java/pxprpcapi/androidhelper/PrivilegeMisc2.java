package pxprpcapi.androidhelper;

import java.io.Closeable;
import java.io.IOException;

public class PrivilegeMisc2 implements Closeable {
    public static final String PxprpcNamespace="AndroidHelper.PrivilegeMisc";
    public static PrivilegeMisc2 i;
    public PrivilegeMisc2(){
        i=this;
    }
    public boolean isRooted(){
        try {
            Runtime.getRuntime().exec("su -c echo rooted").waitFor();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public void toggleScreen() throws IOException {
        inputKeyEvent(26);
    }
    public void tryUnlockScreen() throws IOException{
        inputKeyEvent(82);
    }
    public void inputKeyEvent(int keycode) throws IOException {
        Runtime.getRuntime().exec("input keyevent "+keycode);
    }

    @Override
    public void close() throws IOException {
        if(i==this)i=null;
    }
}
