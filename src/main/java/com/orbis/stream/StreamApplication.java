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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

@Slf4j
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@SpringBootApplication
public class StreamApplication {

	public static void main(String[] args) {
		SpringApplicationBuilder builder = new SpringApplicationBuilder(StreamApplication.class);
		builder.headless(false);
		ConfigurableApplicationContext springContext = builder.run(args);

		SwingUtilities.invokeLater(() -> createAndShowGUI(springContext));
	}

	private static void createAndShowGUI(ConfigurableApplicationContext springContext) {
		try {
			CefAppBuilder cefAppBuilder = new CefAppBuilder();
			cefAppBuilder.setInstallDir(new File("jcef-bundle"));
			cefAppBuilder.getCefSettings().log_severity = org.cef.CefSettings.LogSeverity.LOGSEVERITY_DISABLE;

			CefApp cefApp = cefAppBuilder.build();
			CefClient cefClient = cefApp.createClient();

			CefBrowser browser = cefClient.createBrowser("http://localhost:1200/orbis/handler", false, false);
			Component browserUI = browser.getUIComponent();

			JFrame frame = new JFrame("Orbis Stream");
			frame.getContentPane().add(browserUI, BorderLayout.CENTER);
			frame.setSize(1920, 1080);
			frame.setLocationRelativeTo(null);

			frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

			cefClient.addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
				public boolean doClose(CefClient client, CefBrowser browser) {
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

			frame.setVisible(true);

		} catch (UnsupportedPlatformException | CefInitializationException | IOException | InterruptedException e) {
			log.error("Error during creation of gui:", e.getCause());
		}
	}

	private static void closeApplication(ConfigurableApplicationContext springContext, JFrame frame) {
		try {
			CefApp.getInstance().dispose();
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