package net.sansa_stack.hadoop.format.jena.trig;

import io.reactivex.rxjava3.core.Flowable;
import net.sansa_stack.hadoop.core.Accumulating;
import net.sansa_stack.hadoop.format.jena.base.RecordReaderGenericRdfAccumulatingBase;
import org.aksw.jenax.arq.dataset.api.DatasetOneNg;
import org.aksw.jenax.arq.dataset.impl.DatasetOneNgImpl;
import org.aksw.jenax.sparql.query.rx.RDFDataMgrRx;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.Lang;
import org.apache.jena.sparql.core.Quad;

import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

/**
 * RecordReader for the Trig RDF format that groups consecutive quads having the
 * same IRI for the graph component into Datasets.
 */
public class RecordReaderRdfTrigDataset
    extends RecordReaderGenericRdfAccumulatingBase<Quad, Node, DatasetOneNg, DatasetOneNg>
{
    public static final String RECORD_MINLENGTH_KEY = "mapreduce.input.trig.dataset.record.minlength";
    public static final String RECORD_MAXLENGTH_KEY = "mapreduce.input.trig.dataset.record.maxlength";
    public static final String RECORD_PROBECOUNT_KEY = "mapreduce.input.trig.dataset.record.probecount";
    public static final String PREFIXES_MAXLENGTH_KEY = "mapreduce.input.trig.dataset.prefixes.maxlength";


    protected static final Pattern trigFwdPattern = Pattern.compile("@?base|@?prefix|(graph\\s*)?(<[^>]*>|_?:[^-\\s]+)\\s*\\{", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    public static class AccumulatingDataset
        implements Accumulating<Quad, Node, DatasetOneNg, DatasetOneNg> {

        @Override
        public Node classify(Quad item) {
            return item.getGraph();
        }

        @Override
        public DatasetOneNg createAccumulator(Node groupKey) {
            // Is null handling needed here?
            String graphName = groupKey == null
                    ? Quad.defaultGraphNodeGenerated.getURI()
                    : groupKey.getURI();

            // TODO Allow for a custom graph factory here
            // Graph graph = GraphFactory.createDefaultGraph();
            return DatasetOneNgImpl.create(graphName);
            // return DatasetFactoryEx.createInsertOrderPreservingDataset();
        }

        @Override
        public void accumulate(DatasetOneNg accumulator, Quad item) {
            accumulator.asDatasetGraph().add(item);
        }

        @Override
        public DatasetOneNg accumulatedValue(DatasetOneNg accumulator) {
            return accumulator;
        }
    }

    public RecordReaderRdfTrigDataset() {
        super(
                RECORD_MINLENGTH_KEY,
                RECORD_MAXLENGTH_KEY,
                RECORD_PROBECOUNT_KEY,
                PREFIXES_MAXLENGTH_KEY,
                trigFwdPattern,
                Lang.TRIG,
                new AccumulatingDataset());
    }

    @Override
    protected Flowable<Quad> parse(Callable<InputStream> inputStreamSupplier) {
        Flowable<Quad> result = RDFDataMgrRx.createFlowableQuads(inputStreamSupplier, lang, baseIri);
        // Flowable<Dataset> result = RDFDataMgrRx.createFlowableDatasets(inputStreamSupplier, lang, baseIri);
        return result;
    }


}