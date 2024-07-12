package project.xplat.sdl;
import android.content.Intent;
import android.os.*;
import org.libsdl.app.*;
import project.xplat.launcher.AssetsCopy;
import project.xplat.launcher.ApiServer;

public class MainActivity extends SDLActivity
{

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		ApiServer.onActivityResult(requestCode,resultCode,data);
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
		return "libSDLLoader.so";
	}
	

}
