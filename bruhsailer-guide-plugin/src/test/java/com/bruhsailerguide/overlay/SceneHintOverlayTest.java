package com.bruhsailerguide.overlay;

import com.bruhsailerguide.BruhsailerConfig;
import com.bruhsailerguide.BruhsailerDataManager;
import com.bruhsailerguide.BruhsailerPlugin;
import com.bruhsailerguide.data.GuideChapter;
import com.bruhsailerguide.data.GuideData;
import com.bruhsailerguide.data.GuideSection;
import com.bruhsailerguide.data.GuideStep;
import com.bruhsailerguide.data.SpatialHint;
import java.awt.Polygon;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Provider;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class SceneHintOverlayTest
{
	private SceneHintOverlay overlay;
	private BruhsailerPlugin plugin;
	private BruhsailerDataManager dataManager;
	private BruhsailerConfig config;
	private Client client;
	private CountingGraphics2D graphics;

	@Before
	public void setUp()
	{
		plugin = new BruhsailerPlugin();
		dataManager = new BruhsailerDataManager(createStubGuideData());
		config = createStubConfig();
		client = createStubClient();
		overlay = new SceneHintOverlay(client, () -> plugin, dataManager, config);
		graphics = new CountingGraphics2D();
	}

	@Test
	public void testRenderDrawsNpcConvexHullWhenNpcIdMatches()
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
		overlay = new SceneHintOverlay(client, () -> plugin, dataManager, config);
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
		hint.setLabel("Target NPC");
		hint.setX(3200);
		hint.setY(3200);
		hint.setPlane(0);
		hint.setUnresolved(false);
		hint.setNpcIds(Arrays.asList(123));
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
		Polygon convexHull = new Polygon(new int[]{0, 10, 10, 0}, new int[]{0, 0, 10, 10}, 4);

		NPC npc = (NPC) Proxy.newProxyInstance(
			NPC.class.getClassLoader(),
			new Class<?>[]{NPC.class},
			(proxy, method, args) ->
			{
				if ("getId".equals(method.getName()))
				{
					return 123;
				}
				if ("getConvexHull".equals(method.getName()))
				{
					return convexHull;
				}
				return null;
			}
		);

		Tile tile = (Tile) Proxy.newProxyInstance(
			Tile.class.getClassLoader(),
			new Class<?>[]{Tile.class},
			(proxy, method, args) ->
			{
				if ("getDecorativeObject".equals(method.getName()) || "getGameObjects".equals(method.getName()))
				{
					return null;
				}
				return null;
			}
		);

		Tile[][][] tiles = new Tile[4][][];
		tiles[0] = new Tile[104][104];
		tiles[0][50][50] = tile;

		Scene scene = (Scene) Proxy.newProxyInstance(
			Scene.class.getClassLoader(),
			new Class<?>[]{Scene.class},
			(proxy, method, args) ->
			{
				if ("getTiles".equals(method.getName()))
				{
					return tiles;
				}
				return null;
			}
		);

		WorldView worldView = (WorldView) Proxy.newProxyInstance(
			WorldView.class.getClassLoader(),
			new Class<?>[]{WorldView.class},
			(proxy, method, args) ->
			{
				if ("getScene".equals(method.getName()))
				{
					return scene;
				}
				if ("getId".equals(method.getName()))
				{
					return 0;
				}
				if ("getPlane".equals(method.getName()))
				{
					return 0;
				}
				if ("getBaseX".equals(method.getName()))
				{
					return 3200;
				}
				if ("getBaseY".equals(method.getName()))
				{
					return 3200;
				}
				if ("getSizeX".equals(method.getName()))
				{
					return 104;
				}
				if ("getSizeY".equals(method.getName()))
				{
					return 104;
				}
				return null;
			}
		);

		return (Client) Proxy.newProxyInstance(
			Client.class.getClassLoader(),
			new Class<?>[]{Client.class},
			(proxy, method, args) ->
			{
				if ("getNpcs".equals(method.getName()))
				{
					return Collections.singletonList(npc);
				}
				if ("getTopLevelWorldView".equals(method.getName()))
				{
					return worldView;
				}
				if ("findWorldViewFromWorldPoint".equals(method.getName()))
				{
					return worldView;
				}
				if ("getPlane".equals(method.getName()))
				{
					return 0;
				}
				return null;
			}
		);
	}
}
