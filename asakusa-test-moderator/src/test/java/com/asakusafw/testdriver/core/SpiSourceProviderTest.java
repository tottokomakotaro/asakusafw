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
package com.asakusafw.testdriver.core;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URI;
import org.junit.Test;

/**
 * Test for {@link SpiSourceProvider}.
 * @since 0.2.0
 */
public class SpiSourceProviderTest extends SpiTestRoot {

    /**
     * Test method for {@link SpiSourceProvider#open(DataModelDefinition, java.net.URI)}.
     * @throws Exception if failed
     */
    @Test
    public void open() throws Exception {
        ClassLoader cl = register(SourceProvider.class, Example.class);
        SpiSourceProvider target = new SpiSourceProvider(cl);
        DataModelSource source = target.open(ValueDefinition.of(String.class), new URI("testing:dummy"));
        assertThat(source, not(nullValue()));
        assertThat(ValueDefinition.of(String.class).toObject(source.next()), is("Hello, world!"));
    }

    /**
     * not found.
     * @throws Exception if failed
     */
    @Test
    public void open_notfound() throws Exception {
        ClassLoader cl = register(SourceProvider.class, Example.class);
        SpiSourceProvider target = new SpiSourceProvider(cl);
        DataModelSource source = target.open(ValueDefinition.of(String.class), new URI("dummy:dummy"));
        assertThat(source, is(nullValue()));
    }

    /**
     * Example service.
     */
    public static class Example implements SourceProvider {

        @Override
        public <T> DataModelSource open(DataModelDefinition<T> definition, URI source)
                throws IOException {
            if (source.getScheme().equals("testing") == false) {
                return null;
            }
            return source(ValueDefinition.of(String.class), "Hello, world!");
        }
    }
}
