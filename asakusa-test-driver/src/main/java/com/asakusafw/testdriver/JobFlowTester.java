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
package com.asakusafw.testdriver;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.compiler.flow.JobFlowClass;
import com.asakusafw.compiler.flow.JobFlowDriver;
import com.asakusafw.compiler.flow.Location;
import com.asakusafw.compiler.testing.DirectFlowCompiler;
import com.asakusafw.compiler.testing.JobflowInfo;
import com.asakusafw.testdriver.core.VerifyContext;
import com.asakusafw.vocabulary.flow.FlowDescription;
import com.asakusafw.vocabulary.flow.graph.FlowGraph;

/**
 * ジョブフロー用のテストドライバクラス。
 */
public class JobFlowTester extends TestDriverBase {

    static final Logger LOG = LoggerFactory.getLogger(JobFlowTester.class);
    /** 入力データのリスト。 */
    protected List<JobFlowDriverInput<?>> inputs = new LinkedList<JobFlowDriverInput<?>>();
    /** 出力データのリスト。 */
    protected List<JobFlowDriverOutput<?>> outputs = new LinkedList<JobFlowDriverOutput<?>>();

    /**
     * コンストラクタ。
     *
     * @param callerClass 呼出元クラス
     */
    public JobFlowTester(Class<?> callerClass) {
        super(callerClass);
    }

    /**
     * テスト入力データを指定する。
     *
     * @param <T> ModelType。
     * @param name 入力データ名。テストドライバに指定する入力データ間で一意の名前を指定する。
     * @param modelType ModelType。
     * @return テスト入力データオブジェクト。
     */
    public <T> JobFlowDriverInput<T> input(String name, Class<T> modelType) {
        JobFlowDriverInput<T> input = new JobFlowDriverInput<T>(driverContext, name, modelType);
        inputs.add(input);
        return input;
    }

    /**
     * テスト結果の出力データ（期待値データ）を指定する。
     *
     * @param <T> ModelType。
     * @param name 出力データ名。テストドライバに指定する出力データ間で一意の名前を指定する。
     * @param modelType ModelType。
     * @return テスト入力データオブジェクト。
     */
    public <T> JobFlowDriverOutput<T> output(String name, Class<T> modelType) {
        JobFlowDriverOutput<T> output = new JobFlowDriverOutput<T>(driverContext, name, modelType);
        outputs.add(output);
        return output;
    }

    /**
     * ジョブフローのテストを実行し、テスト結果を検証します。
     * @param jobFlowDescriptionClass ジョブフロークラスのクラスオブジェクト
     * @throws RuntimeException テストの実行に失敗した場合
     */
    public void runTest(Class<? extends FlowDescription> jobFlowDescriptionClass) {
        try {
            runTestInternal(jobFlowDescriptionClass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void runTestInternal(Class<? extends FlowDescription> jobFlowDescriptionClass) throws IOException {
        LOG.info("テストを開始しています: {}", driverContext.getCallerClass().getName());

        // フローコンパイラの実行
        LOG.info("ジョブフローをコンパイルしています: {}", jobFlowDescriptionClass.getName());
        JobFlowDriver jobFlowDriver = JobFlowDriver.analyze(jobFlowDescriptionClass);
        assertFalse(jobFlowDriver.getDiagnostics().toString(), jobFlowDriver.hasError());
        JobFlowClass jobFlowClass = jobFlowDriver.getJobFlowClass();

        String batchId = "bid";
        String flowId = jobFlowClass.getConfig().name();
        File compileWorkDir = driverContext.getCompilerWorkingDirectory();
        if (compileWorkDir.exists()) {
            FileUtils.forceDelete(compileWorkDir);
        }

        FlowGraph flowGraph = jobFlowClass.getGraph();
        JobflowInfo jobflowInfo = DirectFlowCompiler.compile(
                flowGraph,
                batchId,
                flowId,
                "test.jobflow",
                Location.fromPath(driverContext.getClusterWorkDir(), '/'),
                compileWorkDir,
                Arrays.asList(new File[] {
                        DirectFlowCompiler.toLibraryPath(jobFlowDescriptionClass)
                }),
                jobFlowDescriptionClass.getClassLoader(),
                driverContext.getOptions());

        LOG.info("テスト環境を初期化しています: {}", driverContext.getCallerClass().getName());
        JobflowExecutor executor = new JobflowExecutor(driverContext);
        executor.cleanWorkingDirectory();
        executor.cleanInputOutput(jobflowInfo);

        LOG.info("テストデータを配置しています: {}", driverContext.getCallerClass().getName());
        executor.prepareInput(jobflowInfo, inputs);
        executor.prepareOutput(jobflowInfo, outputs);

        LOG.info("ジョブフローを実行しています: {}", jobFlowDescriptionClass.getName());
        VerifyContext verifyContext = new VerifyContext();
        executor.runJobflow(jobflowInfo);
        verifyContext.testFinished();

        LOG.info("実行結果を検証しています: {}", driverContext.getCallerClass().getName());
        executor.verify(jobflowInfo, verifyContext, outputs);
    }
}
