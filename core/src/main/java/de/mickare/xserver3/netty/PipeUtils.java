package de.mickare.xserver3.netty;

import java.util.logging.Logger;

import io.netty.util.AttributeKey;
import io.netty.util.AttributeMap;

public final class PipeUtils {

  public static final AttributeKey<Logger> LOGGER = AttributeKey.newInstance("LOGGER");
  public static final AttributeKey<NettyConnection> CONNECTION = AttributeKey.newInstance("CONNECTION");

  public static Logger getLogger(final AttributeMap map) {
    return map.attr(LOGGER).get();
  }

}
