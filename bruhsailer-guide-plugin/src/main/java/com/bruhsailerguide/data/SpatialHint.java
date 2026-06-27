package com.bruhsailerguide.data;

import java.util.List;
import lombok.Data;

@Data
public class SpatialHint
{
	private String label;
	private List<Integer> npcIds;
	private List<Integer> objectIds;
	private int x;
	private int y;
	private int plane;
	private boolean unresolved;
	private String note;
}
