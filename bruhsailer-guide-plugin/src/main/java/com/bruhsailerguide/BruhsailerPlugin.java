package com.bruhsailerguide;

import com.bruhsailerguide.overlay.MinimapHintOverlay;
import com.bruhsailerguide.overlay.SceneHintOverlay;
import com.bruhsailerguide.overlay.WorldMapHintOverlay;
import com.bruhsailerguide.panel.BruhsailerPluginPanel;
import com.bruhsailerguide.progress.ProgressTracker;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@PluginDescriptor(
	name = "BRUHsailer Guide",
	description = "Sidebar guide + in-world hints for the BRUHsailer ironman route",
	tags = {"guide", "ironman"},
	enabledByDefault = false
)
public class BruhsailerPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private BruhsailerConfig config;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private BruhsailerDataManager dataManager;

	@Inject
	private ProgressTracker progressTracker;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private WorldMapHintOverlay worldMapHintOverlay;

	@Inject
	private MinimapHintOverlay minimapHintOverlay;

	@Inject
	private SceneHintOverlay sceneHintOverlay;

	private BruhsailerPluginPanel panel;
	private NavigationButton navButton;
	private String currentStepId;

	@Override
	protected void startUp() throws Exception
	{
		try
		{
			panel = new BruhsailerPluginPanel(this, dataManager, progressTracker);
		}
		catch (Exception e)
		{
			log.error("BRUHsailer Guide: Failed to initialize panel. Data files may be missing.", e);
			panel = new BruhsailerPluginPanel(this, new BruhsailerDataManager((com.bruhsailerguide.data.GuideData) null), progressTracker);
		}

		BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		java.awt.Graphics2D g = icon.createGraphics();
		g.setColor(java.awt.Color.CYAN);
		g.fillRect(0, 0, 16, 16);
		g.dispose();

		navButton = NavigationButton.builder()
			.tooltip("BRUHsailer Guide")
			.icon(icon)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);

		overlayManager.add(worldMapHintOverlay);
		overlayManager.add(minimapHintOverlay);
		overlayManager.add(sceneHintOverlay);

		log.info("BRUHsailer Guide loaded");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			if (panel != null)
			{
				panel.refresh();
			}
			log.info("BRUHsailer Guide loaded -- open the sidebar");
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
		}

		overlayManager.remove(worldMapHintOverlay);
		overlayManager.remove(minimapHintOverlay);
		overlayManager.remove(sceneHintOverlay);

		panel = null;
		navButton = null;
	}

	public String getCurrentStepId()
	{
		return currentStepId;
	}

	public void setCurrentStepId(String currentStepId)
	{
		this.currentStepId = currentStepId;
	}

	public BruhsailerPluginPanel getPanel()
	{
		return panel;
	}

	@Provides
	BruhsailerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BruhsailerConfig.class);
	}
}
