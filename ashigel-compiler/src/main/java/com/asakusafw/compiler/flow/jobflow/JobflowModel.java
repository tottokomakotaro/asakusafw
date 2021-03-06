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

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;

import com.asakusafw.compiler.common.Precondition;
import com.asakusafw.compiler.flow.Compilable;
import com.asakusafw.compiler.flow.ExternalIoDescriptionProcessor;
import com.asakusafw.compiler.flow.Location;
import com.asakusafw.compiler.flow.plan.FlowBlock;
import com.asakusafw.compiler.flow.plan.StageGraph;
import com.asakusafw.vocabulary.flow.graph.FlowElementOutput;
import com.asakusafw.vocabulary.flow.graph.InputDescription;
import com.asakusafw.vocabulary.flow.graph.OutputDescription;
import com.ashigeru.lang.java.model.syntax.Name;
import com.ashigeru.util.graph.Graph;
import com.ashigeru.util.graph.Graphs;

/**
 * ジョブフロー全体のモデル。
 */
public class JobflowModel extends Compilable.Trait<CompiledJobflow> {

    private StageGraph stageGraph;

    private String batchId;

    private String flowId;

    private List<Import> imports;

    private List<Export> exports;

    private List<Stage> stages;

    /**
     * インスタンスを生成する。
     * @param stageGraph このモデルの元となったステージグラフ
     * @param batchId このモデルに関連するバッチID
     * @param flowId このモデルに関連するフローID
     * @param imports ジョブフローへの入力の一覧
     * @param exports ジョブフローからの出力の一覧
     * @param stages ステージ情報の一覧
     * @throws IllegalArgumentException 引数に{@code null}が指定された場合
     */
    public JobflowModel(
            StageGraph stageGraph,
            String batchId,
            String flowId, List<Import> imports,
            List<Export> exports,
            List<Stage> stages) {
        Precondition.checkMustNotBeNull(stageGraph, "stageGraph"); //$NON-NLS-1$
        Precondition.checkMustNotBeNull(batchId, "batchId"); //$NON-NLS-1$
        Precondition.checkMustNotBeNull(flowId, "flowId"); //$NON-NLS-1$
        Precondition.checkMustNotBeNull(imports, "imports"); //$NON-NLS-1$
        Precondition.checkMustNotBeNull(exports, "exports"); //$NON-NLS-1$
        Precondition.checkMustNotBeNull(stages, "stages"); //$NON-NLS-1$
        this.stageGraph = stageGraph;
        this.batchId = batchId;
        this.flowId = flowId;
        this.imports = imports;
        this.exports = exports;
        this.stages = stages;
    }

    /**
     * このモデルの元となったステージグラフを返す。
     * @return このモデルの元となったステージグラフ
     */
    public StageGraph getStageGraph() {
        return stageGraph;
    }

    /**
     * このモデルに関連するバッチIDを返す。
     * @return このモデルに関連するバッチID
     */
    public String getBatchId() {
        return batchId;
    }

    /**
     * このモデルに関連するフローIDを返す。
     * @return このモデルに関連するフローID
     */
    public String getFlowId() {
        return flowId;
    }

    /**
     * ジョブフローへの入力の一覧を返す。
     * @return ジョブフローへの入力の一覧
     */
    public List<Import> getImports() {
        return imports;
    }

    /**
     * ジョブフローからの出力の一覧を返す。
     * @return ジョブフローからの出力の一覧
     */
    public List<Export> getExports() {
        return exports;
    }

    /**
     * ステージ情報の一覧を返す。
     * @return ステージ情報の一覧
     */
    public List<Stage> getStages() {
        return stages;
    }

    /**
     * このジョブフローに含まれるステージの関係を依存元から依存先へのグラフにして返す。
     * @return 構築したグラフ
     * @throws IllegalArgumentException 引数に{@code null}が指定された場合
     */
    public Graph<Stage> getDependencyGraph() {
        Map<Delivery, Stage> deliveries = new HashMap<Delivery, Stage>();
        for (Stage stage : stages) {
            for (Delivery delivery : stage.getDeliveries()) {
                deliveries.put(delivery, stage);
            }
        }
        Graph<Stage> graph = Graphs.newInstance();
        for (Stage stage : stages) {
            graph.addNode(stage);
            for (Process process : stage.getProcesses()) {
                for (Source source : process.getResolvedSources()) {
                    Stage dependence = deliveries.get(source);
                    if (dependence == null) {
                        // 先頭ステージ
                        continue;
                    }
                    graph.addEdge(stage, dependence);
                }
            }
        }
        return graph;
    }

