package com.bruhsailerguide.overlay;

import com.bruhsailerguide.BruhsailerConfig;
import com.bruhsailerguide.BruhsailerDataManager;
import com.bruhsailerguide.BruhsailerPlugin;
import com.bruhsailerguide.data.SpatialHint;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Provider;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class MinimapHintOverlay extends Overlay
{
	private static final Color MARKER_COLOR = Color.CYAN;
	private static final int MARKER_SIZE = 8;
	private static final int EDGE_PADDING = 4;

	private final Provider<BruhsailerPlugin> pluginProvider;
	private final BruhsailerDataManager dataManager;
	private final BruhsailerConfig config;
	private final Client client;

	@Inject
	public MinimapHintOverlay(Provider<BruhsailerPlugin> pluginProvider, BruhsailerDataManager dataManager,
		BruhsailerConfig config, Client client)
	{
		this.pluginProvider = pluginProvider;
		this.dataManager = dataManager;
		this.config = config;
		this.client = client;
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPosition(OverlayPosition.DYNAMIC);
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

		WorldPoint target = new WorldPoint(hint.getX(), hint.getY(), hint.getPlane());
		LocalPoint localTarget = LocalPoint.fromWorld(client, target);
		if (localTarget == null)
		{
			return null;
		}

		Point minimapPoint = Perspective.localToMinimap(client, localTarget);
		if (minimapPoint != null)
		{
			drawMarker(graphics, minimapPoint.getX(), minimapPoint.getY());
			return new Dimension(MARKER_SIZE, MARKER_SIZE);
		}

		// Target is off the minimap; draw an edge arrow
		Player player = client.getLocalPlayer();
		if (player == null || player.getWorldLocation() == null)
		{
			return null;
		}

		Point edge = computeEdgeArrowPoint(player.getWorldLocation(), target);
		if (edge != null)
		{
			drawEdgeArrow(graphics, edge.getX(), edge.getY(), player.getWorldLocation(), target);
			return new Dimension(MARKER_SIZE, MARKER_SIZE);
		}

		return null;
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

	private void drawMarker(Graphics2D graphics, int x, int y)
	{
		graphics.setColor(MARKER_COLOR);
		int s = MARKER_SIZE;
		graphics.fillOval(x - s / 2, y - s / 2, s, s);
		graphics.setColor(Color.WHITE);
		graphics.setStroke(new BasicStroke(1));
		graphics.drawOval(x - s / 2, y - s / 2, s, s);
	}

	private Point computeEdgeArrowPoint(WorldPoint player, WorldPoint target)
	{
		int dx = target.getX() - player.getX();
		int dy = target.getY() - player.getY();
		if (dx == 0 && dy == 0)
		{
			return null;
		}

		double angle = Math.atan2(dy, dx);
		int radius = 60; // approximate minimap radius
		int ex = (int) (Math.cos(angle) * radius);
		int ey = (int) (Math.sin(angle) * radius);
		return new Point(ex, ey);
	}

	private void drawEdgeArrow(Graphics2D graphics, int x, int y, WorldPoint player, WorldPoint target)
	{
		double angle = Math.atan2(target.getY() - player.getY(), target.getX() - player.getX());
		int size = MARKER_SIZE;

		graphics.setColor(MARKER_COLOR);
		Polygon arrow = new Polygon();
		arrow.addPoint(x, y);
		arrow.addPoint(
			x + (int) (Math.cos(angle + Math.PI * 0.8) * size),
			y + (int) (Math.sin(angle + Math.PI * 0.8) * size)
		);
		arrow.addPoint(
			x + (int) (Math.cos(angle - Math.PI * 0.8) * size),
			y + (int) (Math.sin(angle - Math.PI * 0.8) * size)
		);
		graphics.fillPolygon(arrow);
		graphics.setColor(Color.WHITE);
		graphics.drawPolygon(arrow);
	}
}
