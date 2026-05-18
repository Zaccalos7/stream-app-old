package com.orbis.stream.controller;

import com.orbis.stream.component.LoggerMessageComponent;
import com.orbis.stream.record.VideoRecord;
import com.orbis.stream.record.VideoSettingsRecord;
import com.orbis.stream.service.VideoSettingService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/video-setting/")
@Slf4j
public class VideoSettingController {


    private final LoggerMessageComponent loggerMessageComponent;
    private final VideoSettingService videoSettingService;

    @GetMapping("getVideoSettings")
    @Operation(description = "restituisce tutti i setting per lo streaming video")
    public List<VideoSettingsRecord> getAllVideoSettings(@RequestParam Map<String, String> filtersMap){
        var response = videoSettingService.getAllVideoSettings(filtersMap);
        log.info(loggerMessageComponent.printMessage("got.video.settings"));
        return response;
    }

    @PostMapping("save")
    @Operation(summary = "creazione della configurazione del video")
    public ResponseEntity<Map<String,String>> saveVideoSettings(@RequestBody VideoSettingsRecord videoSettingsRecord){
        return videoSettingService.saveSettingsVideo(videoSettingsRecord);
    }

    @PostMapping("link")
    @Operation(summary = "collega un video a un setting")
    public ResponseEntity<Map<String,String>> linkAndsaveVideoSettings(@RequestBody VideoSettingsRecord videoSettingsRecord, @RequestParam Integer videoPkidToLink){
        return videoSettingService.linkAndSaveSettingsVideo(videoSettingsRecord, videoPkidToLink);
    }

    @PutMapping("edit")
    @Operation(summary = "modifica della configurazione del video")
    public ResponseEntity<Map<String,String>> editVideoSettings(
            @NotNull(message = "input.not.valid") @RequestParam Integer id,
            @NotNull(message = "input.not.valid") @RequestBody VideoSettingsRecord videoSettingDto){
        return videoSettingService.editSettingsVideo(videoSettingDto, id);
    }

    @DeleteMapping("delete")
    @Operation(summary = "cancella un setting audio-video")
    public ResponseEntity<Map<String, String>> deleteVideoSetting(@NotNull @RequestBody Integer id){
        return videoSettingService.deleteVideoSetting(id);
    }

}
