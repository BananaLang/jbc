package io.github.bananalang.jbc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.CheckClassAdapter;

import io.github.bananalang.JavaBananaConstants;
import io.github.bananalang.compile.CompileOptions;
import io.github.bananalang.compile.BananaCompiler;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

public class CompilerCLI {
    public static void main(String[] args) throws IOException {
        ArgumentParser parser = ArgumentParsers.newFor("jbc").build()
            .defaultHelp(true)
            .description("JavaBanana compiler");
        parser.addArgument("file").nargs("+")
              .help("The file(s) to compile")
              .type(File.class);
        Namespace ns = parser.parseArgsOrFail(args);
        CompileOptions options = new CompileOptions();
        for (File file : ns.<File>getList("file")) {
            options.sourceFileName(file.getName())
                .defaultModuleName()
                .defaultClassName();
            if (JavaBananaConstants.DEBUG) {
                System.err.println("Compiling " + options.moduleName());
            }
            byte[] bytecode = BananaCompiler.compileFile(file, options).toByteArray();
            if (JavaBananaConstants.DEBUG) {
                System.err.println("Finished compiling " + options.moduleName());
                CheckClassAdapter.verify(new ClassReader(bytecode), true, new PrintWriter(System.err));
            }
            try (OutputStream out = new FileOutputStream(new File(file.getParentFile(), options.classFileName()))) {
                out.write(bytecode);
            }
        }
    }
}
