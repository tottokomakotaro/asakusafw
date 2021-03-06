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
package com.asakusafw.compiler.flow.stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.hadoop.io.RawComparator;
import org.apache.hadoop.io.WritableComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.compiler.common.Naming;
import com.asakusafw.compiler.common.Precondition;
import com.asakusafw.compiler.flow.FlowCompilingEnvironment;
import com.asakusafw.compiler.flow.stage.ShuffleModel.Arrangement;
import com.asakusafw.compiler.flow.stage.ShuffleModel.Segment;
import com.asakusafw.compiler.flow.stage.ShuffleModel.Term;
import com.asakusafw.runtime.flow.SegmentedWritable;
import com.ashigeru.lang.java.model.syntax.Comment;
import com.ashigeru.lang.java.model.syntax.CompilationUnit;
import com.ashigeru.lang.java.model.syntax.Expression;
import com.ashigeru.lang.java.model.syntax.FormalParameterDeclaration;
import com.ashigeru.lang.java.model.syntax.IfStatement;
import com.ashigeru.lang.java.model.syntax.InfixOperator;
import com.ashigeru.lang.java.model.syntax.Javadoc;
import com.ashigeru.lang.java.model.syntax.MethodDeclaration;
import com.ashigeru.lang.java.model.syntax.ModelFactory;
import com.ashigeru.lang.java.model.syntax.Name;
import com.ashigeru.lang.java.model.syntax.SimpleName;
import com.ashigeru.lang.java.model.syntax.Statement;
import com.ashigeru.lang.java.model.syntax.Type;
import com.ashigeru.lang.java.model.syntax.TypeBodyDeclaration;
import com.ashigeru.lang.java.model.syntax.TypeDeclaration;
import com.ashigeru.lang.java.model.syntax.TypeParameterDeclaration;
import com.ashigeru.lang.java.model.util.AttributeBuilder;
import com.ashigeru.lang.java.model.util.ExpressionBuilder;
import com.ashigeru.lang.java.model.util.ImportBuilder;
import com.ashigeru.lang.java.model.util.JavadocBuilder;
import com.ashigeru.lang.java.model.util.Models;
import com.ashigeru.lang.java.model.util.TypeBuilder;

/**
 * シャッフルフェーズで利用するグループ比較器を生成する。
 */
public class ShuffleGroupingComparatorEmitter {

    static final Logger LOG = LoggerFactory.getLogger(ShuffleGroupingComparatorEmitter.class);

    private FlowCompilingEnvironment environment;

    /**
     * インスタンスを生成する。
     * @param environment 環境オブジェクト
     * @throws IllegalArgumentException 引数に{@code null}が指定された場合
     */
    public ShuffleGroupingComparatorEmitter(FlowCompilingEnvironment environment) {
        Precondition.checkMustNotBeNull(environment, "environment"); //$NON-NLS-1$
        this.environment = environment;
    }

    /**
     * 指定のモデルに対するグループ比較器を表すクラスを生成し、生成したクラスの完全限定名を返す。
     * @param model 対象のモデル
     * @param keyTypeName キー型の完全限定名
     * @return 生成したクラスの完全限定名
     * @throws IOException クラスの生成に失敗した場合
     * @throws IllegalArgumentException 引数に{@code null}が指定された場合
     */
    public Name emit(
            ShuffleModel model,
            Name keyTypeName) throws IOException {
        Precondition.checkMustNotBeNull(model, "model"); //$NON-NLS-1$
        Precondition.checkMustNotBeNull(keyTypeName, "keyTypeName"); //$NON-NLS-1$
        LOG.debug("{}に対するグループ比較器を生成します", model.getStageBlock());
        Engine engine = new Engine(environment, model, keyTypeName);
        CompilationUnit source = engine.generate();
        environment.emit(source);
        Name packageName = source.getPackageDeclaration().getName();
        SimpleName simpleName = source.getTypeDeclarations().get(0).getName();
        Name name = environment.getModelFactory().newQualifiedName(packageName, simpleName);
        LOG.debug("{}のグループ比較には{}が利用されます",
                model.getStageBlock(),
                name);
        return name;
    }

