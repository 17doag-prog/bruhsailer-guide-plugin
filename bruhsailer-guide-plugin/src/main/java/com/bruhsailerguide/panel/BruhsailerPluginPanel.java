package com.bruhsailerguide.panel;

import com.bruhsailerguide.BruhsailerDataManager;
import com.bruhsailerguide.BruhsailerPlugin;
import com.bruhsailerguide.data.GuideChapter;
import com.bruhsailerguide.data.GuideData;
import com.bruhsailerguide.data.GuideSection;
import com.bruhsailerguide.data.GuideStep;
import com.bruhsailerguide.progress.ProgressTracker;
import java.awt.BorderLayout;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import net.runelite.client.ui.PluginPanel;

public class BruhsailerPluginPanel extends PluginPanel
{
	private final BruhsailerPlugin plugin;
	private final BruhsailerDataManager dataManager;
	private final ProgressTracker progressTracker;

	private final JTree tree;
	private final DefaultTreeModel treeModel;
	private final StepDetailPanel detailPanel;

	private String currentStepId;

	public BruhsailerPluginPanel(BruhsailerPlugin plugin, BruhsailerDataManager dataManager, ProgressTracker progressTracker)
	{
		super();
		this.plugin = plugin;
		this.dataManager = dataManager;
		this.progressTracker = progressTracker;

		setLayout(new BorderLayout());

		GuideData guide = dataManager.getGuide();
		if (guide == null || guide.getChapters() == null || guide.getChapters().isEmpty())
		{
			javax.swing.JLabel errorLabel = new javax.swing.JLabel("Data files missing. Please reinstall the plugin.");
			errorLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
			add(errorLabel, BorderLayout.NORTH);
		}

		DefaultMutableTreeNode root = new DefaultMutableTreeNode("BRUHsailer Guide");
		if (guide != null && guide.getChapters() != null)
		{
			for (GuideChapter chapter : guide.getChapters())
			{
				DefaultMutableTreeNode chapterNode = new DefaultMutableTreeNode(chapter.getTitle());
				if (chapter.getSections() != null)
				{
					for (GuideSection section : chapter.getSections())
					{
						DefaultMutableTreeNode sectionNode = new DefaultMutableTreeNode(section.getTitle());
						if (section.getSteps() != null)
						{
							for (GuideStep step : section.getSteps())
							{
								StepNode stepNodeObj = new StepNode(step.getId(), step.getNumber() + ". " + step.getText());
								sectionNode.add(new DefaultMutableTreeNode(stepNodeObj));
							}
						}
						chapterNode.add(sectionNode);
					}
				}
				root.add(chapterNode);
			}
		}

		treeModel = new DefaultTreeModel(root);
		tree = new JTree(treeModel);
		tree.setRootVisible(true);
		tree.setShowsRootHandles(true);
		tree.addTreeSelectionListener(new TreeSelectionListener()
		{
			@Override
			public void valueChanged(TreeSelectionEvent e)
			{
				onTreeSelectionChanged();
			}
		});

		JScrollPane treeScrollPane = new JScrollPane(tree);
		add(treeScrollPane, BorderLayout.CENTER);

		detailPanel = new StepDetailPanel();
		detailPanel.setOnCompleteToggle(selected ->
		{
			if (currentStepId != null)
			{
				progressTracker.setComplete(currentStepId, selected);
			}
		});
		add(detailPanel, BorderLayout.SOUTH);
	}

	private void onTreeSelectionChanged()
	{
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
		if (node == null)
		{
			return;
		}
		Object userObject = node.getUserObject();
		if (userObject instanceof StepNode)
		{
			StepNode stepNode = (StepNode) userObject;
			GuideStep step = dataManager.findStep(stepNode.id);
			if (step != null)
			{
				currentStepId = step.getId();
				detailPanel.update(step, findPreviousStep(step.getId()), progressTracker.isComplete(step.getId()));
				plugin.setCurrentStepId(step.getId());
			}
		}
	}

	private GuideStep findPreviousStep(String stepId)
	{
		GuideData guide = dataManager.getGuide();
		if (guide == null || guide.getChapters() == null)
		{
			return null;
		}

		GuideStep prev = null;
		for (GuideChapter chapter : guide.getChapters())
		{
			if (chapter.getSections() == null)
			{
				continue;
			}
			for (GuideSection section : chapter.getSections())
			{
				if (section.getSteps() == null)
				{
					continue;
				}
				for (GuideStep step : section.getSteps())
				{
					if (step.getId().equals(stepId))
					{
						return prev;
					}
					prev = step;
				}
			}
		}
		return null;
	}

	public void refresh()
	{
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
		for (int i = 0; i < tree.getRowCount(); i++)
		{
			tree.expandRow(i);
		}

		String stepId = plugin.getCurrentStepId();
		if (stepId != null)
		{
			DefaultMutableTreeNode node = findNodeByStepId(root, stepId);
			if (node != null)
			{
				TreePath path = new TreePath(node.getPath());
				tree.setSelectionPath(path);
				tree.scrollPathToVisible(path);
			}
			GuideStep step = dataManager.findStep(stepId);
			if (step != null)
			{
				detailPanel.update(step, findPreviousStep(stepId), progressTracker.isComplete(stepId));
			}
		}
	}

	private DefaultMutableTreeNode findNodeByStepId(DefaultMutableTreeNode node, String stepId)
	{
		Object userObject = node.getUserObject();
		if (userObject instanceof StepNode)
		{
			if (((StepNode) userObject).id.equals(stepId))
			{
				return node;
			}
		}

		for (int i = 0; i < node.getChildCount(); i++)
		{
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
			DefaultMutableTreeNode result = findNodeByStepId(child, stepId);
			if (result != null)
			{
				return result;
			}
		}
		return null;
	}

	JTree getTree()
	{
		return tree;
	}

	JCheckBox getCompleteCheckBox()
	{
		return detailPanel.getCompleteCheckBox();
	}

	private static class StepNode
	{
		final String id;
		final String label;

		StepNode(String id, String label)
		{
			this.id = id;
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}
}
