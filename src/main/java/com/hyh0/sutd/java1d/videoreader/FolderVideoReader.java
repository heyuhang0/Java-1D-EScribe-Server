package com.hyh0.sutd.java1d.videoreader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FolderVideoReader extends MultiVideoReader {

    private Iterator<String> pathsIterator;

    public FolderVideoReader(String dirName, String prefix, String suffix) throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(dirName), 2)) {
            List<String> filteredPaths = paths
                    .filter(p -> p.getFileName().toString().startsWith(prefix))
                    .map(Path::toString)
                    .filter(f -> f.endsWith(suffix))
                    .sorted(String::compareTo)
                    .collect(Collectors.toList());
            this.pathsIterator = filteredPaths.iterator();
        }
    }

    @Override
    protected boolean hasNext() {
        return this.pathsIterator.hasNext();
    }

    @Override
    protected String getNextVideo() {
        return this.pathsIterator.next();
    }
}
