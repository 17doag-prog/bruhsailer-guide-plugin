package com.bruhsailerguide.data;

import org.junit.Test;
import static org.junit.Assert.*;

public class DataLoaderTest
{
	@Test
	public void testGuideHasThreeChapters()
	{
		GuideData guide = DataLoader.loadAndJoin();
		assertNotNull(guide);
		assertEquals(3, guide.getChapters().size());
	}

	@Test
	public void testStepNew11HasActionsWithIds()
	{
		GuideData guide = DataLoader.loadAndJoin();
		GuideStep step = findStep(guide, "new-1-1");
		assertNotNull("Step new-1-1 should exist", step);
		assertNotNull("Actions should not be null", step.getActions());
		assertFalse("Actions should not be empty", step.getActions().isEmpty());

		GuideAction action = step.getActions().get(0);
		assertNotNull("Action id should not be null", action.getId());
		assertNotNull("Action text should not be null", action.getText());
		assertNotNull("Action source should not be null", action.getSource());
	}

	@Test
	public void testStepNew11HasHintsAndPlane1()
	{
		GuideData guide = DataLoader.loadAndJoin();
		GuideStep step = findStep(guide, "new-1-1");
		assertNotNull("Step new-1-1 should exist", step);
		assertNotNull("Hints should not be null", step.getHints());
		assertFalse("Hints should not be empty", step.getHints().isEmpty());
		SpatialHint hint = step.getHints().get(0);
		assertEquals("Duke Horacio", hint.getLabel());
		assertEquals(1, hint.getPlane());
	}

	@Test
	public void testAtLeastOneStepHasNonNullStatsWithLevels()
	{
		GuideData guide = DataLoader.loadAndJoin();
		boolean found = false;
		outer:
		for (GuideChapter chapter : guide.getChapters())
		{
			for (GuideSection section : chapter.getSections())
			{
				for (GuideStep step : section.getSteps())
				{
					StepStats stats = step.getStats();
					if (stats != null && stats.getLevels() != null && !stats.getLevels().isEmpty())
					{
						found = true;
						break outer;
					}
				}
			}
		}
		assertTrue("Expected at least one step with non-null stats containing non-empty levels", found);
	}

	@Test
	public void testNoExceptionThrownWhenLoading()
	{
		GuideData guide = DataLoader.loadAndJoin();
		assertNotNull(guide);
		assertNotNull(DataLoader.loadGuide());
		assertNotNull(DataLoader.loadSpatial());
		assertNotNull(DataLoader.loadStats());
	}

	private GuideStep findStep(GuideData guide, String id)
	{
		for (GuideChapter chapter : guide.getChapters())
		{
			for (GuideSection section : chapter.getSections())
			{
				for (GuideStep step : section.getSteps())
				{
					if (id.equals(step.getId()))
					{
						return step;
					}
				}
			}
		}
		return null;
	}
}
