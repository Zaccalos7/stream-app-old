package com.orbis.stream.mapping.mapperGeneric;

import com.orbis.stream.mapping.BaseMapper;
import com.orbis.stream.record.StartLiveRecord;
import com.orbis.stream.record.VideoRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FromVideoRecordToStartLiveRecordMapper extends BaseMapper<VideoRecord, StartLiveRecord> {

    @Mapping(target = "streamUrl", source = "videoLiveHistory.streamUrl")
    @Mapping(target = "streamKey", source = "videoLiveHistory.streamKey")
    @Mapping(target = "platformStreamName", source = "videoLiveHistory.platformStreamName")
    @Mapping(target = "videoSettingsRecord", source = "videoSetting")
    StartLiveRecord toStartLiveRecord(VideoRecord videoRecord);
}
