package com.orbis.stream.mapping.mapperRECORD;

import com.orbis.stream.mapping.BaseMapper;
import com.orbis.stream.mapping.mapperDTO.UserMapper;
import com.orbis.stream.model.Setting;
import com.orbis.stream.record.SettingRecord;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface SettingRecordMapper extends BaseMapper<SettingRecord, Setting> {

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateSettingFromSettingRecord(SettingRecord record, @MappingTarget Setting setting);

}
