package com.bruhsailerguide.panel;

import com.bruhsailerguide.BruhsailerDataManager;
import com.bruhsailerguide.BruhsailerPlugin;
import com.bruhsailerguide.data.GuideChapter;
import com.bruhsailerguide.data.GuideData;
import com.bruhsailerguide.data.GuideSection;
import com.bruhsailerguide.data.GuideStep;
import com.bruhsailerguide.data.StepStats;
import com.bruhsailerguide.progress.ProgressTracker;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.JCheckBox;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class BruhsailerPluginPanelTest
{
	private BruhsailerPlugin plugin;
	private BruhsailerDataManager dataManager;
	private ProgressTracker progressTracker;
	private BruhsailerPluginPanel panel;

	@Before
	public void setUp()
	{
		plugin = new BruhsailerPlugin();
		dataManager = new BruhsailerDataManager(createStubGuideData());
		progressTracker = new FakeProgressTracker();
		panel = new BruhsailerPluginPanel(plugin, dataManager, progressTracker);
	}

	@Test
	public void treeHasNodes()
	{
		JTree tree = panel.getTree();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
		assertNotNull(root);
		assertEquals(1, root.getChildCount());

		DefaultMutableTreeNode chapterNode = (DefaultMutableTreeNode) root.getFirstChild();
		assertEquals(1, chapterNode.getChildCount());

		DefaultMutableTreeNode sectionNode = (DefaultMutableTreeNode) chapterNode.getFirstChild();
		assertEquals(2, sectionNode.getChildCount());
	}

	@Test
	public void selectingStepUpdatesCurrentStepId()
	{
		JTree tree = panel.getTree();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
		DefaultMutableTreeNode sectionNode = (DefaultMutableTreeNode) ((DefaultMutableTreeNode) root.getFirstChild()).getFirstChild();
		DefaultMutableTreeNode stepNode = (DefaultMutableTreeNode) sectionNode.getFirstChild();

		TreePath path = new TreePath(stepNode.getPath());
		tree.setSelectionPath(path);

		assertEquals("new-1-1", plugin.getCurrentStepId());
	}

	@Test
	public void checkboxPersistsCompletion()
	{
		JTree tree = panel.getTree();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
		DefaultMutableTreeNode sectionNode = (DefaultMutableTreeNode) ((DefaultMutableTreeNode) root.getFirstChild()).getFirstChild();
		DefaultMutableTreeNode stepNode = (DefaultMutableTreeNode) sectionNode.getFirstChild();
		tree.setSelectionPath(new TreePath(stepNode.getPath()));

		JCheckBox checkBox = panel.getCompleteCheckBox();
		assertNotNull(checkBox);

		checkBox.setSelected(true);
		for (java.awt.event.ActionListener l : checkBox.getActionListeners())
		{
			l.actionPerformed(new java.awt.event.ActionEvent(checkBox, java.awt.event.ActionEvent.ACTION_PERFORMED, ""));
		}
		assertTrue(progressTracker.isComplete("new-1-1"));

		checkBox.setSelected(false);
		for (java.awt.event.ActionListener l : checkBox.getActionListeners())
		{
			l.actionPerformed(new java.awt.event.ActionEvent(checkBox, java.awt.event.ActionEvent.ACTION_PERFORMED, ""));
		}
		assertFalse(progressTracker.isComplete("new-1-1"));
	}

	private GuideData createStubGuideData()
	{
		GuideStep step1 = new GuideStep();
		step1.setId("new-1-1");
		step1.setNumber(1);
		step1.setText("Step 1 text");
		step1.setGpStack("1000");
		step1.setItemsNeeded("None");
		step1.setTotalTime("5m");
		StepStats stats1 = new StepStats();
		stats1.setGpChange(100);
		stats1.setGpTotal("1100");
		stats1.setLevels(Collections.emptyMap());
		step1.setStats(stats1);
		step1.setHints(Collections.emptyList());

		GuideStep step2 = new GuideStep();
		step2.setId("new-1-2");
		step2.setNumber(2);
		step2.setText("Step 2 text");
		step2.setGpStack("2000");
		step2.setItemsNeeded("Hammer");
		step2.setTotalTime("10m");
		StepStats stats2 = new StepStats();
		stats2.setGpChange(-50);
		stats2.setGpTotal("1050");
		Map<String, Integer> levels = new HashMap<>();
		levels.put("Attack", 2);
		stats2.setLevels(levels);
		step2.setStats(stats2);
		step2.setHints(Collections.emptyList());

		GuideSection section = new GuideSection();
		section.setIndex(1);
		section.setTitle("Section 1");
		section.setSteps(Arrays.asList(step1, step2));

		GuideChapter chapter = new GuideChapter();
		chapter.setIndex(1);
		chapter.setTitle("Chapter 1");
		chapter.setSections(Collections.singletonList(section));

		GuideData data = new GuideData();
		data.setSourceUrl("test");
		data.setUpdatedOn("2024-01-01");
		data.setChapters(Collections.singletonList(chapter));
		return data;
	}

	private static class FakeProgressTracker extends ProgressTracker
	{
		private final Set<String> completed = new HashSet<>();

		FakeProgressTracker()
		{
			super(null);
		}

		@Override
		public boolean isComplete(String stepId)
		{
			return completed.contains(stepId);
		}

		@Override
		public void setComplete(String stepId, boolean complete)
		{
			if (complete)
			{
				completed.add(stepId);
			}
			else
			{
				completed.remove(stepId);
			}
		}

		@Override
		public Set<String> getAllCompleted()
		{
			return new HashSet<>(completed);
		}
	}
}