    /**
     * ステージの定義。
     */
    public static class Stage extends Compilable.Trait<CompiledStage> {

        private int number;

        private List<Process> processes;

        private List<Delivery> deliveries;

        private Reduce reduceOrNull;

        private Set<SideData> sideData;

        /**
         * インスタンスを生成する。
         * @param number このステージのステージ番号
         * @param processes このステージのプロセス情報一覧
         * @param deliveries このステージの成果物一覧
         * @param reduceOrNull このステージのレデューサー定義、利用しない場合は{@code null}
         * @param sideData このステージで利用するサイドデータの一覧
         * @throws IllegalArgumentException 引数に{@code null}が指定された場合
         */
        public Stage(
                int number,
                List<Process> processes,
                List<Delivery> deliveries,
                Reduce reduceOrNull,
                Set<SideData> sideData) {
            Precondition.checkMustNotBeNull(processes, "processes"); //$NON-NLS-1$
            Precondition.checkMustNotBeNull(deliveries, "deliveries"); //$NON-NLS-1$
            Precondition.checkMustNotBeNull(sideData, "sideData"); //$NON-NLS-1$
            this.number = number;
            this.processes = processes;
            this.deliveries = deliveries;
            this.reduceOrNull = reduceOrNull;
            this.sideData = sideData;
        }

        /**
         * このステージのステージ番号を返す。
         * @return このステージのステージ番号
         * @throws IllegalArgumentException 引数に{@code null}が指定された場合
         */
        public int getNumber() {
            return number;
        }

        /**
         * このステージのプロセス情報を返す。
         * @return プロセス情報
         */
        public List<Process> getProcesses() {
            return processes;
        }

        /**
         * このステージの成果物情報を返す。
         * @return 成果物情報
         */
        public List<Delivery> getDeliveries() {
            return deliveries;
        }

        /**
         * レデューサーの定義を返す。
         * @return レデューサーの定義、レデュースフェーズが存在しない場合は{@code null}
         */
        public Reduce getReduceOrNull() {
            return reduceOrNull;
        }

        /**
         * このステージで利用するサイドデータの一覧を返す。
         * @return サイドデータ
         */
        public Set<SideData> getSideData() {
            return sideData;
        }

        @Override
        public String toString() {
            return MessageFormat.format(
                    "Stage({0})",
                    String.valueOf(getNumber()));
        }
    }

    /**
     * レデューサーの定義。
     */
    public static class Reduce {

        private Name reducerTypeName;

        private Name combinerTypeNameOrNull;

        private Name keyTypeName;

        private Name valueTypeName;

        private Name groupingComparatorTypeName;

        private Name sortComparatorTypeName;

        private Name partitionerTypeName;

        /**
         * インスタンスを生成する。
         * @param reducerTypeName レデューサークラスの完全限定名
         * @param combinerTypeNameOrNull コンバイナークラスの完全限定名、利用しない場合は{@code null}
         * @param keyTypeName キークラスの完全限定名
         * @param valueTypeName 値クラスの完全限定名
         * @param groupingComparatorTypeName グループ比較器クラスの完全限定名
         * @param sortComparatorTypeName 順序比較器クラスの完全限定名
         * @param partitionerTypeName パーティショナークラスの完全限定名
         * @throws IllegalArgumentException 引数に{@code null}が指定された場合
         */
        public Reduce(
                Name reducerTypeName,
                Name combinerTypeNameOrNull,
                Name keyTypeName,
                Name valueTypeName,
                Name groupingComparatorTypeName,
                Name sortComparatorTypeName,
                Name partitionerTypeName) {
            Precondition.checkMustNotBeNull(reducerTypeName, "reducerTypeName"); //$NON-NLS-1$
            Precondition.checkMustNotBeNull(keyTypeName, "keyTypeName"); //$NON-NLS-1$
            Precondition.checkMustNotBeNull(valueTypeName, "valueTypeName"); //$NON-NLS-1$
            Precondition.checkMustNotBeNull(groupingComparatorTypeName, "groupingComparatorTypeName"); //$NON-NLS-1$
            Precondition.checkMustNotBeNull(sortComparatorTypeName, "sortComparatorTypeName"); //$NON-NLS-1$
            Precondition.checkMustNotBeNull(partitionerTypeName, "partitionerTypeName"); //$NON-NLS-1$
            this.reducerTypeName = reducerTypeName;
            this.combinerTypeNameOrNull = combinerTypeNameOrNull;
            this.keyTypeName = keyTypeName;
            this.valueTypeName = valueTypeName;
            this.groupingComparatorTypeName = groupingComparatorTypeName;
            this.sortComparatorTypeName = sortComparatorTypeName;
            this.partitionerTypeName = partitionerTypeName;
        }

