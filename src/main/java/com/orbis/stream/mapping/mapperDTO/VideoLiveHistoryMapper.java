package com.orbis.stream.mapping.mapperDTO;

import com.orbis.stream.dto.VideoLiveHistoryDto;
import com.orbis.stream.mapping.BaseMapper;
import com.orbis.stream.model.VideoLiveHistory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VideoLiveHistoryMapper extends BaseMapper<VideoLiveHistoryDto, VideoLiveHistory> {
}
