package com.orbis.stream.mapping.mapperDTO;

import com.orbis.stream.dto.AudioSettingDto;
import com.orbis.stream.mapping.BaseMapper;
import com.orbis.stream.model.AudioSetting;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AudioSettingMapper extends BaseMapper<AudioSettingDto, AudioSetting> {
}
