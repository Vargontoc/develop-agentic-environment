package es.vargontoc.agentic.viewer.cli.model;

import java.util.List;

public record DescribeData(
    String provider,
    String scope,
    String kind,
    String name,
    String path,
    String format,
    String module,
    String header,
    String body,
    String sharedBody,
    String hash,
    String status,
    boolean modifiedExternally,
    List<String> files
) {
}
