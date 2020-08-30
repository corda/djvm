package net.corda.djvm;

import net.corda.djvm.rewiring.ByteCode;
import net.corda.djvm.rewiring.ByteCodeKey;

import java.util.concurrent.ConcurrentMap;

public interface ConfigurationOptions {
    void setExternalCache(ConcurrentMap<ByteCodeKey, ByteCode> externalCache);
}
