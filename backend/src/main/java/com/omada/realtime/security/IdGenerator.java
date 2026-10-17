package com.omada.realtime.security;
import java.util.UUID;
public final class IdGenerator {
    private IdGenerator(){}
    public static UUID uuid(){ return UUID.randomUUID(); }
    public static String uuidString(){ return UUID.randomUUID().toString(); }
}
