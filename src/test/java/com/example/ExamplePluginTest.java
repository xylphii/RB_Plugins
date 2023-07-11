package com.example;

import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.PacketUtils.PacketUtilsPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.plugins.autologhop.AutoLogHopPlugin;
import net.runelite.client.plugins.fletching.FletchingPlugin;

public class ExamplePluginTest
{
	public static void main(String[] args) throws Exception
	{
		// use this as an import statement for tests lol. just all classes
		ExternalPluginManager.loadBuiltin(
				EthanApiPlugin.class,
				PacketUtilsPlugin.class,
				FletchingPlugin.class,
				AutoLogHopPlugin.class
//				NeverlogPlugin.class
//				ThieverPlugin.class
		);
		RuneLite.main(args);
	}
}