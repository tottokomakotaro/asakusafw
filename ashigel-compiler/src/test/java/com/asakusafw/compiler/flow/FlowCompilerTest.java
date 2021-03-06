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
package com.asakusafw.compiler.flow;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Comparator;
import java.util.List;


import org.junit.Rule;
import org.junit.Test;

import com.asakusafw.compiler.flow.FlowCompiler;
import com.asakusafw.compiler.flow.example.NoShuffleStage;
import com.asakusafw.compiler.flow.example.SimpleShuffleStage;
import com.asakusafw.compiler.flow.testing.model.Ex1;
import com.asakusafw.compiler.flow.testing.model.ExSummarized;
import com.asakusafw.compiler.util.CompilerTester;
import com.asakusafw.compiler.util.CompilerTester.TestInput;
import com.asakusafw.compiler.util.CompilerTester.TestOutput;
import com.asakusafw.vocabulary.flow.FlowDescription;

/**
 * Test for {@link FlowCompiler}.
 */
public class FlowCompilerTest {

    @SuppressWarnings("all")
    @Rule
    public CompilerTester tester = new CompilerTester();

    /**
     * Mapperのみのテスト。
     * @throws Exception テストに失敗した場合
     */
    @Test
    public void mapOnly() throws Exception {
        TestInput<Ex1> in = tester.input(Ex1.class, "ex1");
        TestOutput<Ex1> out = tester.output(Ex1.class, "ex1");

        Ex1 ex1 = new Ex1();
        ex1.setSid(1);
        ex1.setValue(10);
        in.add(ex1);

        FlowDescription flow = new NoShuffleStage(in.flow(), out.flow());
        assertThat(tester.runFlow(flow), is(true));

        List<Ex1> list = out.toList();
        assertThat(list.size(), is(1));
        assertThat(list.get(0).getValue(), is(100));
    }

    /**
     * Reducerを含むテスト。
     * @throws Exception テストに失敗した場合
     */
    @Test
    public void withReduce() throws Exception {
        TestInput<Ex1> in = tester.input(Ex1.class, "ex1");
        TestOutput<ExSummarized> out = tester.output(ExSummarized.class, "exs");

        Ex1 ex1 = new Ex1();
        ex1.setStringAsString("group-1");
        ex1.setValue(10);
        in.add(ex1);
        ex1.setValue(20);
        in.add(ex1);
        ex1.setValue(30);
        in.add(ex1);
        ex1.setStringAsString("group-2");
        ex1.setValue(40);
        in.add(ex1);
        ex1.setValue(50);
        in.add(ex1);

        FlowDescription flow = new SimpleShuffleStage(in.flow(), out.flow());
        assertThat(tester.runFlow(flow), is(true));

        List<ExSummarized> list = out.toList(new Comparator<ExSummarized>() {
            @Override
            public int compare(ExSummarized o1, ExSummarized o2) {
                return o1.getCountOption().compareTo(o2.getCountOption());
            }
        });
        assertThat(list.size(), is(2));
        assertThat(list.get(0).getValue(), is(90L));
        assertThat(list.get(0).getCount(), is(2L));
        assertThat(list.get(1).getValue(), is(60L));
        assertThat(list.get(1).getCount(), is(3L));
    }
}
