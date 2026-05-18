package com.orbis.stream.mapping.mapperDTO;

import com.orbis.stream.dto.VideoSettingsOptionDto;
import com.orbis.stream.mapping.BaseMapper;
import com.orbis.stream.model.VideoSettingsOption;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VideoSettingOptionMapper extends BaseMapper<VideoSettingsOptionDto, VideoSettingsOption> {
}
