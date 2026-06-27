package com.bruhsailerguide.data;

import java.util.Map;
import lombok.Data;

@Data
public class StepStats
{
	private int gpChange;
	private String gpTotal;
	private Map<String, Integer> levels;
}