        /**
         * このオブジェクトに関連するコンバイナークラスの完全限定名を返す。
         * @return コンバイナークラスの完全限定名、利用しない場合は{@code null}
         */
        public Name getCombinerTypeNameOrNull() {
            return combinerTypeNameOrNull;
        }

        /**
         * このオブジェクトに関連するレデューサークラスの完全限定名を返す。
         * @return レデューサークラスの完全限定名
         */
        public Name getReducerTypeName() {
            return reducerTypeName;
        }

        /**
         * シャッフル時に利用するキークラスの完全限定名を返す。
         * @return キークラスの完全限定名
         */
        public Name getKeyTypeName() {
            return keyTypeName;
        }

        /**
         * シャッフル時に利用する値クラスの完全限定名を返す。
         * @return 値クラスの完全限定名
         */
        public Name getValueTypeName() {
            return valueTypeName;
        }

        /**
         * シャッフル時に利用するグループ化比較器クラスの完全限定名を返す。
         * @return グループ化比較器クラスの完全限定名
         */
        public Name getGroupingComparatorTypeName() {
            return groupingComparatorTypeName;
        }

        /**
         * シャッフル時に利用する順序比較器クラスの完全限定名を返す。
         * @return 順序比較器クラスの完全限定名
         */
        public Name getSortComparatorTypeName() {
            return sortComparatorTypeName;
        }

        /**
         * シャッフル時に利用するパーティショナークラスの完全限定名を返す。
         * @return パーティショナークラスの完全限定名
         */
        public Name getPartitionerTypeName() {
            return partitionerTypeName;
        }
    }

    /**
     * 何らかの出力を提供するインターフェース。
     */
    public abstract static class Source {

        private Set<FlowBlock.Output> outputs;

        /**
         * インスタンスを生成する。
         * @param outputs このソースに関連する出力ポートの一覧
         * @throws IllegalArgumentException 引数に{@code null}が指定された場合
         */
        protected Source(Set<FlowBlock.Output> outputs) {
            Precondition.checkMustNotBeNull(outputs, "outputs"); //$NON-NLS-1$
            this.outputs = outputs;
        }

        /**
         * このソースが提供する出力のパス一覧を返す。
         * @return 出力のパス一覧
         */
        public abstract Set<Location> getLocations();

        /**
         * このソースの内容を読み出すための{@link InputFormat}クラスを返す。
         * @return このソースの内容を読み出すための{@link InputFormat}クラス
         */
        @SuppressWarnings("rawtypes")
        public abstract Class<? extends InputFormat> getInputFormatType();

        /**
         * このソースに関連する出力ポートを返す。
         * @return 関連する出力ポート
         */
        public Set<FlowBlock.Output> getOutputs() {
            return outputs;
        }
    }

    /**
     * {@link JobflowModel.Source}からの出力を受け取るインターフェース。
     */
    public abstract static class Target {

        private List<FlowBlock.Input> inputs;

        private Set<Source> sources;

        /**
         * インスタンスを生成する。
         * @param inputs 関連する入力ポート群
         * @throws IllegalArgumentException 引数に{@code null}が指定された場合
         */
        public Target(List<FlowBlock.Input> inputs) {
            Precondition.checkMustNotBeNull(inputs, "inputs"); //$NON-NLS-1$
            if (inputs.isEmpty()) {
                throw new IllegalArgumentException("inputs must not be empty"); //$NON-NLS-1$
            }
            this.inputs = inputs;
        }

        /**
         * このオブジェクトが受け取るべき出力の一覧を設定する。
         * @param opposites 受け取るべき出力の一覧
         * @throws IllegalArgumentException 引数に{@code null}が指定された場合
         */
        public void resolveSources(Collection<? extends Source> opposites) {
            Precondition.checkMustNotBeNull(opposites, "opposites"); //$NON-NLS-1$
            this.sources = new HashSet<JobflowModel.Source>(opposites);
        }

        /**
         * このオブジェクトに設定された出力の一覧を返す。
         * @return 設定された出力の一覧
         * @throws IllegalStateException 設定されていない場合
         * @see #resolveSources(Collection)
         */
        public Set<Source> getResolvedSources() {
            if (sources == null) {
                throw new IllegalStateException();
            }
            return sources;
        }

