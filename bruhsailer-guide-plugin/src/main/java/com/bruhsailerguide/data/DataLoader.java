package com.bruhsailerguide.data;

import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataLoader
{
	private static final Gson gson = new Gson();

	public static GuideData loadGuide()
	{
		try (InputStream is = DataLoader.class.getResourceAsStream("/guide.json"))
		{
			if (is == null)
			{
				throw new IllegalStateException("guide.json not found on classpath");
			}
			Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
			return gson.fromJson(reader, GuideData.class);
		}
		catch (Exception e)
		{
			throw new RuntimeException("Failed to load guide.json", e);
		}
	}

	public static Map<String, SpatialHint> loadSpatial()
	{
		try (InputStream is = DataLoader.class.getResourceAsStream("/spatial.json"))
		{
			if (is == null)
			{
				throw new IllegalStateException("spatial.json not found on classpath");
			}
			Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
			SpatialWrapper wrapper = gson.fromJson(reader, SpatialWrapper.class);
			Map<String, SpatialHint> result = new HashMap<>();
			for (Map.Entry<String, SpatialEntry> entry : wrapper.steps.entrySet())
			{
				SpatialHint hint = new SpatialHint();
				SpatialEntry se = entry.getValue();
				hint.setUnresolved(se.unresolved);
				hint.setNote(se.note);
				if (se.unresolved)
				{
					hint.setLabel(null);
					hint.setNpcIds(Collections.emptyList());
					hint.setObjectIds(Collections.emptyList());
					hint.setX(0);
					hint.setY(0);
					hint.setPlane(0);
				}
				else
				{
					hint.setLabel(se.label);
					hint.setNpcIds(se.npcIds != null ? se.npcIds : Collections.emptyList());
					hint.setObjectIds(se.objectIds != null ? se.objectIds : Collections.emptyList());
					if (se.worldPoint != null)
					{
						hint.setX(se.worldPoint.x);
						hint.setY(se.worldPoint.y);
						hint.setPlane(se.worldPoint.plane);
					}
					else
					{
						hint.setX(0);
						hint.setY(0);
						hint.setPlane(0);
					}
				}
				result.put(entry.getKey(), hint);
			}
			return result;
		}
		catch (Exception e)
		{
			throw new RuntimeException("Failed to load spatial.json", e);
		}
	}

	public static Map<String, StepStats> loadStats()
	{
		try (InputStream is = DataLoader.class.getResourceAsStream("/stats.json"))
		{
			if (is == null)
			{
				throw new IllegalStateException("stats.json not found on classpath");
			}
			Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
			StatsWrapper wrapper = gson.fromJson(reader, StatsWrapper.class);
			Map<String, StepStats> result = new HashMap<>();
			for (Map.Entry<String, StatsEntry> entry : wrapper.steps.entrySet())
			{
				StepStats stats = new StepStats();
				StatsEntry se = entry.getValue();
				try
				{
					stats.setGpChange(Integer.parseInt(se.gpChange));
				}
				catch (NumberFormatException nfe)
				{
					stats.setGpChange(0);
				}
				stats.setGpTotal(se.gpTotal);
				stats.setLevels(se.levels != null ? se.levels : Collections.emptyMap());
				result.put(entry.getKey(), stats);
			}
			return result;
		}
		catch (Exception e)
		{
			throw new RuntimeException("Failed to load stats.json", e);
		}
	}

	public static GuideData loadAndJoin()
	{
		GuideData guide = loadGuide();
		Map<String, SpatialHint> spatialMap = loadSpatial();
		Map<String, StepStats> statsMap = loadStats();

		for (GuideChapter chapter : guide.getChapters())
		{
			for (GuideSection section : chapter.getSections())
			{
				for (GuideStep step : section.getSteps())
				{
					SpatialHint hint = spatialMap.get(step.getId());
					if (hint != null)
					{
						step.setHints(Collections.singletonList(hint));
					}
					else
					{
						step.setHints(Collections.emptyList());
					}

					StepStats stats = statsMap.get(step.getId());
					step.setStats(stats);
				}
			}
		}

		return guide;
	}

	// --- Wrapper classes for Gson parsing ---

	private static class SpatialWrapper
	{
		String source;
		String resolvedAt;
		Map<String, SpatialEntry> steps;
	}

	private static class SpatialEntry
	{
		String label;
		List<Integer> npcIds;
		List<Integer> objectIds;
		WorldPointEntry worldPoint;
		boolean unresolved;
		String note;
	}

	private static class WorldPointEntry
	{
		int x;
		int y;
		int plane;
	}

	private static class StatsWrapper
	{
		String sourceUrl;
		String exportedAt;
		List<String> skills;
		Map<String, StatsEntry> steps;
	}

	private static class StatsEntry
	{
		String gpChange;
		String gpTotal;
		Map<String, Integer> levels;
	}
}
