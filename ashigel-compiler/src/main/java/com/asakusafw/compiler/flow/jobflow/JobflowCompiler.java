/**
 * Copyright 2011 Asakusa Framework Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.asakusafw.compiler.flow.jobflow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.compiler.common.Precondition;
import com.asakusafw.compiler.flow.ExternalIoCommandProvider;
import com.asakusafw.compiler.flow.ExternalIoDescriptionProcessor;
import com.asakusafw.compiler.flow.ExternalIoDescriptionProcessor.Input;
import com.asakusafw.compiler.flow.ExternalIoDescriptionProcessor.IoContext;
import com.asakusafw.compiler.flow.ExternalIoDescriptionProcessor.Output;
import com.asakusafw.compiler.flow.ExternalIoDescriptionProcessor.OutputSource;
import com.asakusafw.compiler.flow.FlowCompilingEnvironment;
import com.asakusafw.compiler.flow.jobflow.JobflowModel.Delivery;
import com.asakusafw.compiler.flow.jobflow.JobflowModel.Export;
import com.asakusafw.compiler.flow.jobflow.JobflowModel.Import;
import com.asakusafw.compiler.flow.jobflow.JobflowModel.Process;
import com.asakusafw.compiler.flow.jobflow.JobflowModel.Processible;
import com.asakusafw.compiler.flow.jobflow.JobflowModel.Reduce;
import com.asakusafw.compiler.flow.jobflow.JobflowModel.SideData;
import com.asakusafw.compiler.flow.jobflow.JobflowModel.Source;
import com.asakusafw.compiler.flow.jobflow.JobflowModel.Stage;
import com.asakusafw.compiler.flow.plan.StageGraph;
import com.asakusafw.compiler.flow.stage.StageModel;
import com.ashigeru.util.graph.Graph;
import com.ashigeru.util.graph.Graphs;

/**
 * ジョブフロー内で利用されるプログラムをコンパイルする。
 */
public class JobflowCompiler {

    static final Logger LOG = LoggerFactory.getLogger(JobflowCompiler.class);

    @SuppressWarnings("unused")
    private FlowCompilingEnvironment environment;

    private JobflowAnalyzer analyzer;

    private StageClientEmitter stageClientEmitter;

    /**
     * インスタンスを生成する。
     * @param environment 環境オブジェクト
     * @throws IllegalArgumentException 引数に{@code null}が指定された場合
     */
    public JobflowCompiler(FlowCompilingEnvironment environment) {
        Precondition.checkMustNotBeNull(environment, "environment"); //$NON-NLS-1$
        this.environment = environment;
        this.analyzer = new JobflowAnalyzer(environment);
        this.stageClientEmitter = new StageClientEmitter(environment);
    }

    /**
     * 指定のステージグラフをコンパイルし、ジョブフロー全体のステージ構造に関する情報を返す。
     * @param graph ステージグラフ
     * @param stageModels 各ステージの情報
     * @return ジョブフロー全体のステージ構造に関する情報、解析に失敗した場合は{@code null}
     * @throws IOException コンパイル結果の出力に失敗した場合
     * @throws IllegalArgumentException 引数に{@code null}が指定された場合
     */
    public JobflowModel compile(
            StageGraph graph,
            Collection<StageModel> stageModels) throws IOException {
        Precondition.checkMustNotBeNull(graph, "graph"); //$NON-NLS-1$
        Precondition.checkMustNotBeNull(stageModels, "stageModels"); //$NON-NLS-1$
        LOG.debug("フロー{}を解析しています",
                graph.getInput().getSource().getDescription().getName());
        JobflowModel jobflow = analyze(graph, stageModels);
        compileClients(jobflow);
        CompiledJobflow compiled = emit(jobflow);
        jobflow.setCompiled(compiled);
        reportSummary(jobflow);
        return jobflow;
    }

