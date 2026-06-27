package com.bruhsailerguide.data;

import java.util.List;
import lombok.Data;

@Data
public class GuideSection
{
	private int index;
	private String title;
	private List<GuideStep> steps;
}
