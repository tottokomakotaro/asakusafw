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
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;

import com.asakusafw.compiler.batch.BatchDriver;
import com.asakusafw.compiler.batch.experimental.ExperimentalWorkflowProcessor;
import com.asakusafw.compiler.flow.ExternalIoCommandProvider.CommandContext;
import com.asakusafw.compiler.flow.Location;
import com.asakusafw.compiler.testing.DirectBatchCompiler;
import com.asakusafw.compiler.testing.DirectFlowCompiler;
import com.asakusafw.vocabulary.batch.BatchDescription;

/**
 * バッチ用のテストドライバクラス。
 */
public class BatchTestDriver extends TestDriverTestToolsBase {

    /**
     * コンストラクタ。
     *
     * @throws RuntimeException インスタンスの生成に失敗した場合
     */
    public BatchTestDriver() throws RuntimeException {
        super();
    }

    /**
     * バッチのテストを実行し、テスト結果を検証します。
     * @param batchDescriptionClass バッチクラスのクラスオブジェクト
     */
    public void runTest(Class<? extends BatchDescription> batchDescriptionClass) {

        // クラスタワークディレクトリ初期化
        try {
            JobflowExecutor executor = new JobflowExecutor(driverContext);
            executor.cleanWorkingDirectory();

            // テストデータ生成ツールを実行し、Excel上のテストデータ定義をデータベースに登録する。
            storeDatabase();

            // バッチコンパイラの実行
            BatchDriver batchDriver = BatchDriver.analyze(batchDescriptionClass);
            assertFalse(
                    batchDriver.getDiagnostics().toString(),
                    batchDriver.hasError());

            File compileWorkDir = driverContext.getCompilerWorkingDirectory();
            if (compileWorkDir.exists()) {
                FileUtils.forceDelete(compileWorkDir);
            }

            File compilerOutputDir = new File(compileWorkDir, "output");
            File compilerLocalWorkingDir = new File(compileWorkDir, "build");

            DirectBatchCompiler.compile(
                    batchDescriptionClass,
                    "test.batch",
                    Location.fromPath(driverContext.getClusterWorkDir(), '/'),
                    compilerOutputDir,
                    compilerLocalWorkingDir,
                    Arrays.asList(new File[] {
                            DirectFlowCompiler.toLibraryPath(batchDescriptionClass)
                    }),
                    batchDescriptionClass.getClassLoader(),
                    driverContext.getOptions());

            // バッチコンパイラが生成したテスト用シェルスクリプトを実行
            String[] batchRunCmd = new String[] {
                    "/bin/bash",
                    ExperimentalWorkflowProcessor.getScriptOutput(compilerOutputDir).getAbsolutePath(),
            };
            // 環境変数に-Dの引数一覧を積む
            StringBuilder dProps = new StringBuilder();
            for (Map.Entry<String, String> entry : driverContext.getExtraConfigurations().entrySet()) {
                dProps.append(MessageFormat.format(
                        " -D \"{0}={1}\"",
                        entry.getKey(),
                        entry.getValue()));
            }

            CommandContext context = driverContext.getCommandContext();

            Map<String, String> environment = new TreeMap<String, String>();
            environment.put(ExperimentalWorkflowProcessor.VAR_BATCH_ARGS, context.getVariableList());
            environment.put(ExperimentalWorkflowProcessor.K_OPTS, dProps.toString());

            int exitCode = executor.runShell(batchRunCmd, environment);
            if (exitCode != 0) {
                throw new AssertionError(MessageFormat.format(
                        "バッチの実行に失敗しました (exitCode={0}): {1}",
                        exitCode,
                        batchDescriptionClass.getName()));
            }

            // テスト結果検証ツールを実行し、Excel上の期待値とDB上の実際値を比較する。
            loadDatabase();
            if (!testUtils.inspect()) {
                Assert.fail(testUtils.getCauseMessage());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void storeDatabase() {
        testUtils.storeToDatabase(false);
    }

    private void loadDatabase() {
        testUtils.loadFromDatabase();
    }
}
