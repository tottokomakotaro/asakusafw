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
package test.modelgen.table.io;
import com.asakusafw.runtime.io.ModelInput;
import com.asakusafw.runtime.io.RecordParser;
import java.io.IOException;
import javax.annotation.Generated;
import test.modelgen.table.model.BalanceTran;
/**
 * TSVファイルなどのレコードを表すファイルを入力として{@link BalanceTran}を読み出す。
 */
@Generated("ModelInputEmitter:0.0.1")@SuppressWarnings("deprecation") public final class BalanceTranModelInput
        implements ModelInput<BalanceTran> {
    /**
     * 内部で利用するパーサー
     */
    private final RecordParser parser;
    /**
     * インスタンスを生成する。
     * @param parser 利用するパーサー
     * @throws IllegalArgumentException 引数にnullが指定された場合
     */
    public BalanceTranModelInput(RecordParser parser) {
        if(parser == null) {
            throw new IllegalArgumentException();
        }
        this.parser = parser;
    }
    @Override public boolean readTo(BalanceTran model) throws IOException {
        if(parser.next()== false) {
            return false;
        }
        parser.fill(model.getSidOption());
        parser.fill(model.getVersionNoOption());
        parser.fill(model.getRgstDatetimeOption());
        parser.fill(model.getUpdtDatetimeOption());
        parser.fill(model.getSellerCodeOption());
        parser.fill(model.getPreviousCutoffDateOption());
        parser.fill(model.getCutoffDateOption());
        parser.fill(model.getNextCutoffDateOption());
        parser.fill(model.getPayoutDateOption());
        parser.fill(model.getCarriedOption());
        parser.fill(model.getPurchaseOption());
        parser.fill(model.getRtnOption());
        parser.fill(model.getDiscountOption());
        parser.fill(model.getTaxOption());
        parser.fill(model.getPayableOption());
        parser.fill(model.getMutualOption());
        parser.fill(model.getReservesOption());
        parser.fill(model.getCancelOption());
        parser.fill(model.getPaymentOption());
        parser.fill(model.getNextPurchaseOption());
        parser.fill(model.getNextReturnOption());
        parser.fill(model.getNextDiscountOption());
        parser.fill(model.getNextTaxOption());
        parser.fill(model.getPaymentFlagOption());
        return true;
    }
    @Override public void close() throws IOException {
        parser.close();
    }
}