    private JobflowModel analyze(
            StageGraph graph,
            Collection<StageModel> stageModels) throws IOException {
        assert graph != null;
        assert stageModels != null;
        JobflowModel jobflow = analyzer.analyze(graph, stageModels);
        if (analyzer.hasError()) {
            analyzer.clearError();
            throw new IOException("ジョブフローのコンパイルは中断されました");
        }
        return jobflow;
    }

    private CompiledJobflow emit(JobflowModel model) throws IOException {
        Precondition.checkMustNotBeNull(model, "model"); //$NON-NLS-1$
        LOG.debug("{}に対する外部入出力の記述を生成します",
                model.getStageGraph().getInput().getSource().getDescription().getName());
        Map<ExternalIoDescriptionProcessor, List<Import>> imports = group(model.getImports());
        Map<ExternalIoDescriptionProcessor, List<Export>> exports = group(model.getExports());
        fillEmptyList(imports, exports.keySet());
        fillEmptyList(exports, imports.keySet());

        List<ExternalIoCommandProvider> commands = new ArrayList<ExternalIoCommandProvider>();
        List<CompiledStage> prologues = new ArrayList<CompiledStage>();
        List<CompiledStage> epilogues = new ArrayList<CompiledStage>();
        for (Map.Entry<ExternalIoDescriptionProcessor, List<Import>> entry : imports.entrySet()) {
            ExternalIoDescriptionProcessor proc = entry.getKey();
            List<Import> importGroup = entry.getValue();
            List<Export> exportGroup = exports.get(proc);
            assert exportGroup != null;
            assert importGroup.isEmpty() == false || exportGroup.isEmpty() == false;

            IoContext context = createEmitContext(proc, importGroup, exportGroup);

            LOG.debug("{}によって外部入出力の記述を生成しています", proc.getClass().getName());
            proc.emitPackage(context);

            LOG.debug("{}によってインポーターの記述を生成しています", proc.getClass().getName());
            prologues.addAll(proc.emitPrologue(context));

            LOG.debug("{}によってエクスポーターの記述を生成しています", proc.getClass().getName());
            epilogues.addAll(proc.emitEpilogue(context));

            commands.add(proc.createCommandProvider(context));
        }

        return new CompiledJobflow(commands, prologues, epilogues);
    }

    private IoContext createEmitContext(
            ExternalIoDescriptionProcessor processor,
            List<Import> importGroup,
            List<Export> exportGroup) {
        assert processor != null;
        assert importGroup != null;
        assert exportGroup != null;
        List<Input> inputs = new ArrayList<Input>();
        for (Import model : importGroup) {
            inputs.add(new Input(model.getDescription(), model.getOutputFormatType()));
        }
        List<Output> outputs = new ArrayList<Output>();
        for (Export model : exportGroup) {
            List<OutputSource> sources = new ArrayList<OutputSource>();
            for (Source source : model.getResolvedSources()) {
                sources.add(new OutputSource(
                        source.getLocations(),
                        source.getInputFormatType()));
            }
            outputs.add(new Output(model.getDescription(), sources));
        }
        IoContext context = new IoContext(inputs, outputs);
        return context;
    }

