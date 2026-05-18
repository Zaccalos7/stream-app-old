package com.orbis.stream.mapping.mapperDTO;

import com.orbis.stream.dto.SettingDto;
import com.orbis.stream.mapping.BaseMapper;
import com.orbis.stream.model.Setting;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface SettingMapper extends BaseMapper<SettingDto, Setting> {
}
