package com.orbis.stream.mapping.mapperDTO;

import com.orbis.stream.dto.CredentialDto;
import com.orbis.stream.mapping.BaseMapper;
import com.orbis.stream.model.Credential;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface CredentialMapper extends BaseMapper<CredentialDto, Credential> {

}
