package com.bruhsailerguide;

import com.bruhsailerguide.data.DataLoader;
import com.bruhsailerguide.data.GuideData;
import com.bruhsailerguide.data.GuideChapter;
import com.bruhsailerguide.data.GuideSection;
import com.bruhsailerguide.data.GuideStep;
import com.bruhsailerguide.data.SpatialHint;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class BruhsailerDataManager
{
	private final GuideData guide;

	@Inject
	public BruhsailerDataManager()
	{
		GuideData loaded = null;
		try
		{
			loaded = DataLoader.loadAndJoin();
		}
		catch (Exception e)
		{
			log.error("Failed to load BRUHsailer Guide data files. The plugin will show an error state.", e);
		}
		this.guide = loaded;
	}

	public BruhsailerDataManager(GuideData guide)
	{
		this.guide = guide;
	}

	public GuideData getGuide()
	{
		return guide;
	}

	public GuideStep findStep(String id)
	{
		if (guide == null || guide.getChapters() == null)
		{
			return null;
		}
		for (GuideChapter chapter : guide.getChapters())
		{
			for (GuideSection section : chapter.getSections())
			{
				for (GuideStep step : section.getSteps())
				{
					if (step.getId().equals(id))
					{
						return step;
					}
				}
			}
		}
		return null;
	}

	public List<SpatialHint> getHintsForStep(String id)
	{
		GuideStep step = findStep(id);
		return step == null ? Collections.emptyList() : step.getHints();
	}
}
