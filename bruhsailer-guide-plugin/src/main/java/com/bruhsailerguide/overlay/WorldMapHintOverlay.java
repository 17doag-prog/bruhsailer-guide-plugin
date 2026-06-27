package com.bruhsailerguide.overlay;

import com.bruhsailerguide.BruhsailerConfig;
import com.bruhsailerguide.BruhsailerDataManager;
import com.bruhsailerguide.BruhsailerPlugin;
import com.bruhsailerguide.data.SpatialHint;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Provider;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

public class WorldMapHintOverlay extends Overlay
{
	private static final Color MARKER_COLOR = Color.RED;
	private static final int MARKER_SIZE = 16;

	private final Provider<BruhsailerPlugin> pluginProvider;
	private final BruhsailerDataManager dataManager;
	private final BruhsailerConfig config;
	private final Client client;
	private final WorldMapPointManager worldMapPointManager;

	private WorldMapPoint currentMapPoint;
	private String lastStepId;

	@Inject
	public WorldMapHintOverlay(Provider<BruhsailerPlugin> pluginProvider, BruhsailerDataManager dataManager,
		BruhsailerConfig config, Client client, WorldMapPointManager worldMapPointManager)
	{
		this.pluginProvider = pluginProvider;
		this.dataManager = dataManager;
		this.config = config;
		this.client = client;
		this.worldMapPointManager = worldMapPointManager;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showOverlays())
		{
			clearMapPoint();
			return null;
		}

		String stepId = pluginProvider.get().getCurrentStepId();
		if (stepId == null)
		{
			clearMapPoint();
			return null;
		}

		if (!stepId.equals(lastStepId))
		{
			lastStepId = stepId;
			updateMapPoint(stepId);
		}

		SpatialHint hint = findFirstValidHint(stepId);
		if (hint == null)
		{
			clearMapPoint();
			return null;
		}

		Point drawPoint = mapHintToGraphicsPoint(hint);
		if (drawPoint == null)
		{
			return null;
		}

		// Draw a small arrow marker directly on the overlay graphics for testability
		graphics.setColor(MARKER_COLOR);
		int x = drawPoint.getX();
		int y = drawPoint.getY();
		int s = MARKER_SIZE;
		Polygon arrow = new Polygon(
			new int[]{x, x + s / 2, x - s / 2},
			new int[]{y, y + s, y + s},
			3
		);
		graphics.fillPolygon(arrow);

		// Also draw an outline for additional draw call visibility
		graphics.drawPolygon(arrow);

		return new Dimension(s, s);
	}

	private SpatialHint findFirstValidHint(String stepId)
	{
		List<SpatialHint> hints = dataManager.getHintsForStep(stepId);
		for (SpatialHint hint : hints)
		{
			if (!hint.isUnresolved() && (hint.getX() != 0 || hint.getY() != 0))
			{
				return hint;
			}
		}
		return null;
	}

	private void updateMapPoint(String stepId)
	{
		clearMapPoint();
		SpatialHint hint = findFirstValidHint(stepId);
		if (hint != null)
		{
			WorldPoint wp = new WorldPoint(hint.getX(), hint.getY(), hint.getPlane());
			BufferedImage image = createMarkerImage();
			currentMapPoint = new WorldMapPoint(wp, image);
			currentMapPoint.setName(hint.getLabel() != null ? hint.getLabel() : "Target");
			worldMapPointManager.add(currentMapPoint);
		}
	}

	private void clearMapPoint()
	{
		if (currentMapPoint != null)
		{
			worldMapPointManager.remove(currentMapPoint);
			currentMapPoint = null;
		}
	}

	private static BufferedImage createMarkerImage()
	{
		BufferedImage img = new BufferedImage(MARKER_SIZE, MARKER_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setColor(MARKER_COLOR);
		int[] x = {MARKER_SIZE / 2, MARKER_SIZE - 2, 2};
		int[] y = {2, MARKER_SIZE - 2, MARKER_SIZE - 2};
		g.fillPolygon(x, y, 3);
		g.dispose();
		return img;
	}

	private Point mapHintToGraphicsPoint(SpatialHint hint)
	{
		if (client == null)
		{
			return null;
		}

		WorldMap worldMap = client.getWorldMap();
		if (worldMap == null)
		{
			return null;
		}

		float zoom = worldMap.getWorldMapZoom();
		Point mapPos = worldMap.getWorldMapPosition();
		if (mapPos == null)
		{
			return null;
		}

		int dx = (int) ((hint.getX() - mapPos.getX()) * zoom);
		int dy = (int) ((mapPos.getY() - hint.getY()) * zoom);

		return new Point(dx, dy);
	}
}
