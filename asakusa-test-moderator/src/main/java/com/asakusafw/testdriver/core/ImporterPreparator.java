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

import java.io.IOException;

import com.asakusafw.runtime.io.ModelOutput;
import com.asakusafw.vocabulary.external.ImporterDescription;

/**
 * Creates test input for suitable {@link ImporterDescription}.
 * <p>
 * Adding {@link ImporterDescription} test moderators, clients can implement this
 * and put the class name in
 * {@code META-INF/services/com.asakusafw.testdriver.core.ImporterPreparator}.
 * </p>
 * @param <T> type of target {@link ImporterDescription}
 * @since 0.2.0
 * @see AbstractImporterPreparator
 */
public interface ImporterPreparator<T extends ImporterDescription> {

    /**
     * Returns the class of target {@link ImporterDescription}.
     * @return the class
     */
    Class<T> getDescriptionClass();

    /**
     * Truncates all resources which the importer will use.
     * <p>
     * If target resources do not support truncate operations,
     * this method has no effects.
     * </p>
     * @param description the description
     * @throws IOException if failed to open the target
     */
    void truncate(T description) throws IOException;

    /**
     * Creates a {@link ModelOutput} to prepare the resource which the importer will use.
     * @param <V> type of model
     * @param definition the data model definition
     * @param description the description
     * @return the created {@link ModelOutput}
     * @throws IOException if failed to open the target
     */
    <V> ModelOutput<V> createOutput(DataModelDefinition<V> definition, T description) throws IOException;
}
