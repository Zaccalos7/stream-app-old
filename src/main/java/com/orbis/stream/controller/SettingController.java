package com.orbis.stream.controller;

import com.orbis.stream.component.LoggerMessageComponent;
import com.orbis.stream.dto.SettingDto;
import com.orbis.stream.record.SettingRecord;
import com.orbis.stream.record.output.VideoPathRecord;
import com.orbis.stream.service.SettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@CrossOrigin(origins = "*")
@RestController
@Slf4j
@RequestMapping("/settings/")
@Tag(name="Setting for streaming")
@RequiredArgsConstructor
public class SettingController {

    private final SettingService settingService;
    private final LoggerMessageComponent loggerMessageComponent;

    @PostMapping("save")
    @Operation(summary = "Endpoint per il salvataggio delle impostazioni dello streaming, tutti i parametri sono required",
            description = "Restituisce l'esito dell'operazione")
    public ResponseEntity<Map<String, String>> saveSettings(@Valid @RequestBody SettingRecord settingRecord){
        var response = settingService.addNewConfiguration(settingRecord);
        log.info(loggerMessageComponent.printMessage("setting.created"));

        return response;
    }

    @PutMapping("change")
    @Operation(summary = "Endpoint per la modifica della configurazione di streaming, tutti i parametri sono required",
            description = "Restituisce l'esito dell'operazione")
    public ResponseEntity<Map<String, String>> changeSettings(@RequestParam Integer id, @RequestBody SettingRecord settingRecord){
        var response = settingService.modifySetting(id, settingRecord);

        log.info(loggerMessageComponent.printMessage("setting.update"));

        return response;
    }

    @GetMapping("retrive")
    @Operation(summary = "Endpoint per prendere tutte le configurazioni associate a quell'utente",
            description = "Restituisce la lista delle configurazioni dell'utente")
    public List<SettingDto> retriveSettings(@RequestParam Map<String, String> filtersMap){
        log.info(loggerMessageComponent.printMessage("retrieves.configurations"));
        return settingService.retrieveSettings(filtersMap);
    }

    @GetMapping("retrive-channels")
    @Operation(summary = "Endpoint per prendere tutte le configurazioni associate a quell'utente",
            description = "Restituisce la lista dei canali delle configurazioni dell'utente")
    public Set<String> retriveAllChannel(@RequestParam Map<String, String> filtersMap){
        Set<String> channelList = settingService.retrieveChannel(filtersMap);
        log.info(loggerMessageComponent.printMessage("retrieves.channel.list"));
        return channelList;
    }

    @DeleteMapping("delete")
    @Operation(summary = "Endpoint per la cancellazione della configurazione di streaming",
            description = "Restituisce l'esito dell'operazione")
    public ResponseEntity<Map<String, String>> deleteAStreamingSetting(@RequestBody Integer id){
        var response = settingService.deleteAStreamingSetting(id);

        log.info(loggerMessageComponent.printMessage("setting.delete"));

        return response;
    }


    @GetMapping("retrive-directories-path")
    @Operation(summary = "Endpoint per prendere tutti i path collegati alle configurazioni dell'utente",
            description = "Restituisce la lista delle configurazioni dell'utente")
    public List<VideoPathRecord> retriveDirectoriesSettingsPath(@RequestParam Map<String, String> filtersMap){
        log.info(loggerMessageComponent.printMessage("retrieves.configurations"));
        return settingService.retrieveDirectoriesSettingsPath(filtersMap);
    }
}
