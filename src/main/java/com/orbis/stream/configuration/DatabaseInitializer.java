package com.orbis.stream.configuration;

import com.orbis.stream.model.AudioSetting;
import com.orbis.stream.model.VideoSetting;
import com.orbis.stream.model.VideoSettingsOption;
import com.orbis.stream.repository.VideoSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

    private final VideoSettingRepository videoSettingRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (!videoSettingRepository.existsById(1)) {
            log.info("Initializing default VideoSetting...");
            AudioSetting audioSetting = AudioSetting.builder()
                    .audioCodec(86018)
                    .audioBitrate(128000)
                    .build();

            VideoSetting videoSetting = VideoSetting.builder()
                    .id(1)
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
            log.info("Default VideoSetting initialized.");
        }
    }
}
