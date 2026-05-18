package com.orbis.stream.enums;

import lombok.Getter;

@Getter
public enum LiveStatusEnum {
    LIVE("LIVE"),
    OFFLINE("OFFLINE"),
    ENDED("ENDED"),
    ERROR("ERROR"),
    STOPPED("STOPPED");

    private final String value;

    LiveStatusEnum(String value){
        this.value = value;
    }

}
