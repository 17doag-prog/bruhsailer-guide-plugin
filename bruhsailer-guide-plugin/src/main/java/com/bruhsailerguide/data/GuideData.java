package com.bruhsailerguide.data;

import java.util.List;
import lombok.Data;

@Data
public class GuideData
{
	private String sourceUrl;
	private String updatedOn;
	private List<GuideChapter> chapters;
}
