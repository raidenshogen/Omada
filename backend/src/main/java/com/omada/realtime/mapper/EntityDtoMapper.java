package com.omada.realtime.mapper;
import java.util.UUID;
public final class EntityDtoMapper {
    private EntityDtoMapper(){}
    public static String toString(UUID id){ return id == null ? null : id.toString(); }
    public static UUID toUuid(String id){ try { return id == null ? null : UUID.fromString(id); } catch(Exception e){ return null; } }
}
