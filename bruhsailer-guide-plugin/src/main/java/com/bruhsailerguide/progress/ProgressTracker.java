package com.bruhsailerguide.progress;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

@Singleton
public class ProgressTracker
{
	private static final String CONFIG_GROUP = "bruhsailerguide";
	private static final String COMPLETED_KEY = "completedSteps";
	private static final Gson GSON = new Gson();

	private final ConfigManager configManager;

	@Inject
	public ProgressTracker(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	public boolean isComplete(String stepId)
	{
		return getAllCompleted().contains(stepId);
	}

	public void setComplete(String stepId, boolean complete)
	{
		Set<String> completed = getAllCompleted();
		if (complete)
		{
			completed.add(stepId);
		}
		else
		{
			completed.remove(stepId);
		}
		save(completed);
	}

	public Set<String> getAllCompleted()
	{
		String json = configManager.getConfiguration(CONFIG_GROUP, COMPLETED_KEY);
		if (json == null || json.isEmpty())
		{
			return new HashSet<>();
		}
		return GSON.fromJson(json, new TypeToken<Set<String>>() {}.getType());
	}

	private void save(Set<String> completed)
	{
		String json = GSON.toJson(completed);
		configManager.setConfiguration(CONFIG_GROUP, COMPLETED_KEY, json);
	}
}
