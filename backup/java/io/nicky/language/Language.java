package io.nicky.language;

import language.backend.compiler.bytecode.ir.Compressor;
import dtool.logger.ImplLogger;
import dtool.logger.Logger;
import dtool.logger.profiler.LanguageProfiler;

public class Language {

    public static final LanguageProfiler PROFILER = new LanguageProfiler();
    public static final Logger LOGGER = ImplLogger.getInstance();

    // format
    public static final String IR_FILE_SUFFIX = ".dem";
    public static final String SRC_FILE_SUFFIX = ".ag";

    // version and details

    public static final String VERSION = "0.0.3";

    // language compression
    public static final Compressor COMPRESSOR = new Compressor();

}




