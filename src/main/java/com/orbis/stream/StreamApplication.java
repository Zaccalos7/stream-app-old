package com.orbis.stream;

import lombok.extern.slf4j.Slf4j;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.CefInitializationException;
import me.friwi.jcefmaven.UnsupportedPlatformException;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@SpringBootApplication
public class StreamApplication {

	public static final String EMBEDDED_BROWSER_PROPERTY = "lazy-stream.embedded-browser";
	private static final String EMBEDDED_BROWSER_ENV = "LAZY_STREAM_EMBEDDED_BROWSER";
	private static final String BROWSER_LAUNCHER_THREAD_NAME = "lazy-stream-server";
	private static final String DATA_DIRECTORY_ENV = "LAZY_STREAM_DATA_DIR";
	private static final String DATA_DIRECTORY_PROPERTY = "lazy-stream.data-dir";

	private static CefApp cefApp;

	public static void main(String[] args) {
		configureWritablePaths();

		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("nux") || os.contains("nix")) {
			System.setProperty("sun.java2d.opengl", "false");
		}

		SpringApplicationBuilder builder = new SpringApplicationBuilder(StreamApplication.class);
		builder.headless(false);
		ConfigurableApplicationContext springContext = builder.run(args);

		if (shouldStartEmbeddedBrowser()) {
			SwingUtilities.invokeLater(() -> createAndShowGUI(springContext));
		} else {
			log.info("Embedded browser disabled: running in server-only mode");
		}
	}

	/**
	 * A jpackage application is normally started with its working directory set to /.
	 * Resolve the database and log paths before Spring creates its beans, otherwise
	 * SQLite tries to write to the read-only root filesystem.
	 */
	private static void configureWritablePaths() {
		Path dataDirectory = configuredDataDirectory();
		try {
			Files.createDirectories(dataDirectory.resolve("logs"));
		} catch (IOException exception) {
			throw new IllegalStateException("Impossibile creare la directory dati: " + dataDirectory, exception);
		}

		String normalizedDirectory = dataDirectory.toString().replace('\\', '/');
		setPropertyIfAbsent(DATA_DIRECTORY_PROPERTY, normalizedDirectory);
		setPropertyIfAbsent("spring.datasource.url", "jdbc:sqlite:" + normalizedDirectory + "/stream.db");
		setPropertyIfAbsent("logging.file.name", normalizedDirectory + "/logs/twitch.log");
	}

	private static Path configuredDataDirectory() {
		String configured = System.getProperty(DATA_DIRECTORY_PROPERTY);
		if (configured == null || configured.isBlank()) {
			configured = System.getenv(DATA_DIRECTORY_ENV);
		}
		if (configured != null && !configured.isBlank()) {
			return Paths.get(configured).toAbsolutePath().normalize();
		}

		Path workingDirectory = Paths.get("").toAbsolutePath().normalize();
		if (workingDirectory.getParent() != null && Files.isWritable(workingDirectory)) {
			return workingDirectory;
		}

		Path home = Paths.get(System.getProperty("user.home"));
		if (System.getProperty("os.name").toLowerCase().contains("mac")) {
			return home.resolve("Library").resolve("Application Support").resolve("lazy-stream");
		}
		return home.resolve(".orbis-stream");
	}

	private static void setPropertyIfAbsent(String key, String value) {
		String current = System.getProperty(key);
		if (current == null || current.isBlank()) {
			System.setProperty(key, value);
		}
	}

	/**
	 * The packaged browser launcher starts Spring Boot on a dedicated server thread and
	 * opens the default browser itself. Starting JCEF from that thread would create a
	 * second UI (and made the application appear twice on macOS).
	 *
	 * <p>The explicit property keeps the two launch modes independent. The thread-name
	 * fallback also supports the existing browser launcher, which predates the property.</p>
	 */
	private static boolean shouldStartEmbeddedBrowser() {
		String configured = System.getProperty(EMBEDDED_BROWSER_PROPERTY);
		if (configured == null) {
			configured = System.getenv(EMBEDDED_BROWSER_ENV);
		}

		if (configured != null && !configured.isBlank()) {
			return Boolean.parseBoolean(configured);
		}

		return !BROWSER_LAUNCHER_THREAD_NAME.equals(Thread.currentThread().getName());
	}

	private static void createAndShowGUI(ConfigurableApplicationContext springContext) {
		try {
			CefAppBuilder cefAppBuilder = new CefAppBuilder();
			cefAppBuilder.setInstallDir(new File(System.getProperty("user.home"), ".orbis-stream/jcef-bundle"));
			cefAppBuilder.getCefSettings().log_severity = org.cef.CefSettings.LogSeverity.LOGSEVERITY_DISABLE;
			cefAppBuilder.getCefSettings().windowless_rendering_enabled = true;

			cefApp = cefAppBuilder.build();
			CefClient cefClient = cefApp.createClient();

			CefBrowser browser = cefClient.createBrowser("http://localhost:1200/orbis/mainMenu", true, false);
			Component browserUI = browser.getUIComponent();

			JFrame frame = new JFrame("Orbis Stream");
			frame.getContentPane().add(browserUI, BorderLayout.CENTER);
			frame.setSize(1920, 1080);
			frame.setLocationRelativeTo(null);

			frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

			cefClient.addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
				public boolean doClose(CefClient client, CefBrowser b) {
					closeApplication(springContext, frame);
					return true;
				}
			});

			frame.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					closeApplication(springContext, frame);
				}
			});

			KeyStroke quitKey = KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
			frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(quitKey, "quit");
			frame.getRootPane().getActionMap().put("quit", new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					closeApplication(springContext, frame);
				}
			});

			frame.setVisible(true);
			browserUI.requestFocusInWindow();

		} catch (UnsupportedPlatformException | CefInitializationException | IOException | InterruptedException e) {
			log.error("Error during creation of gui:", e.getCause());
		}
	}

	private static void closeApplication(ConfigurableApplicationContext springContext, JFrame frame) {
		try {
			if (cefApp != null) {
				cefApp.dispose();
			}
		} catch (Exception e) {
			log.error("ERROR--->{}", e.getMessage(), e.getCause());
		}
		if (springContext != null) {
			springContext.close();
		}
		frame.dispose();
		System.exit(0);
	}
}