    private static class Engine {

        private ShuffleModel model;

        private ModelFactory factory;

        private ImportBuilder importer;

        private Type keyType;

        public Engine(
                FlowCompilingEnvironment environment,
                ShuffleModel model,
                Name keyTypeName) {
            assert environment != null;
            assert model != null;
            assert keyTypeName != null;
            this.model = model;
            this.factory = environment.getModelFactory();
            Name packageName = environment.getStagePackageName(model.getStageBlock().getStageNumber());
            this.importer = new ImportBuilder(
                    factory,
                    factory.newPackageDeclaration(packageName),
                    ImportBuilder.Strategy.TOP_LEVEL);
            this.keyType = importer.resolve(factory.newNamedType(keyTypeName));
        }

        public CompilationUnit generate() {
            TypeDeclaration type = createType();
            return factory.newCompilationUnit(
                    importer.getPackageDeclaration(),
                    importer.toImportDeclarations(),
                    Collections.singletonList(type),
                    Collections.<Comment>emptyList());
        }

        private TypeDeclaration createType() {
            SimpleName name = factory.newSimpleName(Naming.getShuffleGroupingComparatorClass());
            importer.resolvePackageMember(name);
            List<TypeBodyDeclaration> members = new ArrayList<TypeBodyDeclaration>();
            members.add(createCompareBytes());
            members.add(createCompareObjects());
            members.add(ShuffleEmiterUtil.createCompareInts(factory));
            members.add(ShuffleEmiterUtil.createPortToElement(factory, model));
            return factory.newClassDeclaration(
                    createJavadoc(),
                    new AttributeBuilder(factory)
                        .annotation(t(SuppressWarnings.class), v("rawtypes"))
                        .Public()
                        .toAttributes(),
                    name,
                    Collections.<TypeParameterDeclaration>emptyList(),
                    null,
                    Collections.singletonList(
                            importer.resolve(factory.newParameterizedType(
                                    t(RawComparator.class),
                                    Collections.singletonList(keyType)))),
                    members);
        }

