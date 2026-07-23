package com.logicgate.editor.io;

import com.logicgate.Circuit;
import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.model.VisualWire;
import com.logicgate.editor.state.EditorContext;
import com.logicgate.gates.InputPin;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ProjectManagerTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void missingModNodeDoesNotShiftWireIndexes() throws Exception {
        Path projectRoot = temporaryFolder.newFolder("missing-mod").toPath();
        writeCircuitWithMissingFirstNode(projectRoot.resolve("circuit.lgs"));

        EditorContext context = new EditorContext(new Circuit());
        context.projectRoot = projectRoot.toFile();
        context.projectConfig = new ProjectConfig("missing-mod");
        ProjectManager manager = new ProjectManager(context);

        manager.loadCircuitOnly();

        assertEquals(2, context.visualNodes.size());
        assertEquals(1, context.visualWires.size());
        VisualWire wire = context.visualWires.get(0);
        assertSame(context.visualNodes.get(0), wire.from);
        assertSame(context.visualNodes.get(1), wire.to);
        assertEquals(1, manager.consumeLoadWarnings().size());
    }

    @Test
    public void corruptCircuitFileLeavesCurrentCanvasUntouched() throws Exception {
        Path projectRoot = temporaryFolder.newFolder("corrupt-circuit").toPath();
        Files.write(projectRoot.resolve("circuit.lgs"), new byte[] { 0x01, 0x02 });

        EditorContext context = new EditorContext(new Circuit());
        context.projectRoot = projectRoot.toFile();
        InputPin input = new InputPin();
        context.getCircuit().addNode(input);
        VisualNode original = new VisualNode(input, 10, 20, "Original");
        context.visualNodes.add(original);
        context.setDirty(true);

        ProjectManager manager = new ProjectManager(context);
        manager.loadCircuitOnly();

        assertEquals(1, context.visualNodes.size());
        assertSame(original, context.visualNodes.get(0));
        assertTrue(context.isDirty());
        assertEquals(1, manager.consumeLoadWarnings().size());
    }

    @Test
    public void failedSaveDoesNotOverwriteExistingCircuitFile() throws Exception {
        Path projectRoot = temporaryFolder.newFolder("failed-save").toPath();
        Path circuitFile = projectRoot.resolve("circuit.lgs");
        byte[] originalContent = new byte[] { 9, 8, 7 };
        Files.write(circuitFile, originalContent);

        EditorContext context = new EditorContext(new Circuit());
        context.projectRoot = projectRoot.toFile();
        context.projectConfig = new ProjectConfig("failed-save");
        InputPin input = new InputPin();
        input.setTypeId("x".repeat(70_000));
        context.getCircuit().addNode(input);
        context.visualNodes.add(new VisualNode(input, 0, 0, ""));

        ProjectManager manager = new ProjectManager(context);

        assertFalse(manager.saveCurrentProject());
        assertArrayEquals(originalContent, Files.readAllBytes(circuitFile));
    }

    private static void writeCircuitWithMissingFirstNode(Path file) throws Exception {
        try (DataOutputStream output = new DataOutputStream(Files.newOutputStream(file))) {
            output.writeInt(0x4C475321);
            output.writeInt(8);
            output.writeInt(3);
            writeNode(output, "missing.mod.Node");
            writeNode(output, "InputPin");
            writeNode(output, "OutputPin");

            output.writeInt(1);
            output.writeInt(1);
            output.writeInt(0);
            output.writeInt(2);
            output.writeInt(0);
            output.writeBoolean(false);
            output.writeUTF("");
            output.writeInt(0);
        }
    }

    private static void writeNode(DataOutputStream output, String type) throws Exception {
        output.writeUTF(type);
        output.writeDouble(0);
        output.writeDouble(0);
        output.writeDouble(0);
        output.writeUTF("");
        output.writeBoolean(false);
        output.writeUTF("");
        output.writeBoolean(false);
        output.writeInt(0);
    }
}
