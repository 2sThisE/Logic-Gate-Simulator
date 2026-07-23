package com.logicgate.editor.mod;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ModLoaderTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void missingModFileProducesFileDiagnostic() throws Exception {
        ModLoader loader = new ModLoader(temporaryFolder.newFolder("missing"));

        assertTrue(loader.loadSingleMod("missing.jar").isEmpty());

        List<ModLoadDiagnostic> diagnostics = loader.consumeDiagnostics();
        assertEquals(1, diagnostics.size());
        assertEquals(ModLoadDiagnostic.Severity.ERROR, diagnostics.get(0).severity);
        assertEquals(ModLoadDiagnostic.Stage.FILE_ACCESS, diagnostics.get(0).stage);
        assertEquals("missing.jar", diagnostics.get(0).jarName);
        assertTrue(loader.consumeDiagnostics().isEmpty());
    }

    @Test
    public void invalidClassProducesClassNameAndCauseDiagnostic() throws Exception {
        Path projectRoot = temporaryFolder.newFolder("invalid-class").toPath();
        Path modsDir = Files.createDirectories(projectRoot.resolve("mods"));
        Path jarFile = modsDir.resolve("broken.jar");
        writeInvalidClassJar(jarFile);
        ModLoader loader = new ModLoader(projectRoot.toFile());

        assertTrue(loader.loadSingleMod("broken.jar").isEmpty());

        List<ModLoadDiagnostic> diagnostics = loader.consumeDiagnostics();
        assertEquals(1, diagnostics.size());
        ModLoadDiagnostic diagnostic = diagnostics.get(0);
        assertEquals(ModLoadDiagnostic.Stage.CLASS_LOADING, diagnostic.stage);
        assertEquals("broken.Invalid", diagnostic.className);
        assertTrue(diagnostic.detail.contains("ClassFormatError"));
        assertTrue(diagnostic.toDisplayString().contains("broken.jar"));
    }

    private static void writeInvalidClassJar(Path file) throws Exception {
        try (OutputStream stream = Files.newOutputStream(file);
             JarOutputStream jar = new JarOutputStream(stream)) {
            jar.putNextEntry(new JarEntry("broken/Invalid.class"));
            jar.write(new byte[] { 0x01, 0x02, 0x03 });
            jar.closeEntry();
        }
    }
}
