package com.bruhsailerguide.overlay;

import com.bruhsailerguide.BruhsailerConfig;
import com.bruhsailerguide.BruhsailerDataManager;
import com.bruhsailerguide.BruhsailerPlugin;
import com.bruhsailerguide.data.GuideChapter;
import com.bruhsailerguide.data.GuideData;
import com.bruhsailerguide.data.GuideSection;
import com.bruhsailerguide.data.GuideStep;
import com.bruhsailerguide.data.SpatialHint;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import javax.inject.Provider;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class WorldMapHintOverlayTest
{
	private WorldMapHintOverlay overlay;
	private BruhsailerPlugin plugin;
	private BruhsailerDataManager dataManager;
	private BruhsailerConfig config;
	private Client client;
	private WorldMapPointManager worldMapPointManager;
	private CountingGraphics2D graphics;

	@Before
	public void setUp()
	{
		plugin = new BruhsailerPlugin();
		dataManager = new BruhsailerDataManager(createStubGuideData());
		config = createStubConfig();
		client = createStubClient();
		worldMapPointManager = new WorldMapPointManager();
		overlay = new WorldMapHintOverlay(() -> plugin, dataManager, config, client, worldMapPointManager);
		graphics = new CountingGraphics2D();
	}

	@Test
	public void testRenderDrawsMarkerWhenStepHasWorldPoint()
	{
		plugin.setCurrentStepId("step-1");
		overlay.render(graphics);
		assertTrue("Expected at least one draw or fill call", graphics.getTotalDrawCount() > 0);
	}

	@Test
	public void testRenderReturnsNullWhenOverlaysDisabled()
	{
		plugin.setCurrentStepId("step-1");
		config = createStubConfigDisabled();
		overlay = new WorldMapHintOverlay(() -> plugin, dataManager, config, client, worldMapPointManager);
		assertNull(overlay.render(graphics));
		assertEquals(0, graphics.getTotalDrawCount());
	}

	@Test
	public void testRenderReturnsNullWhenNoCurrentStep()
	{
		plugin.setCurrentStepId(null);
		assertNull(overlay.render(graphics));
		assertEquals(0, graphics.getTotalDrawCount());
	}

	private GuideData createStubGuideData()
	{
		SpatialHint hint = new SpatialHint();
		hint.setLabel("Target");
		hint.setX(3200);
		hint.setY(3200);
		hint.setPlane(0);
		hint.setUnresolved(false);
		hint.setNpcIds(Collections.emptyList());
		hint.setObjectIds(Collections.emptyList());

		GuideStep step = new GuideStep();
		step.setId("step-1");
		step.setNumber(1);
		step.setText("Test step");
		step.setHints(Collections.singletonList(hint));

		GuideSection section = new GuideSection();
		section.setIndex(1);
		section.setTitle("Section 1");
		section.setSteps(Collections.singletonList(step));

		GuideChapter chapter = new GuideChapter();
		chapter.setIndex(1);
		chapter.setTitle("Chapter 1");
		chapter.setSections(Collections.singletonList(section));

		GuideData data = new GuideData();
		data.setSourceUrl("test");
		data.setUpdatedOn("2024-01-01");
		data.setChapters(Collections.singletonList(chapter));
		return data;
	}

	private BruhsailerConfig createStubConfig()
	{
		return new BruhsailerConfig()
		{
			@Override
			public boolean showOverlays()
			{
				return true;
			}
		};
	}

	private BruhsailerConfig createStubConfigDisabled()
	{
		return new BruhsailerConfig()
		{
			@Override
			public boolean showOverlays()
			{
				return false;
			}
		};
	}

	private Client createStubClient()
	{
		WorldMap worldMap = (WorldMap) Proxy.newProxyInstance(
			WorldMap.class.getClassLoader(),
			new Class<?>[]{WorldMap.class},
			(proxy, method, args) ->
			{
				if ("getWorldMapZoom".equals(method.getName()))
				{
					return 4.0f;
				}
				if ("getWorldMapPosition".equals(method.getName()))
				{
					return new Point(3200, 3200);
				}
				return null;
			}
		);

		return (Client) Proxy.newProxyInstance(
			Client.class.getClassLoader(),
			new Class<?>[]{Client.class},
			(proxy, method, args) ->
			{
				if ("getWorldMap".equals(method.getName()))
				{
					return worldMap;
				}
				return null;
			}
		);
	}
}
