package com.asakusafw.compiler.bulkloader.testing.io;
import com.asakusafw.compiler.bulkloader.testing.model.Ex1;
import com.asakusafw.runtime.io.ModelInput;
import com.asakusafw.runtime.io.RecordParser;
import java.io.IOException;
/**
 * TSVファイルなどのレコードを表すファイルを入力として<code>ex1</code>を読み出す
 */
public final class Ex1Input implements ModelInput<Ex1> {
    private final RecordParser parser;
    /**
     * インスタンスを生成する。
     * @param parser 利用するパーサー
     * @throws IllegalArgumentException 引数に<code>null</code>が指定された場合
     */
    public Ex1Input(RecordParser parser) {
        if(parser == null) {
            throw new IllegalArgumentException("parser");
        }
        this.parser = parser;
    }
    @Override public boolean readTo(Ex1 model) throws IOException {
        if(parser.next()== false) {
            return false;
        }
        parser.fill(model.getSidOption());
        parser.fill(model.getValueOption());
        parser.fill(model.getStringOption());
        return true;
    }
    @Override public void close() throws IOException {
        parser.close();
    }
}