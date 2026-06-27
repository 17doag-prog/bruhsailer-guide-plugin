package com.bruhsailerguide;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("bruhsailerguide")
public interface BruhsailerConfig extends Config
{
	@ConfigItem(
		keyName = "showOverlays",
		name = "Show Overlays",
		description = "Show in-world overlay hints"
	)
	default boolean showOverlays()
	{
		return true;
	}

	@ConfigItem(
		keyName = "autoAdvance",
		name = "Auto-advance",
		description = "Automatically advance to the next step when the current step is marked complete"
	)
	default boolean autoAdvance()
	{
		return false;
	}
}
