package com.orbis.stream.mapping.mapperRECORD;

import com.orbis.stream.mapping.BaseMapper;
import com.orbis.stream.model.AudioSetting;
import com.orbis.stream.record.AudioSettingsRecord;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AudioSettingRecordMapper extends BaseMapper<AudioSettingsRecord, AudioSetting> {
}