        /**
         * このオブジェクトに設定された出力からのパス一覧を返す。
         * @return 設定された出力からのパス一覧
         * @throws IllegalStateException 設定されていない場合
         * @see #resolveSources(Collection)
         */
        public Set<Location> getResolvedLocations() {
            Set<Location> results = new HashSet<Location>();
            for (Source source : getResolvedSources()) {
                results.addAll(source.getLocations());
            }
            return results;
        }

        /**
         * このターゲットに関連する入力ポートを返す。
         * @return 関連する入力ポート
         */
        public List<FlowBlock.Input> getInputs() {
            return inputs;
        }

        /**
         * このターゲットのデータ型を返す。
         * @return このターゲットのデータ型
         */
        public java.lang.reflect.Type getDataType() {
            if (inputs.isEmpty()) {
                return void.class;
            }
            return inputs.get(0).getElementPort().getDescription().getDataType();
        }
    }

    /**
     * 外部入出力に対するプロセッサーを提供するインターフェース。
     */
    public interface Processible {

        /**
         * このオブジェクトの記述を処理するプロセッサーを返す。
         * @return 記述を処理するプロセッサー
         */
        ExternalIoDescriptionProcessor getProcessor();
    }

    /**
     * ステージで処理されるプロセスの情報。
     */
    public static class Process extends Target {

        private Name mapperTypeName;

        /**
         * インスタンスを生成する。
         * @param inputs 関連する入力ポート
         * @param mapperTypeName 処理を行うマッパークラスの限定名
         * @throws IllegalArgumentException 引数に{@code null}が指定された場合
         */
        public Process(List<FlowBlock.Input> inputs, Name mapperTypeName) {
            super(inputs);
            Precondition.checkMustNotBeNull(mapperTypeName, "mapperTypeName"); //$NON-NLS-1$
            this.mapperTypeName = mapperTypeName;
        }

        /**
         * 処理を行うマッパークラスの限定名を返す。
         * @return 処理を行うマッパークラスの限定名
         */
        public Name getMapperTypeName() {
            return mapperTypeName;
        }

        @Override
        public String toString() {
            return MessageFormat.format(
                    "Process(inputs={0}, mapper={1})",
                    getInputs(),
                    getMapperTypeName());
        }
    }

    /**
     * ステージで出力される成果物。
     */
    public static class Delivery extends Source {

        private Set<Location> locations;

        /**
         * インスタンスを生成する。
         * @param outputs 関連する出力ポートの一覧
         * @param locations 出力先のパス一覧
         * @throws IllegalArgumentException 引数に{@code null}が指定された場合
         */
        public Delivery(Set<FlowBlock.Output> outputs, Set<Location> locations) {
            super(outputs);
            Precondition.checkMustNotBeNull(locations, "locations"); //$NON-NLS-1$
            this.locations = locations;
        }

        /**
         * この成果物の型を返す。
         * @return この成果物の型
         */
        public java.lang.reflect.Type getDataType() {
            FlowBlock.Output first = getOutputs().iterator().next();
            FlowElementOutput port = first.getElementPort();
            return port.getDescription().getDataType();
        }

        @Override
        @SuppressWarnings("rawtypes")
        public Class<? extends InputFormat> getInputFormatType() {
            return SequenceFileInputFormat.class;
        }

        /**
         * この成果物の形式を表す{@link OutputFormat}クラスを返す。
         * @return この成果物の形式を表す{@link OutputFormat}クラス
         */
        @SuppressWarnings("rawtypes")
        public Class<? extends OutputFormat> getOutputFormatType() {
            return SequenceFileOutputFormat.class;
        }

        @Override
        public Set<Location> getLocations() {
            return locations;
        }

        @Override
        public String toString() {
            return MessageFormat.format(
                    "Delivery(output={0}, locations={1})",
                    getOutputs(),
                    getLocations());
        }
    }

    /**
     * フロー全体への入力。
     */
    public static class Import extends Source implements Processible {

        private InputDescription description;

        private ExternalIoDescriptionProcessor processor;

        /**
         * インスタンスを生成する。
         * @param description 入力記述
         * @param processor 上記記述を処理するプロセッサー
         * @throws IllegalArgumentException 引数に{@code null}が指定された場合
         */
        public Import(
                InputDescription description,
                ExternalIoDescriptionProcessor processor) {
            super(Collections.<FlowBlock.Output>emptySet());
            Precondition.checkMustNotBeNull(description, "description"); //$NON-NLS-1$
            Precondition.checkMustNotBeNull(processor, "processor"); //$NON-NLS-1$
            this.description = description;
            this.processor = processor;
        }

