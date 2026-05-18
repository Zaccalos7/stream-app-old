package com.orbis.stream.mapping.mapperRECORD;

import com.orbis.stream.mapping.BaseMapper;
import com.orbis.stream.model.VideoSetting;
import com.orbis.stream.record.VideoSettingsRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {AudioSettingRecordMapper.class, VideoSettingOptionRecordMapper.class})
public interface VideoSettingRecordMapper extends BaseMapper<VideoSettingsRecord, VideoSetting> {
    @Mapping(source = "audioSetting", target = "audioSettingRecord")
    @Mapping(source = "videoSettingsOptions", target = "videoOptions")
    VideoSettingsRecord toDto(VideoSetting model);

    @Mapping(source = "audioSettingRecord", target = "audioSetting")
    @Mapping(source = "videoOptions", target = "videoSettingsOptions")
    VideoSetting toModel(VideoSettingsRecord dto);
}
