package com.bruhsailerguide.panel;

import com.bruhsailerguide.data.GuideAction;
import com.bruhsailerguide.data.GuideStep;
import com.bruhsailerguide.data.StepStats;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

class StepDetailPanel extends JPanel
{
	private final JLabel headerLabel;
	private final JPanel actionListPanel;
	private final JLabel gpStackLabel;
	private final JLabel itemsNeededLabel;
	private final JLabel totalTimeLabel;
	private final JLabel gpChangeLabel;
	private final JLabel gpTotalLabel;
	private final JLabel skillChangesLabel;
	private final JCheckBox completeCheckBox;

	private BiConsumer<String, Boolean> actionToggleListener;
	private Consumer<Boolean> stepToggleListener;

	StepDetailPanel()
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		headerLabel = new JLabel("Select a step");
		headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
		add(headerLabel);

		actionListPanel = new JPanel();
		actionListPanel.setLayout(new BoxLayout(actionListPanel, BoxLayout.Y_AXIS));
		add(new JScrollPane(actionListPanel));

		gpStackLabel = new JLabel();
		add(gpStackLabel);
		itemsNeededLabel = new JLabel();
		add(itemsNeededLabel);
		totalTimeLabel = new JLabel();
		add(totalTimeLabel);
		gpChangeLabel = new JLabel();
		add(gpChangeLabel);
		gpTotalLabel = new JLabel();
		add(gpTotalLabel);
		skillChangesLabel = new JLabel();
		add(skillChangesLabel);

		completeCheckBox = new JCheckBox("Mark complete");
		completeCheckBox.addActionListener(e -> {
			if (stepToggleListener != null)
			{
				stepToggleListener.accept(completeCheckBox.isSelected());
			}
		});
		add(completeCheckBox);
	}

	void setOnActionToggle(BiConsumer<String, Boolean> listener)
	{
		this.actionToggleListener = listener;
	}

	void setOnCompleteToggle(Consumer<Boolean> listener)
	{
		this.stepToggleListener = listener;
	}

	void update(GuideStep step, GuideStep previousStep, boolean complete, Set<String> completedActionIds)
	{
		headerLabel.setText(step.getNumber() + ". " + step.getText());

		actionListPanel.removeAll();
		List<GuideAction> actions = step.getActions() != null ? step.getActions() : Collections.emptyList();
		if (actions.isEmpty())
		{
			actionListPanel.add(new JLabel(step.getText()));
		}
		else
		{
			for (GuideAction action : actions)
			{
				JCheckBox actionCheckBox = new JCheckBox(action.getText());
				actionCheckBox.setSelected(completedActionIds != null && completedActionIds.contains(action.getId()));
				actionCheckBox.addActionListener(e -> {
					if (actionToggleListener != null)
					{
						actionToggleListener.accept(action.getId(), actionCheckBox.isSelected());
					}
				});
				actionListPanel.add(actionCheckBox);
			}
		}
		actionListPanel.revalidate();
		actionListPanel.repaint();

		gpStackLabel.setText("GP Stack: " + (step.getGpStack() != null ? step.getGpStack() : "-"));
		itemsNeededLabel.setText("Items: " + (step.getItemsNeeded() != null ? step.getItemsNeeded() : "-"));
		totalTimeLabel.setText("Time: " + (step.getTotalTime() != null ? step.getTotalTime() : "-"));

		StepStats stats = step.getStats();
		if (stats != null)
		{
			int gpChange = stats.getGpChange();
			gpChangeLabel.setText("GP Change: " + (gpChange >= 0 ? "+" : "") + gpChange);
			gpChangeLabel.setForeground(gpChange >= 0 ? Color.GREEN.darker() : Color.RED);

			gpTotalLabel.setText("GP Total: " + (stats.getGpTotal() != null ? stats.getGpTotal() : "-"));
			skillChangesLabel.setText("Top Skills: " + formatTopSkillChanges(step, previousStep));
		}
		else
		{
			gpChangeLabel.setText("GP Change: -");
			gpChangeLabel.setForeground(getForeground());
			gpTotalLabel.setText("GP Total: -");
			skillChangesLabel.setText("Top Skills: -");
		}

		completeCheckBox.setSelected(complete);
	}

	void setAllActionsSelected(boolean selected)
	{
		for (int i = 0; i < actionListPanel.getComponentCount(); i++)
		{
			if (actionListPanel.getComponent(i) instanceof JCheckBox)
			{
				JCheckBox cb = (JCheckBox) actionListPanel.getComponent(i);
				// Avoid firing listener recursively by temporarily removing it.
				java.awt.event.ActionListener[] listeners = cb.getActionListeners();
				for (java.awt.event.ActionListener l : listeners)
				{
					cb.removeActionListener(l);
				}
				cb.setSelected(selected);
				for (java.awt.event.ActionListener l : listeners)
				{
					cb.addActionListener(l);
				}
			}
		}
	}

	JCheckBox getCompleteCheckBox()
	{
		return completeCheckBox;
	}

	private static String formatTopSkillChanges(GuideStep current, GuideStep previous)
	{
		if (previous == null || previous.getStats() == null || current.getStats() == null)
		{
			return "-";
		}

		Map<String, Integer> prevLevels = previous.getStats().getLevels();
		Map<String, Integer> currLevels = current.getStats().getLevels();
		if (prevLevels == null)
		{
			prevLevels = Collections.emptyMap();
		}
		if (currLevels == null)
		{
			currLevels = Collections.emptyMap();
		}

		List<Map.Entry<String, Integer>> changes = new ArrayList<>();
		for (Map.Entry<String, Integer> entry : currLevels.entrySet())
		{
			String skill = entry.getKey();
			int prev = prevLevels.getOrDefault(skill, 0);
			int diff = entry.getValue() - prev;
			if (diff != 0)
			{
				changes.add(new java.util.AbstractMap.SimpleEntry<>(skill, diff));
			}
		}

		changes.sort((a, b) -> Integer.compare(Math.abs(b.getValue()), Math.abs(a.getValue())));

		if (changes.isEmpty())
		{
			return "-";
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < Math.min(3, changes.size()); i++)
		{
			Map.Entry<String, Integer> e = changes.get(i);
			if (i > 0)
			{
				sb.append(", ");
			}
			sb.append(e.getKey()).append(" ").append(e.getValue() > 0 ? "+" : "").append(e.getValue());
		}
		return sb.toString();
	}
}
