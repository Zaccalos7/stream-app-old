package com.orbis.stream.utilities;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.io.Resource;

@Getter
@Setter
@Builder
public class ImageUtilities {
    private String contentType;
    private Resource resource;
}
