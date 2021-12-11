package io.github.bananalang.jbc;

import java.io.File;
import java.io.IOException;

import io.github.bananalang.JavaBananaConstants;
import io.github.bananalang.bytecode.ByteCodeFile;
import io.github.bananalang.compile.BananaCompiler;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

public class JBC {
    public static void main(String[] args) throws IOException {
        ArgumentParser parser = ArgumentParsers.newFor("jbc").build()
            .defaultHelp(true)
            .description("JavaBanana compiler");
        parser.addArgument("file").nargs("+")
              .help("The file(s) to compile")
              .type(File.class);
        Namespace ns = parser.parseArgsOrFail(args);
        for (File file : ns.<File>getList("file")) {
            String moduleName = file.getName();
            moduleName = moduleName.substring(0, moduleName.lastIndexOf('.'));
            if (JavaBananaConstants.DEBUG) {
                System.err.println("Compiling " + moduleName);
            }
            ByteCodeFile bbc = BananaCompiler.compileFile(file);
            if (JavaBananaConstants.DEBUG) {
                System.err.println("Finished compiling " + moduleName);
                bbc.disassemble(System.err);
            }
            bbc.write(new File(file.getParentFile(), moduleName + ".bbc"));
        }
    }
}
