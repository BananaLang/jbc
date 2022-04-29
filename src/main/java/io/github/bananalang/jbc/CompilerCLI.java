package io.github.bananalang.jbc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.CheckClassAdapter;

import io.github.bananalang.JavaBananaConstants;
import io.github.bananalang.compile.BananaCompiler;
import io.github.bananalang.compile.CompileOptions;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;

public class CompilerCLI {
    @FunctionalInterface
    private static interface ActionHandler {
        public abstract void execute(Namespace ns) throws Throwable;
    }

    private static enum Action {
        COMPILE(ns -> {
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
        }),
        RUN(ns -> {
            CompileOptions options = new CompileOptions();
            File file = ns.<File>get("file");
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
                System.out.println();
            }
            try {
                new ClassLoader() {
                    public Class<?> loadFromBytecode(String name, byte[] bytecode) {
                        return defineClass(name, bytecode, 0, bytecode.length);
                    }
                }.loadFromBytecode(options.className(), bytecode)
                    .getDeclaredMethod("main", String[].class)
                    .invoke(null, new Object[] {ns.getList("extraArgs").toArray(new String[0])});
            } catch (NoSuchMethodException e) {
                System.err.println("ERROR: No top-level code in the specified file");
                System.exit(1);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            } catch (Exception e) {
                throw new Error(e);
            }
        });

        private final ActionHandler handler;

        private Action(ActionHandler handler) {
            this.handler = handler;
        }
    }

    public static void main(String[] args) throws Throwable {
        ArgumentParser parser = ArgumentParsers.newFor("jbc").build()
            .defaultHelp(true)
            .description("JavaBanana compiler");
        Subparsers subparsers = parser.addSubparsers();

        {
            Subparser compileParser = subparsers.addParser("compile")
                .setDefault("action", Action.COMPILE)
                .description("Compile files");
            compileParser.addArgument("file").nargs("+")
                .help("The file(s) to compile")
                .type(File.class);
        }

        {
            Subparser runParser = subparsers.addParser("run")
                .setDefault("action", Action.RUN)
                .description("Run files");
            runParser.addArgument("file")
                .help("The file to run")
                .type(File.class);
            runParser.addArgument("extraArgs").nargs("*")
                .help("Extra args to pass to the script");
        }

        Namespace ns = parser.parseArgsOrFail(args);
        ns.<Action>get("action").handler.execute(ns);
    }
}
