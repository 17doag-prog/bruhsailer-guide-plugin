package com.bruhsailerguide.data;

import java.util.List;
import lombok.Data;

@Data
public class GuideChapter
{
	private int index;
	private String title;
	private List<GuideSection> sections;
}
