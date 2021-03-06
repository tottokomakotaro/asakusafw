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
package com.asakusafw.dmdl.parser;

import java.io.Reader;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.dmdl.model.AstScript;

/**
 * Parses DMDL scripts and creates AST.
 */
public class DmdlParser {

    static final Logger LOG = LoggerFactory.getLogger(DmdlParser.class);

    /**
     * Analyze DMDL Script from the source stream and returns a corresponded AST.
     * @param source the script source
     * @param identifier the script identifier (nullable)
     * @return the analyzed AST
     * @throws DmdlSyntaxException if the specified script is not a valid DMDL script
     * @throws IllegalArgumentException if some parameters were {@code null}
     */
    public AstScript parse(Reader source, URI identifier) throws DmdlSyntaxException {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null"); //$NON-NLS-1$
        }
        LOG.debug("Parsing DMDL: {}", identifier);
        JjDmdlParser parser = new JjDmdlParser(source);
        try {
            return parser.parse(identifier);
        } catch (ParseException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(
                        MessageFormat.format(
                                "Parse faild: uri={0}, token={1}, stack={2}",
                                identifier,
                                e.currentToken,
                                Arrays.toString(parser.getFrames())),
                        e);
            }
            throw new DmdlSyntaxException(e, parser);
        }
    }
}
