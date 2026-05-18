package com.orbis.stream.mapping.mapperRECORD;

import com.orbis.stream.mapping.BaseMapper;
import com.orbis.stream.model.VideoSettingsOption;
import com.orbis.stream.record.VideoOptionRecord;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VideoSettingOptionRecordMapper extends BaseMapper<VideoOptionRecord,VideoSettingsOption> {
}
