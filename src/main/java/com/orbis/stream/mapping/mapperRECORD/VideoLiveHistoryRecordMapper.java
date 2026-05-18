package com.orbis.stream.mapping.mapperRECORD;

import com.orbis.stream.mapping.BaseMapper;
import com.orbis.stream.model.VideoLiveHistory;
import com.orbis.stream.record.VideoLiveHistoryRecord;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VideoLiveHistoryRecordMapper extends BaseMapper<VideoLiveHistoryRecord, VideoLiveHistory> {
}
