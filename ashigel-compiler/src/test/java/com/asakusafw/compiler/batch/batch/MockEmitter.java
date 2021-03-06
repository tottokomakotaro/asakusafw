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
package com.asakusafw.compiler.batch.batch;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.ashigeru.lang.java.jsr199.testing.VolatileJavaFile;
import com.ashigeru.lang.java.model.syntax.PackageDeclaration;
import com.ashigeru.lang.java.model.util.Emitter;

/**
 * メモリ上にエミットする{@link Emitter}の実装。
 */
public class MockEmitter extends Emitter {

    private List<VolatileJavaFile> emitted = new ArrayList<VolatileJavaFile>();

    @Override
    public PrintWriter openFor(
            PackageDeclaration packageDeclOrNull,
            String subPath) throws IOException {
        StringBuilder buf = new StringBuilder();
        if (packageDeclOrNull != null) {
            buf.append(packageDeclOrNull.getName().toNameString().replace('.', '/'));
            buf.append("/");
        }
        assert subPath.endsWith(".java");
        buf.append(subPath.substring(0, subPath.length() - 5));
        VolatileJavaFile file = new VolatileJavaFile(buf.toString());
        register(file);
        return new PrintWriter(file.openWriter());
    }

    private void register(VolatileJavaFile file) {
        emitted.add(file);
    }

    /**
     * これまでにエミットされたファイルの一覧を返す。
     * @return これまでにエミットされたファイルの一覧
     */
    public List<VolatileJavaFile> getEmitted() {
        return emitted;
    }
}
