package io.nicky.language.workspace.tasks;

import io.nicky.language.Language;
import language.utils.sneak.SneakyThrow;
import io.nicky.language.workspace.source.Source;
import io.nicky.language.workspace.source.SourceService;
import language.backend.compiler.ChunkBuilder;
import language.backend.compiler.bytecode.values.bytecode.ByteCode;
import language.vm.VirtualMachine;
import language.vm.VirtualMachineResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class RunExecutable implements RunTask {

    private final Path binaryDirectory;
    private final SourceService sourceService;

    public RunExecutable(Path binaryDirectory) {
        this.binaryDirectory = binaryDirectory;

        this.sourceService = new SourceService();
    }

    @Override
    public void execute(final String[] arguments) {

        Language.PROFILER.separator();

        Source source = sourceService.loadAsSource(SourceService.State.EXECUTE, binaryDirectory);
        final String file = source.getFile();
        final String directory = source.getDirectory();

        Path path = Paths.get(directory + file);

        if (!path.toString().endsWith(Language.IR_FILE_SUFFIX)) {
            System.err.println("File is not a bytecode file!");
            System.exit(-1);
        }
        if (!Files.exists(path)) {
            System.err.println("File does not exist: \"" + path + "\"!");
            System.exit(-1);
        }

        byte[] arr = Language.PROFILER.profileSegment("de-compression",
                SneakyThrow.sneaky(() -> Language.COMPRESSOR.decompress(Files.readAllBytes(path))));

        ByteCode func = Language.PROFILER.profileSegment("chunk building", () -> ChunkBuilder.build(arr));

        Language.PROFILER.profileSegment("program execution", () -> {
            final VirtualMachine virtualMachine = new VirtualMachine(func).trace(file);

            VirtualMachineResult res = virtualMachine.run();
            if (res == VirtualMachineResult.ERROR) return;
            virtualMachine.finish(arguments);
        });
    }

}
