package net.corda.djvm.tools.cli

import picocli.CommandLine.*
import java.nio.file.Files
import java.nio.file.Path

@Command(
    name = "new",
    description = ["Create one or more new Java classes implementing the sandbox runnable interface that is " +
        "required for execution in the deterministic sandbox. Each Java file is created using a template, " +
        "with class name derived from the provided file name."
    ],
    showDefaultValues = true
)
@Suppress("KDocMissingDocumentation")
class NewCommand : CommandBase() {

    @Parameters(description = ["The names of the Java source files that will be created."])
    var files: Array<Path> = emptyArray()

    @Option(names = ["-f", "--force"], description = ["Forcefully overwrite files if they already exist."])
    var force: Boolean = false

    @Option(names = ["--from"], description = ["The input type to use for the constructed runnable."])
    var fromType: String = "Object"

    @Option(names = ["--to"], description = ["The output type to use for the constructed runnable."])
    var toType: String = "Object"

    @Option(names = ["--return"], description = ["The default return value for the constructed runnable."])
    var returnValue: String = "null"

    override fun validateArguments() = files.isNotEmpty()

    override fun handleCommand(): Boolean {
        val codePath = createCodePath()
        val files = files.getFiles { codePath.resolve(it) }
        for (file in files) {
            try {
                printVerbose("Creating file '$file'...")
                Files.newBufferedWriter(file, *openOptions(force)).use {
                    it.append(TEMPLATE
                        .replace("[NAME]", file.baseName)
                        .replace("[FROM]", fromType)
                        .replace("[TO]", toType)
                        .replace("[RETURN]", returnValue))
                }
            } catch (exception: Throwable) {
                throw Exception("Failed to create file '$file'", exception)
            }
        }
        return true
    }

    companion object {

        private val TEMPLATE = """
            |package net.corda.sandbox;
            |
            |import java.util.function.Function;
            |
            |public class [NAME] implements Function<[FROM], [TO]> {
            |    @Override
            |    public [TO] apply([FROM] input) {
            |        return [RETURN];
            |    }
            |}
            """.trimMargin()

    }

}
