package com.bruhsailerguide.data;

import java.util.List;
import lombok.Data;

@Data
public class GuideStep
{
	private String id;
	private int number;
	private String text;
	private String gpStack;
	private String itemsNeeded;
	private String totalTime;
	private List<SpatialHint> hints;
	private StepStats stats;
}
