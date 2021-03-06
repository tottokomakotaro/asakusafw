package com.asakusafw.compiler.operator.io;
import com.asakusafw.compiler.operator.model.MockHoge;
import com.asakusafw.runtime.io.ModelInput;
import com.asakusafw.runtime.io.RecordParser;
import java.io.IOException;
/**
 * TSVファイルなどのレコードを表すファイルを入力として<code>mock_hoge</code>を読み出す
 */
public final class MockHogeInput implements ModelInput<MockHoge> {
    private final RecordParser parser;
    /**
     * インスタンスを生成する。
     * @param parser 利用するパーサー
     * @throws IllegalArgumentException 引数に<code>null</code>が指定された場合
     */
    public MockHogeInput(RecordParser parser) {
        if(parser == null) {
            throw new IllegalArgumentException("parser");
        }
        this.parser = parser;
    }
    @Override public boolean readTo(MockHoge model) throws IOException {
        if(parser.next()== false) {
            return false;
        }
        parser.fill(model.getValueOption());
        return true;
    }
    @Override public void close() throws IOException {
        parser.close();
    }
}