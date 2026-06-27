package com.bruhsailerguide.panel;

import com.bruhsailerguide.data.GuideStep;
import com.bruhsailerguide.data.StepStats;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

class StepDetailPanel extends JPanel
{
	private final JLabel headerLabel;
	private final JTextArea textArea;
	private final JLabel gpStackLabel;
	private final JLabel itemsNeededLabel;
	private final JLabel totalTimeLabel;
	private final JLabel gpChangeLabel;
	private final JLabel gpTotalLabel;
	private final JLabel skillChangesLabel;
	private final JCheckBox completeCheckBox;

	StepDetailPanel()
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		headerLabel = new JLabel("Select a step");
		headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
		add(headerLabel);

		textArea = new JTextArea(5, 20);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setEditable(false);
		add(new JScrollPane(textArea));

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
		add(completeCheckBox);
	}

	void setOnCompleteToggle(Consumer<Boolean> listener)
	{
		for (java.awt.event.ActionListener l : completeCheckBox.getActionListeners())
		{
			completeCheckBox.removeActionListener(l);
		}
		completeCheckBox.addActionListener(e -> listener.accept(completeCheckBox.isSelected()));
	}

	void update(GuideStep step, GuideStep previousStep, boolean complete)
	{
		headerLabel.setText(step.getNumber() + ". " + step.getText());
		textArea.setText(step.getText());

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
