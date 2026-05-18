package com.orbis.stream.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SystemInfoDto {

    String field;
    Integer value;
}
