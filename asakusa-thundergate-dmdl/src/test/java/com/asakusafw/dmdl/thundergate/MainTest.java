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
package com.asakusafw.dmdl.thundergate;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test for {@link Main}.
 */
public class MainTest {

    /**
     * Temporary folder.
     */
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /**
     * create configuration.
     * @throws Exception if test was failed
     */
    @Test
    public void simple() throws Exception {
        List<String> arguments = new ArrayList<String>();

        Properties jdbcProperties = new Properties();
        jdbcProperties.setProperty(Constants.K_JDBC_DRIVER, "com.asakusafw.Driver");
        jdbcProperties.setProperty(Constants.K_JDBC_URL, "asakusa:thundergate");
        jdbcProperties.setProperty(Constants.K_JDBC_USER, "asakusa");
        jdbcProperties.setProperty(Constants.K_JDBC_PASSWORD, "asakusapw");
        jdbcProperties.setProperty(Constants.K_DATABASE_NAME, "asakusadb");
        File jdbc = folder.newFile("jdbc.properties");
        FileOutputStream out = new FileOutputStream(jdbc);
        try {
            jdbcProperties.store(out, "testing");
        } finally {
            out.close();
        }

        File output = folder.newFolder("output").getCanonicalFile().getAbsoluteFile();

        Collections.addAll(arguments, "-jdbc", jdbc.getAbsolutePath());
        Collections.addAll(arguments, "-output", output.getAbsolutePath());
        Collections.addAll(arguments, "-encoding", "ASCII");
        Collections.addAll(arguments, "-includes", "ACCEPT|DENIED");
        Collections.addAll(arguments, "-excludes", "DENIED");

        Configuration conf = Main.loadConfigurationFromArguments(
                arguments.toArray(new String[arguments.size()]));

        assertThat(conf.getJdbcDriver(), is("com.asakusafw.Driver"));
        assertThat(conf.getJdbcUrl(), is("asakusa:thundergate"));
        assertThat(conf.getJdbcUser(), is("asakusa"));
        assertThat(conf.getJdbcPassword(), is("asakusapw"));
        assertThat(conf.getDatabaseName(), is("asakusadb"));
        assertThat(conf.getEncoding(), is(Charset.forName("ASCII")));
        assertThat(conf.getOutput(), is(output));
        assertThat(conf.getMatcher().acceptModel("ACCEPT"), is(true));
        assertThat(conf.getMatcher().acceptModel("DENIED"), is(false));
    }
}
