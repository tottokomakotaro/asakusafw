package com.asakusafw.compiler.bulkloader.testing.model;
import com.asakusafw.compiler.bulkloader.testing.io.SystemColumnsInput;
import com.asakusafw.compiler.bulkloader.testing.io.SystemColumnsOutput;
import com.asakusafw.runtime.model.DataModel;
import com.asakusafw.runtime.model.DataModelKind;
import com.asakusafw.runtime.model.ModelInputLocation;
import com.asakusafw.runtime.model.ModelOutputLocation;
import com.asakusafw.runtime.value.LongOption;
import com.asakusafw.vocabulary.bulkloader.ColumnOrder;
import com.asakusafw.vocabulary.bulkloader.OriginalName;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.apache.hadoop.io.Writable;
/**
 * system_columnsを表すデータモデルクラス。
 */
@ColumnOrder(value = {"SID"})@DataModelKind("DMDL")@ModelInputLocation(SystemColumnsInput.class)@ModelOutputLocation(
        SystemColumnsOutput.class)@OriginalName(value = "SYSTEM_COLUMNS") public class SystemColumns implements 
        DataModel<SystemColumns>, Writable {
    private final LongOption sid = new LongOption();
    @Override@SuppressWarnings("deprecation") public void reset() {
        this.sid.setNull();
    }
    @Override@SuppressWarnings("deprecation") public void copyFrom(SystemColumns other) {
        this.sid.copyFrom(other.sid);
    }
    /**
     * sidを返す。
     * @return sid
     * @throws NullPointerException sidの値が<code>null</code>である場合
     */
    public long getSid() {
        return this.sid.get();
    }
    /**
     * sidを設定する。
     * @param value 設定する値
     */
    @SuppressWarnings("deprecation") public void setSid(long value) {
        this.sid.modify(value);
    }
    /**
     * <code>null</code>を許すsidを返す。
     * @return sid
     */
    @OriginalName(value = "SID") public LongOption getSidOption() {
        return this.sid;
    }
    /**
     * sidを設定する。
     * @param option 設定する値、<code>null</code>の場合にはこのプロパティが<code>null</code>を表すようになる
     */
    @SuppressWarnings("deprecation") public void setSidOption(LongOption option) {
        this.sid.copyFrom(option);
    }
    @Override public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("{");
        result.append("class=system_columns");
        result.append(", sid=");
        result.append(this.sid);
        result.append("}");
        return result.toString();
    }
    @Override public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + sid.hashCode();
        return result;
    }
    @Override public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if(obj == null) {
            return false;
        }
        if(this.getClass()!= obj.getClass()) {
            return false;
        }
        SystemColumns other = (SystemColumns) obj;
        if(this.sid.equals(other.sid)== false) {
            return false;
        }
        return true;
    }
    @Override public void write(DataOutput out) throws IOException {
        sid.write(out);
    }
    @Override public void readFields(DataInput in) throws IOException {
        sid.readFields(in);
    }
}