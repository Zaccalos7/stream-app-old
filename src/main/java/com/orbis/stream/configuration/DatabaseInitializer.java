package com.orbis.stream.configuration;

import com.orbis.stream.model.AudioSetting;
import com.orbis.stream.model.VideoSetting;
import com.orbis.stream.model.VideoSettingsOption;
import com.orbis.stream.repository.VideoSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer {

    private final VideoSettingRepository videoSettingRepository;


    @Bean
    @Transactional
    public CommandLineRunner addDefaultVideoAndAudioSetting() {
        return args -> {
            log.info("Initializing default VideoSetting...");
            initializeDefaultVideoSetting("Twitch");
            initializeDefaultVideoSetting("Youtube");
        };
    }

    private void initializeDefaultVideoSetting(String platform) {
        List<VideoSetting> existing = videoSettingRepository.findByIsDefaultConfigurationTrueAndDefaultPlatformConfiguration(platform);

        if (existing.size() > 1) {
            videoSettingRepository.deleteAll(existing.subList(1, existing.size()));
            videoSettingRepository.flush();
        }

        if (!existing.isEmpty()) {
            return;
        }

        AudioSetting audioSetting = AudioSetting.builder()
                .audioCodec(86018)
                .audioBitrate(128000)
                .build();

        VideoSetting videoSetting = VideoSetting.builder()
                .title("test")
                .videoCodec(27)
                .videoCodecName("libx264")
                .pixelFormat(0)
                .videoBitrate(5000000)
                .lastModified(null)
                .isVideoAndAudioSettingActive(true)
                .gopSize(2)
                .videoSettingsOptions(List.of(
                        VideoSettingsOption.builder().key("preset").value("ultrafast").build(),
                        VideoSettingsOption.builder().key("tune").value("zerolatency").build()
                ))
                .videoFormat("flv")
                .audioSetting(audioSetting)
                .isDefaultConfiguration(true)
                .defaultPlatformConfiguration(platform)
                .build();

        videoSettingRepository.save(videoSetting);

        log.info("Default VideoSetting initialized for {}", platform);
    }
}