package net.sansa_stack.spark.cli.cmd;

import picocli.CommandLine.Command;

@Command(name = "analyze",
    description = "Analyze parallel parallel parsing of files",
    subcommands = { CmdSansaAnalyzeRdf.class, CmdSansaAnalyzeCsv.class }
)
public class CmdSansaAnalyzeParent {

}