        /**
         * インスタンスを生成する。
         * @param output 関連する出力ポート
         * @param description 入力記述
         * @param processor 上記記述を処理するプロセッサー
         * @throws IllegalArgumentException 引数に{@code null}が指定された場合
         */
        public Import(
                FlowBlock.Output output,
                InputDescription description,
                ExternalIoDescriptionProcessor processor) {
            super(Collections.singleton(output));
            Precondition.checkMustNotBeNull(description, "description"); //$NON-NLS-1$
            Precondition.checkMustNotBeNull(processor, "processor"); //$NON-NLS-1$
            this.description = description;
            this.processor = processor;
        }

        /**
         * このオブジェクトが構成する識別子を返す。
         * @return このオブジェクトが構成する識別子
         */
        public String getId() {
            return description.getName();
        }

        @Override
        @SuppressWarnings("rawtypes")
        public Class<? extends InputFormat> getInputFormatType() {
            return processor.getInputFormatType(description);
        }

        /**
         * この入力を後続で利用する形式を表す{@link OutputFormat}クラスを返す。
         * @return この入力を後続で利用する形式を表す{@link OutputFormat}クラス
         */
        @SuppressWarnings("rawtypes")
        public Class<? extends OutputFormat> getOutputFormatType() {
            return SequenceFileOutputFormat.class;
        }

        @Override
        public Set<Location> getLocations() {
            return processor.getInputLocations(description);
        }

        /**
         * このオブジェクトの内容を表す入力記述を返す。
         * @return 記述
         */
        public InputDescription getDescription() {
            return description;
        }

        @Override
        public ExternalIoDescriptionProcessor getProcessor() {
            return processor;
        }

        @Override
        public String toString() {
            return MessageFormat.format(
                    "Import(output={0}, locations={1}, description={2})",
                    getOutputs(),
                    getLocations(),
                    getDescription());
        }
    }

    /**
     * フロー全体からの出力。
     */
    public static class Export extends Target implements Processible {

        private OutputDescription description;

        private ExternalIoDescriptionProcessor processor;

        /**
         * インスタンスを生成する。
         * @param inputs 関連する入力ポート
         * @param description 入力記述
         * @param processor 上記記述を処理するプロセッサー
         * @throws IllegalArgumentException 引数に{@code null}が指定された場合
         */
        public Export(
                List<FlowBlock.Input> inputs,
                OutputDescription description,
                ExternalIoDescriptionProcessor processor) {
            super(inputs);
            Precondition.checkMustNotBeNull(description, "description"); //$NON-NLS-1$
            Precondition.checkMustNotBeNull(processor, "processor"); //$NON-NLS-1$
            this.description = description;
            this.processor = processor;
        }

        /**
         * このオブジェクトが構成する識別子を返す。
         * @return このオブジェクトが構成する識別子
         */
        public String getId() {
            return description.getName();
        }

        /**
         * このオブジェクトの内容を表す出力記述を返す。
         * @return 記述
         */
        public OutputDescription getDescription() {
            return description;
        }

        @Override
        public ExternalIoDescriptionProcessor getProcessor() {
            return processor;
        }

        @Override
        public String toString() {
            return MessageFormat.format(
                    "Export(inputs={0}, description={1})",
                    getInputs(),
                    getDescription());
        }
    }

    /**
     * ステージで利用するサイドデータの一覧。
     */
    public static class SideData {

        private Location clusterPath;

        private String localName;

        /**
         * インスタンスを生成する。
         * @param clusterPath サイドデータのクラスター上のパス
         * @param localName ローカル上での名前
         * @throws IllegalArgumentException 引数に{@code null}が指定された場合
         */
        public SideData(Location clusterPath, String localName) {
            Precondition.checkMustNotBeNull(clusterPath, "clusterPath"); //$NON-NLS-1$
            Precondition.checkMustNotBeNull(localName, "localName"); //$NON-NLS-1$
            this.clusterPath = clusterPath;
            this.localName = localName;
        }

        /**
         * サイドデータのクラスター上のパスを返す。
         * @return サイドデータのクラスター上のパス
         */
        public Location getClusterPath() {
            return clusterPath;
        }

        /**
         * ローカル上での名前を返す。
         * @return ローカル上での名前
         */
        public String getLocalName() {
            return localName;
        }

        @Override
        public String toString() {
            return MessageFormat.format(
                    "SideData(path={0}, name={1})",
                    getClusterPath(),
                    getLocalName());
        }
    }
}