        private MethodDeclaration createCompareBytes() {
            SimpleName b1 = factory.newSimpleName("b1");
            SimpleName s1 = factory.newSimpleName("s1");
            SimpleName l1 = factory.newSimpleName("l1");
            SimpleName b2 = factory.newSimpleName("b2");
            SimpleName s2 = factory.newSimpleName("s2");
            SimpleName l2 = factory.newSimpleName("l2");

            List<Statement> statements = new ArrayList<Statement>();
            SimpleName segmentId1 = factory.newSimpleName("segmentId1");
            SimpleName segmentId2 = factory.newSimpleName("segmentId2");
            statements.add(new TypeBuilder(factory, t(WritableComparator.class))
                .method("readInt", b1, s1)
                .toLocalVariableDeclaration(t(int.class), segmentId1));
            statements.add(new TypeBuilder(factory, t(WritableComparator.class))
                .method("readInt", b2, s2)
                .toLocalVariableDeclaration(t(int.class), segmentId2));

            SimpleName diff = factory.newSimpleName("diff");
            statements.add(new ExpressionBuilder(factory, factory.newThis())
                .method(ShuffleEmiterUtil.COMPARE_INT,
                        new ExpressionBuilder(factory, factory.newThis())
                            .method(ShuffleEmiterUtil.PORT_TO_ELEMENT, segmentId1)
                            .toExpression(),
                        new ExpressionBuilder(factory, factory.newThis())
                            .method(ShuffleEmiterUtil.PORT_TO_ELEMENT, segmentId2)
                            .toExpression())
                .toLocalVariableDeclaration(t(int.class), diff));
            statements.add(createDiffBranch(diff));

            SimpleName o1 = factory.newSimpleName("o1");
            SimpleName o2 = factory.newSimpleName("o2");
            SimpleName size1 = factory.newSimpleName("size1");
            SimpleName size2 = factory.newSimpleName("size2");
            statements.add(new ExpressionBuilder(factory, v(4))
                .toLocalVariableDeclaration(t(int.class), o1));
            statements.add(new ExpressionBuilder(factory, v(4))
                .toLocalVariableDeclaration(t(int.class), o2));
            statements.add(new ExpressionBuilder(factory, v(-1))
                .toLocalVariableDeclaration(t(int.class), size1));
            statements.add(new ExpressionBuilder(factory, v(-1))
                .toLocalVariableDeclaration(t(int.class), size2));

            List<Statement> cases = new ArrayList<Statement>();
            for (List<Segment> segments : ShuffleEmiterUtil.groupByElement(model)) {
                for (Segment segment : segments) {
                    cases.add(factory.newSwitchCaseLabel(v(segment.getPortId())));
                }
                for (Term term : segments.get(0).getTerms()) {
                    if (term.getArrangement() != Arrangement.GROUPING) {
                        continue;
                    }
                    cases.add(new ExpressionBuilder(factory, size1)
                        .assignFrom(term.getSource().createBytesSize(
                                b1,
                                factory.newInfixExpression(s1, InfixOperator.PLUS, o1),
                                factory.newInfixExpression(l1, InfixOperator.MINUS, o1)))
                        .toStatement());
                    cases.add(new ExpressionBuilder(factory, size2)
                        .assignFrom(term.getSource().createBytesSize(
                                b2,
                                factory.newInfixExpression(s2, InfixOperator.PLUS, o2),
                                factory.newInfixExpression(l2, InfixOperator.MINUS, o2)))
                        .toStatement());
                    cases.add(new ExpressionBuilder(factory, diff)
                        .assignFrom(
                                term.getSource().createBytesDiff(
                                        b1,
                                        factory.newInfixExpression(s1, InfixOperator.PLUS, o1),
                                        size1,
                                        b2,
                                        factory.newInfixExpression(s2, InfixOperator.PLUS, o2),
                                        size2))
                        .toStatement());
                    cases.add(createDiffBranch(diff));
                    cases.add(new ExpressionBuilder(factory, o1)
                        .assignFrom(InfixOperator.PLUS, size1)
                        .toStatement());
                    cases.add(new ExpressionBuilder(factory, o2)
                        .assignFrom(InfixOperator.PLUS, size2)
                        .toStatement());
                }
                cases.add(factory.newBreakStatement());
            }
            cases.add(factory.newSwitchDefaultLabel());
            cases.add(new TypeBuilder(factory, t(AssertionError.class))
                .newObject()
                .toThrowStatement());

            statements.add(factory.newSwitchStatement(segmentId1, cases));
            statements.add(new ExpressionBuilder(factory, v(0))
                .toReturnStatement());

            return factory.newMethodDeclaration(
                    null,
                    new AttributeBuilder(factory)
                        .annotation(t(Override.class))
                        .Public()
                        .toAttributes(),
                    t(int.class),
                    factory.newSimpleName("compare"),
                    Arrays.asList(new FormalParameterDeclaration[] {
                            factory.newFormalParameterDeclaration(t(byte[].class), b1),
                            factory.newFormalParameterDeclaration(t(int.class), s1),
                            factory.newFormalParameterDeclaration(t(int.class), l1),
                            factory.newFormalParameterDeclaration(t(byte[].class), b2),
                            factory.newFormalParameterDeclaration(t(int.class), s2),
                            factory.newFormalParameterDeclaration(t(int.class), l2),
                    }),
                    statements);
        }

