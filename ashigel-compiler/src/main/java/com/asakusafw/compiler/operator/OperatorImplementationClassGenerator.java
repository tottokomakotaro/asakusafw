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
package com.asakusafw.compiler.operator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.lang.model.type.TypeMirror;

import com.asakusafw.compiler.common.NameGenerator;
import com.ashigeru.lang.java.model.syntax.ConstructorDeclaration;
import com.ashigeru.lang.java.model.syntax.FormalParameterDeclaration;
import com.ashigeru.lang.java.model.syntax.Javadoc;
import com.ashigeru.lang.java.model.syntax.ModelFactory;
import com.ashigeru.lang.java.model.syntax.SimpleName;
import com.ashigeru.lang.java.model.syntax.Type;
import com.ashigeru.lang.java.model.syntax.TypeBodyDeclaration;
import com.ashigeru.lang.java.model.util.AttributeBuilder;
import com.ashigeru.lang.java.model.util.ImportBuilder;
import com.ashigeru.lang.java.model.util.JavadocBuilder;

/**
 * 演算子実装クラスの情報を構築するジェネレータ。
 */
public class OperatorImplementationClassGenerator extends OperatorClassGenerator {

    /**
     * インスタンスを生成する。
     * @param environment 環境オブジェクト
     * @param factory DOMを構築するためのファクトリ
     * @param importer インポート宣言を構築するビルダー
     * @param operatorClass 演算子クラスの情報
     * @throws IllegalArgumentException 引数に{@code null}が含まれる場合
     */
    public OperatorImplementationClassGenerator(
            OperatorCompilingEnvironment environment,
            ModelFactory factory,
            ImportBuilder importer,
            OperatorClass operatorClass) {
        super(environment, factory, importer, operatorClass);
    }

    @Override
    protected SimpleName getClassName() {
        return util.getImplementorName(operatorClass.getElement());
    }

    @Override
    protected Type getSuperClass() {
        TypeMirror type = operatorClass.getElement().asType();
        return util.t(type);
    }

    @Override
    protected Javadoc createJavadoc() {
        return new JavadocBuilder(factory)
            .linkType(util.t(operatorClass.getElement()))
            .text("に関する演算子実装クラス。")
            .toJavadoc();
    }

    @Override
    protected List<TypeBodyDeclaration> createMembers() {
        NameGenerator names = new NameGenerator(factory);
        List<TypeBodyDeclaration> results = new ArrayList<TypeBodyDeclaration>();
        results.add(createConstructor());
        for (OperatorMethod method : operatorClass.getMethods()) {
            OperatorProcessor.Context context = new OperatorProcessor.Context(
                    environment,
                    method.getAnnotation(),
                    method.getElement(),
                    importer,
                    names);
            OperatorProcessor processor = method.getProcessor();
            List<? extends TypeBodyDeclaration> members = processor.implement(context);
            if (members != null) {
                results.addAll(members);
            }
        }
        return results;
    }

    private ConstructorDeclaration createConstructor() {
        return factory.newConstructorDeclaration(
                new JavadocBuilder(factory)
                    .text("インスタンスを生成する。")
                    .toJavadoc(),
                new AttributeBuilder(factory)
                    .Public()
                    .toAttributes(),
                getClassName(),
                Collections.<FormalParameterDeclaration>emptyList(),
                Collections.singletonList(factory.newReturnStatement()));
    }
}
