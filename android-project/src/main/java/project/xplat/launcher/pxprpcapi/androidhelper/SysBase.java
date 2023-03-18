package project.xplat.launcher.pxprpcapi.androidhelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import project.xplat.launcher.pxprpcapi.ApiServer;
import project.xplat.launcher.pxprpcapi.Utils;
import pursuer.patchedmsgpack.core.MessageBufferPacker;
import pursuer.patchedmsgpack.core.MessagePack;
import pursuer.patchedmsgpack.core.MessagePacker;
import pursuer.patchedmsgpack.value.MapValue;
import pursuer.patchedmsgpack.value.ValueFactory;
import pursuer.patchedmsgpack.value.ValueFactory.MapBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.UUID;

public class SysBase {

	public static final String PxprpcNamespace="AndroidHelper-Sysbase";

	public BroadcastReceiver newBroadcastReceiver() {
		return new PxprpcBroadcastReceiverAdapter();
	}

	public Context getDefaultContext() {
		return ApiServer.defaultAndroidContext;
	}

	public void registerBroadcastReceiver(BroadcastReceiver receiver, String filter) {
		getDefaultContext().registerReceiver(receiver, new IntentFilter(filter));
	}

	public void unregisterBroadcastReceiver(BroadcastReceiver receiver) {
		getDefaultContext().unregisterReceiver(receiver);
	}

	
	public static MapValue bundleToMap(Bundle b) {
		MapBuilder mb = ValueFactory.newMapBuilder();
		for (String k : b.keySet()) {
			mb.put(ValueFactory.newString(k), Utils.msgpackValueFrom(b.get(k)));
		}
		return mb.build();
	}
	public byte[] describeBundle(Bundle b) {
		return Utils.packFrom(bundleToMap(b));
	}

	public byte[] describeIntent(Intent intent) {
		Bundle b = intent.getExtras();
		return Utils.packFrom(ValueFactory.newMapBuilder()
				.put(ValueFactory.newString("action"),ValueFactory.newString(intent.getAction()))
				.put(ValueFactory.newString("extras"),bundleToMap(intent.getExtras()))
				.build());
	}

	public String typeof(Object obj) {
		return obj.getClass().getName();
	}

	public Object getService(String name) {
		return getDefaultContext().getSystemService(name);
	}

	public UUID newUUID(long mostSigBits, long leastSigBits) {
		return new UUID(mostSigBits, leastSigBits);
	}

	public byte[] describeFields(Object o) {
		MapBuilder mb = ValueFactory.newMapBuilder();
		for (Field f : o.getClass().getFields()) {
			Object v = null;
			try {
				v = f.get(o);
				if (v == null)
					continue;
				mb.put(ValueFactory.newString(f.getName()),Utils.msgpackValueFrom(v));
			} catch (IllegalAccessException e) {
			}
		}
		return Utils.packFrom(mb.build());
	}
	public byte[] describeStaticFields(Class<?> c) {
		MapBuilder mb = ValueFactory.newMapBuilder();
		for (Field f : c.getFields()) {
			Object v = null;
			try {
				v = f.get(null);
				if (v == null)
					continue;
				mb.put(ValueFactory.newString(f.getName()),Utils.msgpackValueFrom(v));
			} catch (IllegalAccessException e) {
			}
		}
		return Utils.packFrom(mb.build());
	}

	public void close(Closeable c) {
		ApiServer.closeQuietly(c);
	}
}
