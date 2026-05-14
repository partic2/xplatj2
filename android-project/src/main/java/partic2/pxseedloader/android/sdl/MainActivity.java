package partic2.pxseedloader.android.sdl;
import android.content.Intent;
import android.os.*;
import org.libsdl.app.*;
import partic2.pxseedloader.android.launcher.ApiServer;
import pxprpcapi.androidhelper.AndroidUIBase;

public class MainActivity extends SDLActivity
{

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		ApiServer.defaultAndroidContext = this;
		if(AndroidUIBase.i!=null){
			AndroidUIBase.i.extraEvent.fireEvent("sdl.onResume");
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if(ApiServer.defaultAndroidContext==this){
			ApiServer.defaultAndroidContext=getApplicationContext();
		}
		if(AndroidUIBase.i!=null){
			AndroidUIBase.i.extraEvent.fireEvent("sdl.onStop");
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		ApiServer.onActivityResult(requestCode,resultCode,data);
		super.onActivityResult(requestCode,resultCode,data);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected String[] getLibraries() {
		return new String[] {
				"SDL2",
				// "SDL2_image",
				// "SDL2_mixer",
				// "SDL2_net",
				// "SDL2_ttf",
		};
	}
	@Override
	protected String getMainSharedObject()
	{
		return "libpxprpc_rtbridge.so";
	}
	

}
