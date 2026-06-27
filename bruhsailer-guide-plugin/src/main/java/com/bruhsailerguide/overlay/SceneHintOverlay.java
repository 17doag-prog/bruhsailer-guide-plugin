package com.bruhsailerguide.overlay;

import com.bruhsailerguide.BruhsailerConfig;
import com.bruhsailerguide.BruhsailerDataManager;
import com.bruhsailerguide.BruhsailerPlugin;
import com.bruhsailerguide.data.SpatialHint;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Provider;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;

public class SceneHintOverlay extends Overlay
{
	private static final Color HIGHLIGHT_COLOR = new Color(0, 255, 255, 100);
	private static final Color OUTLINE_COLOR = Color.CYAN;

	private final Client client;
	private final Provider<BruhsailerPlugin> pluginProvider;
	private final BruhsailerDataManager dataManager;
	private final BruhsailerConfig config;

	@Inject
	public SceneHintOverlay(Client client, Provider<BruhsailerPlugin> pluginProvider,
		BruhsailerDataManager dataManager, BruhsailerConfig config)
	{
		this.client = client;
		this.pluginProvider = pluginProvider;
		this.dataManager = dataManager;
		this.config = config;
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showOverlays())
		{
			return null;
		}

		String stepId = pluginProvider.get().getCurrentStepId();
		if (stepId == null)
		{
			return null;
		}

		SpatialHint hint = findFirstValidHint(stepId);
		if (hint == null)
		{
			return null;
		}

		boolean drewSomething = false;

		// Highlight NPCs
		List<Integer> npcIds = hint.getNpcIds();
		if (npcIds != null && !npcIds.isEmpty())
		{
			drewSomething |= highlightNpcs(graphics, npcIds);
		}

		// Highlight objects
		List<Integer> objectIds = hint.getObjectIds();
		if (objectIds != null && !objectIds.isEmpty())
		{
			drewSomething |= highlightObjects(graphics, objectIds);
		}

		// Highlight tile
		if (hint.getX() != 0 || hint.getY() != 0)
		{
			WorldPoint worldPoint = new WorldPoint(hint.getX(), hint.getY(), hint.getPlane());
			LocalPoint localPoint = LocalPoint.fromWorld(client, worldPoint);
			if (localPoint != null)
			{
				Polygon tilePoly = Perspective.getCanvasTilePoly(client, localPoint);
				if (tilePoly != null)
				{
					graphics.setColor(HIGHLIGHT_COLOR);
					graphics.fillPolygon(tilePoly);
					graphics.setColor(OUTLINE_COLOR);
					graphics.drawPolygon(tilePoly);
					drewSomething = true;
				}
			}
		}

		return drewSomething ? new Dimension(1, 1) : null;
	}

	private SpatialHint findFirstValidHint(String stepId)
	{
		List<SpatialHint> hints = dataManager.getHintsForStep(stepId);
		for (SpatialHint hint : hints)
		{
			if (!hint.isUnresolved())
			{
				return hint;
			}
		}
		return null;
	}

	private boolean highlightNpcs(Graphics2D graphics, List<Integer> npcIds)
	{
		boolean drew = false;
		List<NPC> npcs = client.getNpcs();
		if (npcs == null)
		{
			return false;
		}
		for (NPC npc : npcs)
		{
			if (npc != null && npcIds.contains(npc.getId()))
			{
				java.awt.Shape hull = npc.getConvexHull();
				if (hull != null)
				{
					graphics.setColor(HIGHLIGHT_COLOR);
					graphics.fill(hull);
					graphics.setColor(OUTLINE_COLOR);
					graphics.draw(hull);
					drew = true;
				}
			}
		}
		return drew;
	}

	private boolean highlightObjects(Graphics2D graphics, List<Integer> objectIds)
	{
		boolean drew = false;
		Scene scene = client.getTopLevelWorldView().getScene();
		Tile[][][] tiles = scene.getTiles();
		int plane = client.getPlane();
		if (plane < 0 || plane >= tiles.length)
		{
			return false;
		}
		for (int x = 0; x < tiles[plane].length; x++)
		{
			for (int y = 0; y < tiles[plane][x].length; y++)
			{
				Tile tile = tiles[plane][x][y];
				if (tile == null)
				{
					continue;
				}
				if (tile.getDecorativeObject() != null)
				{
					drew |= checkDecorativeObject(graphics, tile.getDecorativeObject(), objectIds);
				}
				GameObject[] gameObjects = tile.getGameObjects();
				if (gameObjects != null)
				{
					for (GameObject obj : gameObjects)
					{
						if (obj != null)
						{
							drew |= checkGameObject(graphics, obj, objectIds);
						}
					}
				}
			}
		}
		return drew;
	}

	private boolean checkDecorativeObject(Graphics2D graphics, net.runelite.api.DecorativeObject decorativeObject, List<Integer> objectIds)
	{
		if (decorativeObject == null)
		{
			return false;
		}
		if (!objectIds.contains(decorativeObject.getId()))
		{
			return false;
		}
		java.awt.Shape hull = decorativeObject.getConvexHull();
		if (hull == null)
		{
			return false;
		}
		graphics.setColor(HIGHLIGHT_COLOR);
		graphics.fill(hull);
		graphics.setColor(OUTLINE_COLOR);
		graphics.draw(hull);
		return true;
	}

	private boolean checkGameObject(Graphics2D graphics, GameObject gameObject, List<Integer> objectIds)
	{
		if (gameObject == null)
		{
			return false;
		}
		if (!objectIds.contains(gameObject.getId()))
		{
			return false;
		}
		java.awt.Shape hull = gameObject.getConvexHull();
		if (hull == null)
		{
			return false;
		}
		graphics.setColor(HIGHLIGHT_COLOR);
		graphics.fill(hull);
		graphics.setColor(OUTLINE_COLOR);
		graphics.draw(hull);
		return true;
	}
}
