package com.orbis.stream.mapping.mapperRECORD;

import com.orbis.stream.mapping.BaseMapper;
import com.orbis.stream.model.Video;
import com.orbis.stream.record.VideoRecord;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {VideoLiveHistoryRecordMapper.class, VideoSettingRecordMapper.class})
public interface VideoRecordMapper extends BaseMapper<VideoRecord, Video> {
}
