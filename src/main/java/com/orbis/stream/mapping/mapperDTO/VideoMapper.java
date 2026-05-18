package com.orbis.stream.mapping.mapperDTO;

import com.orbis.stream.dto.VideoDto;
import com.orbis.stream.mapping.BaseMapper;
import com.orbis.stream.mapping.mapperRECORD.VideoRecordMapper;
import com.orbis.stream.model.Video;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {VideoLiveHistoryMapper.class, VideoRecordMapper.class})
public interface VideoMapper extends BaseMapper<VideoDto, Video> {
}
