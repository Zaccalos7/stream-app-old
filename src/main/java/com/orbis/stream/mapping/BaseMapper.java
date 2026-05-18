package com.orbis.stream.mapping;

public interface BaseMapper<D, M>{
    D toDto (M model);
    M toModel(D dto);
}