    private void reportSummary(JobflowModel jobflow) {
        LOG.info("Compilation Report: {} - {}", jobflow.getBatchId(), jobflow.getFlowId());
        LOG.info("Imports: {}", jobflow.getImports().size());
        LOG.info("Exports: {}", jobflow.getExports().size());
        LOG.info("Stages : {}", jobflow.getStages().size());
        if (LOG.isDebugEnabled()) {
            LOG.debug("Details:");
            for (Import stage : jobflow.getImports()) {
                LOG.debug("====");
                LOG.debug("Import: {}", stage.getId());
                LOG.debug("Description: {}", stage.getDescription().getImporterDescription().getClass().getName());
                LOG.debug("Target: {}", stage.getLocations());
                LOG.debug("Format: {}", stage.getInputFormatType().getName());
            }
            for (CompiledStage stage : jobflow.getCompiled().getPrologueStages()) {
                LOG.debug("====");
                LOG.debug("Prologue: {}", stage.getStageId());
                LOG.debug("Client: {}", stage.getQualifiedName().toNameString());
            }
            Graph<Stage> graph = jobflow.getDependencyGraph();
            Graph<Stage> tgraph = Graphs.transpose(graph);
            for (Stage stage : jobflow.getStages()) {
                LOG.debug("====");
                LOG.debug("Stage: {}", stage.getCompiled().getStageId());
                LOG.debug("Client: {}", stage.getCompiled().getQualifiedName().toNameString());
                for (Process unit : stage.getProcesses()) {
                    LOG.debug("Input: {} ({})", unit.getResolvedLocations(), unit.getDataType());
                }
                for (Delivery unit : stage.getDeliveries()) {
                    LOG.debug("Output: {} ({})", unit.getLocations(), unit.getDataType());
                }
                Reduce reducer = stage.getReduceOrNull();
                if (reducer != null) {
                    LOG.debug("ShuffleKey: {}", reducer.getKeyTypeName().toNameString());
                    LOG.debug("ShuffleValue: {}", reducer.getValueTypeName().toNameString());
                    LOG.debug("Partitioner: {}", reducer.getPartitionerTypeName().toNameString());
                    LOG.debug("Grouping: {}", reducer.getGroupingComparatorTypeName().toNameString());
                    LOG.debug("Sort: {}", reducer.getSortComparatorTypeName().toNameString());
                    LOG.debug("Combiner: {}", reducer.getCombinerTypeNameOrNull() == null
                            ? "N/A" : reducer.getCombinerTypeNameOrNull().toNameString());
                    LOG.debug("Reducer: {}", reducer.getReducerTypeName().toNameString());
                }
                for (SideData data : stage.getSideData()) {
                    LOG.debug("SideData: {} ({})", data.getLocalName(), data.getClusterPath());
                }
                LOG.debug("Upstreams: {}", getStageIds(graph.getConnected(stage)));
                LOG.debug("Downstreams: {}", getStageIds(tgraph.getConnected(stage)));
            }
            for (CompiledStage stage : jobflow.getCompiled().getEpilogueStages()) {
                LOG.debug("====");
                LOG.debug("Epilogue: {}", stage.getStageId());
                LOG.debug("Client: {}", stage.getQualifiedName().toNameString());
            }
            for (Export stage : jobflow.getExports()) {
                LOG.debug("====");
                LOG.debug("Export: {}", stage.getId());
                LOG.debug("Description: {}", stage.getDescription().getExporterDescription().getClass().getName());
                LOG.debug("Source: {}", stage.getResolvedLocations());
            }
            LOG.debug("====");
        }
    }

    private List<String> getStageIds(Collection<Stage> stages) {
        assert stages != null;
        List<String> results = new ArrayList<String>();
        for (Stage stage : stages) {
            results.add(stage.getCompiled().getStageId());
        }
        Collections.sort(results);
        return results;
    }

    private <K, V> void fillEmptyList(Map<K, List<V>> map, Set<K> samples) {
        assert map != null;
        assert samples != null;
        for (K sample : samples) {
            if (map.containsKey(sample) == false) {
                map.put(sample, Collections.<V>emptyList());
            }
        }
    }

    private <T extends Processible> Map<ExternalIoDescriptionProcessor, List<T>> group(List<T> targets) {
        assert targets != null;
        Map<ExternalIoDescriptionProcessor, List<T>> results =
            new HashMap<ExternalIoDescriptionProcessor, List<T>>();
        for (T processible : targets) {
            ExternalIoDescriptionProcessor proc = processible.getProcessor();
            List<T> members = results.get(proc);
            if (members == null) {
                members = new ArrayList<T>();
                results.put(proc, members);
            }
            members.add(processible);
        }
        return results;
    }

    private void compileClients(JobflowModel jobflow) throws IOException {
        for (Stage stage : jobflow.getStages()) {
            CompiledStage client = stageClientEmitter.emit(stage);
            stage.setCompiled(client);
        }
    }
}
