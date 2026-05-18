package com.orbis.stream.controller;


import com.orbis.stream.component.LoggerMessageComponent;
import com.orbis.stream.dto.VideoDto;
import com.orbis.stream.handler.ResponseHandler;
import com.orbis.stream.record.StartLiveRecord;
import com.orbis.stream.record.VideoRecord;
import com.orbis.stream.service.StreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/live/")
@Tag(name="Setting handle live start stop")
@RequiredArgsConstructor
public class StreamController {

    private final LoggerMessageComponent loggerMessageComponent;
    private final ResponseHandler responseHandler;

    private final StreamService streamService;


    @PostMapping("/start-live")
    @Operation(summary = "Endpoint per lo start della live",
            description = "Restituisce l'esito dell'operazione")
    public ResponseEntity<Map<String,String>> startLive(@RequestBody StartLiveRecord startLiveRecord) {

        var response = streamService.startLive(startLiveRecord);
        log.info(loggerMessageComponent.printMessage("live.started"));
        return response;
    }

    @PutMapping("/stop-live")
    @Operation(summary = "Endpoint per lo stop della live con il suo pkid",
            description = "Restituisce l'esito dell'operazione e il pkid del videoLiveHistory fermato")
    public ResponseEntity<Map<String,String>> stopLive(@RequestParam Integer videoLivePkid) {

       streamService.stopVideoStreamingByPkid(videoLivePkid);
       log.info(loggerMessageComponent.printMessage("live.stopped"));
       return responseHandler.buildResponse("live.stopped", HttpStatus.OK);
    }

    @PostMapping("/start-video-live")
    @Operation(summary = "Endpoint per lo stop delle live del canale attive",
            description = "Restituisce l'esito dell'operazione")
    public ResponseEntity<Map<String,String>> startVideoLive(@NotNull(message = "input.not.valid") @RequestBody VideoRecord videoRecord) {

        var response = streamService.startVideo(videoRecord);
        log.info(loggerMessageComponent.printMessage("live.started"));
        return response;
    }

}
