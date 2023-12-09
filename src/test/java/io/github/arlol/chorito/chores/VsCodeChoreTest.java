package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class VsCodeChoreTest {

    @RegisterExtension
    final FileSystemExtension extension = new FileSystemExtension();

    private void doit() {
        new VsCodeChore().doit(extension.choreContext());
    }

    @Test
    public void testWithNothing() {
        Path pom = extension.root().resolve("pom.xml");
        FilesSilent.touch(pom);
        Path extensions = extension.root().resolve(".vscode/extensions.json");
        doit();
        assertThat(FilesSilent.exists(extensions)).isTrue();
    }

}
