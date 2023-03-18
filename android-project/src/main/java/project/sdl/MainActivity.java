package project.sdl;
import android.os.*;
import org.libsdl.app.*;
import project.xplat.launcher.pxprpcapi.ApiServer;

public class MainActivity extends SDLActivity
{

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ApiServer.start(this);
	}

	@Override
	protected void onDestroy() {
		ApiServer.stop();
		super.onDestroy();
	}

	@Override
	protected String[] getLibraries() {
		return new String[] {
				"hidapi",
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
