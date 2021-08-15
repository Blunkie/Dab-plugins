package net.runelite.client.plugins.elbreakhandler.ui;

import net.runelite.client.plugins.elbreakhandler.ElBreakHandler;
import net.runelite.client.plugins.elbreakhandler.ElBreakHandlerPlugin;
import net.runelite.client.plugins.elbreakhandler.ui.utils.ConfigPanel;
import net.runelite.client.plugins.elbreakhandler.ui.utils.JMultilineLabel;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.disposables.Disposable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

public class ElBreakHandlerPanel extends PluginPanel
{
	public final static Color PANEL_BACKGROUND_COLOR = ColorScheme.DARK_GRAY_COLOR;
	final static Color BACKGROUND_COLOR = ColorScheme.DARKER_GRAY_COLOR;

	static final Font NORMAL_FONT = FontManager.getRunescapeFont();
	static final Font SMALL_FONT = FontManager.getRunescapeSmallFont();

	private static final ImageIcon HELP_ICON;
	private static final ImageIcon HELP_HOVER_ICON;

	static
	{
		final BufferedImage helpIcon =
			ImageUtil.recolorImage(
				ImageUtil.loadImageResource(ElBreakHandlerPlugin.class, "help.png"), ColorScheme.BRAND_BLUE
			);
		HELP_ICON = new ImageIcon(helpIcon);
		HELP_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(helpIcon, 0.53f));
	}

	private final ElBreakHandlerPlugin elBreakHandlerPlugin;
	private final ElBreakHandler elBreakHandler;
	private final ConfigPanel configPanel;

	public @NonNull Disposable pluginDisposable;
	public @NonNull Disposable activeDisposable;
	public @NonNull Disposable currentDisposable;
	public @NonNull Disposable startDisposable;
	public @NonNull Disposable configDisposable;

	private final JPanel unlockAccountPanel = new JPanel(new BorderLayout());
	private final JPanel breakTimingsPanel = new JPanel(new GridLayout(0, 1));

	@Inject
	private ElBreakHandlerPanel(ElBreakHandlerPlugin elBreakHandlerPlugin, ElBreakHandler elBreakHandler, ConfigPanel configPanel)
	{
		super(false);

		configPanel.init(elBreakHandlerPlugin.getOptionsConfig());

		this.elBreakHandlerPlugin = elBreakHandlerPlugin;
		this.elBreakHandler = elBreakHandler;
		this.configPanel = configPanel;

		pluginDisposable = elBreakHandler
			.getPluginObservable()
			.subscribe((Map<Plugin, Boolean> plugins) ->
				SwingUtil.syncExec(() ->
					buildPanel(plugins)));

		activeDisposable = elBreakHandler
			.getActiveObservable()
			.subscribe(
				(ignored) ->
					SwingUtil.syncExec(() ->
						buildPanel(elBreakHandler.getPlugins()))
			);

		currentDisposable = elBreakHandler
			.getActiveBreaksObservable()
			.subscribe(
				(ignored) ->
					SwingUtil.syncExec(() ->
						buildPanel(elBreakHandler.getPlugins()))
			);

		startDisposable = elBreakHandler
			.getActiveObservable()
			.subscribe(
				(ignored) ->
					SwingUtil.syncExec(() -> {
						unlockAccountsPanel();
						unlockAccountPanel.revalidate();
						unlockAccountPanel.repaint();

						breakTimingsPanel();
						breakTimingsPanel.revalidate();
						breakTimingsPanel.repaint();
					})
			);

		configDisposable = elBreakHandler
			.configChanged
			.subscribe(
				(ignored) ->
					SwingUtil.syncExec(() -> {
						unlockAccountsPanel();
						unlockAccountPanel.revalidate();
						unlockAccountPanel.repaint();
					})
			);

		this.setBackground(PANEL_BACKGROUND_COLOR);
		this.setLayout(new BorderLayout());

		buildPanel(elBreakHandler.getPlugins());
	}

	void buildPanel(Map<Plugin, Boolean> plugins)
	{
		removeAll();

		if (plugins.isEmpty())
		{
			PluginErrorPanel errorPanel = new PluginErrorPanel();
			errorPanel.setContent("El Break Handler", "There were no plugins that registered themselves with the break handler.");

			add(errorPanel, BorderLayout.NORTH);
		}
		else
		{
			JPanel contentPanel = new JPanel(new BorderLayout());

			contentPanel.add(statusPanel(), BorderLayout.NORTH);
			contentPanel.add(tabbedPane(plugins), BorderLayout.CENTER);

			add(titleBar(), BorderLayout.NORTH);
			add(contentPanel, BorderLayout.CENTER);
		}

		revalidate();
		repaint();
	}

	private JPanel titleBar()
	{
		JPanel titlePanel = new JPanel(new BorderLayout());
		titlePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

		JLabel title = new JLabel();
		JLabel help = new JLabel(HELP_ICON);

		title.setText("El Break Handler");
		title.setForeground(Color.WHITE);

		help.setToolTipText("Info");
		help.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				JOptionPane.showMessageDialog(
						ClientUI.getFrame(),
						"<html><center>The configs in this panel can be used to <b>schedule</b> breaks.<br>" +
								"When the timer hits zero a break is scheduled. This does not mean that the break will be taken immediately!<br>" +
								"Plugins decide what the best time is for a break, for example a NMZ dream will be finished instead of interrupted.</center></html>",
						"El Break Handler",
						JOptionPane.QUESTION_MESSAGE
				);
			}

			@Override
			public void mouseEntered(MouseEvent mouseEvent)
			{
				help.setIcon(HELP_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent mouseEvent)
			{
				help.setIcon(HELP_ICON);
			}
		});
		help.setBorder(new EmptyBorder(0, 3, 0, 0));

		titlePanel.add(title, BorderLayout.WEST);
		titlePanel.add(help, BorderLayout.EAST);

		return titlePanel;
	}

	private boolean unlockAccountsPanel()
	{
		unlockAccountPanel.removeAll();

		Set<Plugin> activePlugins = elBreakHandler.getActivePlugins();

		boolean manual = Boolean.parseBoolean(elBreakHandlerPlugin.getConfigManager().getConfiguration(ElBreakHandlerPlugin.CONFIG_GROUP, "accountselection"));

		String data = ElBreakHandlerPlugin.data;

		if (activePlugins.isEmpty() || manual || (data != null && !data.trim().isEmpty()))
		{
			return false;
		}

		JPanel titleWrapper = new JPanel(new BorderLayout());
		titleWrapper.setBackground(new Color(125, 40, 40));
		titleWrapper.setBorder(new CompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(115, 30, 30)),
				BorderFactory.createLineBorder(new Color(125, 40, 40))
		));

		JLabel title = new JLabel();
		title.setText("Warning");
		title.setFont(NORMAL_FONT);
		title.setPreferredSize(new Dimension(0, 24));
		title.setForeground(Color.WHITE);
		title.setBorder(new EmptyBorder(0, 8, 0, 0));

		titleWrapper.add(title, BorderLayout.CENTER);

		unlockAccountPanel.add(titleWrapper, BorderLayout.NORTH);

		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.setBackground(new Color(125, 40, 40));

		JMultilineLabel description = new JMultilineLabel();

		description.setText("Please make sure to unlock your profiles plugins data in the account tab!");
		description.setFont(SMALL_FONT);
		description.setDisabledTextColor(Color.WHITE);
		description.setBackground(new Color(115, 30, 30));

		description.setBorder(new EmptyBorder(5, 5, 10, 5));

		contentPanel.add(description, BorderLayout.CENTER);

		unlockAccountPanel.add(contentPanel, BorderLayout.CENTER);

		return true;
	}

	private boolean breakTimingsPanel()
	{
		breakTimingsPanel.removeAll();

		Set<Plugin> pluginStream = elBreakHandler.getActivePlugins().stream().filter(e -> !elBreakHandlerPlugin.isValidBreak(e)).collect(Collectors.toSet());

		if (pluginStream.isEmpty())
		{
			return false;
		}

		for (Plugin plugin : pluginStream)
		{
			JPanel wrapperPanel = new JPanel(new BorderLayout());

			JPanel titleWrapper = new JPanel(new BorderLayout());
			titleWrapper.setBackground(new Color(125, 40, 40));
			titleWrapper.setBorder(new CompoundBorder(
					BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(115, 30, 30)),
					BorderFactory.createLineBorder(new Color(125, 40, 40))
			));

			JLabel title = new JLabel();
			title.setText("Warning");
			title.setFont(NORMAL_FONT);
			title.setPreferredSize(new Dimension(0, 24));
			title.setForeground(Color.WHITE);
			title.setBorder(new EmptyBorder(0, 8, 0, 0));

			titleWrapper.add(title, BorderLayout.CENTER);

			wrapperPanel.add(titleWrapper, BorderLayout.NORTH);

			JPanel contentPanel = new JPanel(new BorderLayout());
			contentPanel.setBackground(new Color(125, 40, 40));

			JMultilineLabel description = new JMultilineLabel();

			description.setText("The break timings for " + plugin.getName() + " are invalid!");
			description.setFont(SMALL_FONT);
			description.setDisabledTextColor(Color.WHITE);
			description.setBackground(new Color(115, 30, 30));

			description.setBorder(new EmptyBorder(5, 5, 10, 5));

			contentPanel.add(description, BorderLayout.CENTER);

			wrapperPanel.add(contentPanel, BorderLayout.CENTER);

			breakTimingsPanel.add(wrapperPanel);
		}

		return true;
	}

	private JPanel statusPanel()
	{
		Set<Plugin> activePlugins = elBreakHandler.getActivePlugins();

		JPanel contentPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		if (unlockAccountsPanel())
		{
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			c.gridy += 1;
			c.insets = new Insets(5, 10, 0, 10);

			contentPanel.add(unlockAccountPanel, c);
		}

		if (breakTimingsPanel())
		{
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			c.gridy += 1;
			c.insets = new Insets(5, 10, 0, 10);

			contentPanel.add(breakTimingsPanel, c);
		}

		if (activePlugins.isEmpty())
		{
			return contentPanel;
		}

		for (Plugin plugin : activePlugins)
		{
			ElBreakHandlerStatusPanel statusPanel = new ElBreakHandlerStatusPanel(elBreakHandlerPlugin, elBreakHandler, plugin);

			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			c.gridy += 1;
			c.insets = new Insets(5, 10, 0, 10);

			contentPanel.add(statusPanel, c);
		}

		JButton scheduleBreakButton = new JButton("Schedule break now");

		if (activePlugins.size() > 0)
		{
			scheduleBreakButton.addActionListener(e -> activePlugins.forEach(plugin -> {
				if (!elBreakHandler.isBreakActive(plugin))
				{
					elBreakHandler.planBreak(plugin, Instant.now());
				}
			}));

			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			c.gridy += 1;
			c.insets = new Insets(5, 10, 0, 10);

			contentPanel.add(scheduleBreakButton, c);
		}

		return contentPanel;
	}

	private JTabbedPane tabbedPane(Map<Plugin, Boolean> plugins)
	{
		JTabbedPane mainTabPane = new JTabbedPane();

		JScrollPane pluginPanel = wrapContainer(contentPane(plugins));
		JScrollPane repositoryPanel = wrapContainer(new ElBreakHandlerAccountPanel(elBreakHandlerPlugin, elBreakHandler));
		JScrollPane optionsPanel = wrapContainer(configPanel);

		mainTabPane.add("Plugins", pluginPanel);
		mainTabPane.add("Accounts", repositoryPanel);
		mainTabPane.add("Options", optionsPanel);

		return mainTabPane;
	}

	private JPanel contentPane(Map<Plugin, Boolean> plugins)
	{
		JPanel contentPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		if (elBreakHandler.getPlugins().isEmpty())
		{
			return contentPanel;
		}

		for (Map.Entry<Plugin, Boolean> plugin : plugins.entrySet())
		{
			ElBreakHandlerPluginPanel panel = new ElBreakHandlerPluginPanel(elBreakHandlerPlugin, plugin.getKey(), plugin.getValue());

			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			c.gridy += 1;
			c.insets = new Insets(5, 10, 0, 10);

			contentPanel.add(panel, c);
		}

		return contentPanel;
	}

	public static JScrollPane wrapContainer(final JPanel container)
	{
		final JPanel wrapped = new JPanel(new BorderLayout());
		wrapped.add(container, BorderLayout.NORTH);
		wrapped.setBackground(PANEL_BACKGROUND_COLOR);

		final JScrollPane scroller = new JScrollPane(wrapped);
		scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroller.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
		scroller.setBackground(PANEL_BACKGROUND_COLOR);

		return scroller;
	}
}