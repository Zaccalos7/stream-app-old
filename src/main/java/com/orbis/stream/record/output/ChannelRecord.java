package com.orbis.stream.record.output;

import lombok.Builder;

import java.util.List;

@Builder
public record ChannelRecord(String channelName, List<VideoPathAndKey> videoPathAndKeyList) {
}
