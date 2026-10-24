package fr.zbar.starter.logs;

import java.net.URI;

/** Host, port and path of an outgoing call: never the user info, query or fragment. */
final class OutgoingTargets {

  private OutgoingTargets() {}

  static String hostAndPath(URI uri) {
    String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
    return uri.getHost() + port + uri.getRawPath();
  }
}
