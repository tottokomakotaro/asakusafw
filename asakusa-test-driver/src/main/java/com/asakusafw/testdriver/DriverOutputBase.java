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

import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.vocabulary.external.ExporterDescription;

/**
 * テストドライバのテスト出力データの親クラス。
 * @since 0.2.0
 * 
 * @param <T> モデルクラス
 */
public class DriverOutputBase<T> extends DriverInputBase<T> {

    private static final Logger LOG = LoggerFactory.getLogger(DriverOutputBase.class);

    /** 期待値URI */
    protected URI expectedUri;
    /** 検証ルールURI */
    protected URI verifyRuleUri;
    /** エクスポータ記述 */
    protected ExporterDescription exporterDescription;

    /**
     * @return the expectedUri
     */
    protected URI getExpectedUri() {
        return expectedUri;
    }

    /**
     * @param expectedUri the expectedUri to set
     */
    protected void setExpectedUri(URI expectedUri) {
        this.expectedUri = expectedUri;
    }

    /**
     * @return the verifyRuleUri
     */
    protected URI getVerifyRuleUri() {
        return verifyRuleUri;
    }

    /**
     * @param verifyRuleUri the verifyRuleUri to set
     */
    protected void setVerifyRuleUri(URI verifyRuleUri) {
        this.verifyRuleUri = verifyRuleUri;
    }

    /**
     * @return the exporterDescription
     */
    protected ExporterDescription getExporterDescription() {
        return exporterDescription;
    }

    /**
     * @param exporterDescription the exporterDescription to set
     */
    protected void setExporterDescription(ExporterDescription exporterDescription) {
        this.exporterDescription = exporterDescription;
    }

    /**
     * set expected URI from expected path.
     * 
     * @param expectedPath expected path.
     */
    protected void setExpectedUri(String expectedPath) {

        try {
            expectedUri = toUri(expectedPath);
            LOG.info("Expected URI:" + expectedUri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("invalid expected URI. expectedPath:" + expectedPath, e);
        }
    }

    /**
     * set verifyRule URI from verifyRule path.
     * 
     * @param verifyRulePath expected path.
     */
    protected void setVerifyRuleUri(String verifyRulePath) {

        try {
            verifyRuleUri = toUri(verifyRulePath);
            LOG.info("Verify Rule URI:" + verifyRuleUri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("invalid verifyRule URI. verifyRulePath:" + verifyRulePath, e);
        }
    }

}