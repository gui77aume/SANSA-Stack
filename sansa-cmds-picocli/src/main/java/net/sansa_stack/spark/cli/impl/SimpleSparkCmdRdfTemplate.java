package net.sansa_stack.spark.cli.impl;

import java.util.List;

import net.sansa_stack.spark.cli.cmd.CmdMixinSparkInput;
import net.sansa_stack.spark.io.rdf.input.api.RdfSourceCollection;
import net.sansa_stack.spark.io.rdf.input.api.RdfSourceFactory;
import net.sansa_stack.spark.io.rdf.input.impl.RdfSourceFactoryImpl;

public abstract class SimpleSparkCmdRdfTemplate<T>
    extends SimpleSparkCmdTemplate<T>
{
    protected CmdMixinSparkInput inputSpec; // Not optimal; each file should have its own set of input options
    protected RdfSourceCollection rdfSources;

    public SimpleSparkCmdRdfTemplate(
            String appName,
            CmdMixinSparkInput inputSpec,
            List<String> inputFiles) {
        super(appName, inputFiles);
        this.inputSpec = inputSpec;
    }

    @Override
    protected void processInputs() {
        RdfSourceFactory rdfSourceFactory = RdfSourceFactoryImpl.from(sparkSession);

        rdfSources = CmdUtils.createRdfSourceCollection(rdfSourceFactory, inputFiles, inputSpec);
    }
}