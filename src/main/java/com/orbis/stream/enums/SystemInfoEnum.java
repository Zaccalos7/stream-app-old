package com.orbis.stream.enums;

import lombok.Getter;

@Getter
public enum SystemInfoEnum {
    CPU("CPU"),
    RAM("RAM"),
    SWAP("SWAP"),
    CPU_TEMPERATURE("TEMPERATURA CPU");

    private final String infoName;

    SystemInfoEnum(String infoName) {
        this.infoName = infoName;
    }
}