        private IfStatement createDiffBranch(SimpleName diff) {
            return factory.newIfStatement(
                    new ExpressionBuilder(factory, diff)
                        .apply(InfixOperator.NOT_EQUALS, v(0))
                        .toExpression(),
                    new ExpressionBuilder(factory, diff)
                        .toReturnStatement(),
                    null);
        }

        private TypeBodyDeclaration createCompareObjects() {
            SimpleName o1 = factory.newSimpleName("o1");
            SimpleName o2 = factory.newSimpleName("o2");

            List<Statement> statements = new ArrayList<Statement>();
            SimpleName segmentId1 = factory.newSimpleName("segmentId1");
            SimpleName segmentId2 = factory.newSimpleName("segmentId2");
            statements.add(new ExpressionBuilder(factory, o1)
                .method(SegmentedWritable.ID_GETTER)
                .toLocalVariableDeclaration(t(int.class), segmentId1));
            statements.add(new ExpressionBuilder(factory, o2)
                .method(SegmentedWritable.ID_GETTER)
                .toLocalVariableDeclaration(t(int.class), segmentId2));

            SimpleName diff = factory.newSimpleName("diff");
            statements.add(new ExpressionBuilder(factory, factory.newThis())
                .method(ShuffleEmiterUtil.COMPARE_INT,
                        new ExpressionBuilder(factory, factory.newThis())
                            .method(ShuffleEmiterUtil.PORT_TO_ELEMENT, segmentId1)
                            .toExpression(),
                        new ExpressionBuilder(factory, factory.newThis())
                            .method(ShuffleEmiterUtil.PORT_TO_ELEMENT, segmentId2)
                            .toExpression())
                .toLocalVariableDeclaration(t(int.class), diff));
            statements.add(createDiffBranch(diff));

            List<Statement> cases = new ArrayList<Statement>();
            for (List<Segment> segments : ShuffleEmiterUtil.groupByElement(model)) {
                for (Segment segment : segments) {
                    cases.add(factory.newSwitchCaseLabel(v(segment.getPortId())));
                }
                Segment segment = segments.get(0);
                for (Term term : segment.getTerms()) {
                    if (term.getArrangement() != Arrangement.GROUPING) {
                        continue;
                    }
                    String name = ShuffleEmiterUtil.getPropertyName(segment, term);
                    Expression rhs = term.getSource().createValueDiff(
                            new ExpressionBuilder(factory, o1)
                                .field(name)
                                .toExpression(),
                            new ExpressionBuilder(factory, o2)
                                .field(name)
                                .toExpression());
                    cases.add(new ExpressionBuilder(factory, diff)
                        .assignFrom(rhs)
                        .toStatement());
                    cases.add(createDiffBranch(diff));
                }
                cases.add(factory.newBreakStatement());
            }
            cases.add(factory.newSwitchDefaultLabel());
            cases.add(new TypeBuilder(factory, t(AssertionError.class))
                .newObject()
                .toThrowStatement());

            statements.add(factory.newSwitchStatement(segmentId1, cases));
            statements.add(new ExpressionBuilder(factory, v(0))
                .toReturnStatement());

            return factory.newMethodDeclaration(
                    null,
                    new AttributeBuilder(factory)
                        .annotation(t(Override.class))
                        .Public()
                        .toAttributes(),
                    t(int.class),
                    factory.newSimpleName("compare"),
                    Arrays.asList(new FormalParameterDeclaration[] {
                            factory.newFormalParameterDeclaration(keyType, o1),
                            factory.newFormalParameterDeclaration(keyType, o2),
                    }),
                    statements);
        }

        private Javadoc createJavadoc() {
            return new JavadocBuilder(factory)
                .text("ステージ#{0}シャッフルで利用するグループ比較器。",
                    model.getStageBlock().getStageNumber())
                .toJavadoc();
        }

        private Type t(java.lang.reflect.Type type) {
            return importer.resolve(Models.toType(factory, type));
        }

        private Expression v(Object value) {
            return Models.toLiteral(factory, value);
        }
    }
}
