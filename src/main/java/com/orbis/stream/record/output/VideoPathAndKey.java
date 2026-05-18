package com.orbis.stream.record.output;

import lombok.Builder;

@Builder
public record VideoPathAndKey(String videoPath, Integer videoKey) {
}
