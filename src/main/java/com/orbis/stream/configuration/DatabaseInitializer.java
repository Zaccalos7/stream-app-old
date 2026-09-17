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
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer {

    private final VideoSettingRepository videoSettingRepository;


    @Bean
    @Transactional
    public CommandLineRunner addDefaultVideoAndAudioSetting() {
        return args -> {

            Optional<VideoSetting> defaultVideoAndAudioTwitch = videoSettingRepository.findByDefaultPlatformConfiguration("Twitch");
            Optional<VideoSetting> defaultVideoAndAudioYoutube = videoSettingRepository.findByDefaultPlatformConfiguration("Youtube");

            boolean isPresentDefaultTwitchPlatformConfiguration = defaultVideoAndAudioTwitch.isPresent();
            boolean isPresentDefaultYoutubePlatformConfiguration = defaultVideoAndAudioYoutube.isPresent();

            AudioSetting audioSetting = AudioSetting.builder()
                    .audioCodec(86018)
                    .audioBitrate(128000)
                    .build();

            log.info("Initializing default VideoSetting...");

            if (!isPresentDefaultTwitchPlatformConfiguration) {
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
                        .defaultPlatformConfiguration("Twitch")
                        .build();

                videoSettingRepository.save(videoSetting);

                log.info("Default VideoSetting initialized for Twitch");
            }

            if (!isPresentDefaultYoutubePlatformConfiguration) {
                VideoSetting videoSettingYoutube = VideoSetting.builder()
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
                        .defaultPlatformConfiguration("Youtube")
                        .build();

                videoSettingRepository.save(videoSettingYoutube);
                log.info("Default VideoSetting initialized for Youtube");
            }
        };
    }
}