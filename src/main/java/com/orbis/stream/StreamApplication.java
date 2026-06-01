package com.orbis.stream;

import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.web.WebView;




@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@SpringBootApplication
public class StreamApplication extends Application {

	private ConfigurableApplicationContext springContext;

	public static void main(String[] args) {
		Application.launch(StreamApplication.class, args);
	}

	@Override
	public void init() {
		SpringApplicationBuilder builder = new SpringApplicationBuilder(StreamApplication.class);
		builder.headless(false);
		springContext = builder.run();
	}

	@Override
	public void start(Stage stage) {
		WebView webView = new WebView();
		webView.getEngine().load("http://localhost:1200/orbis/handler");

		Scene scene = new Scene(webView, 1920, 1080);
		stage.setScene(scene);

		stage.setOnCloseRequest(event -> {
			if (springContext != null) {
				springContext.close();
			}
			System.exit(0);
		});

		stage.show();
	}

	@Override
	public void stop() {
		if (springContext != null) {
			springContext.close();
		}
	}
}
