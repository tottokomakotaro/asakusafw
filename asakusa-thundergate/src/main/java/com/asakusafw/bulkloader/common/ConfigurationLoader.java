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
package com.asakusafw.bulkloader.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.asakusafw.bulkloader.exception.BulkLoaderSystemException;

/**
 * 設定を読み込んで保持するクラス。
 * <p>
 * 以下の設定を保持する。
 * </p>
<pre>
・プロパティファイル
・環境変数
・システムプロパティ
</pre>
 * @author yuta.shirai
 */
public final class ConfigurationLoader {
    /**
     * このクラス。
     */
    private static final Class<ConfigurationLoader> CLASS = ConfigurationLoader.class;

    /**
     * プロパティファイル。
     */
    private static volatile Properties prop = new Properties();
    /**
     * 環境変数。
     */
    private static volatile Map<String, String> env = null;
    /**
     * システムプロパティ。
     */
    private static volatile Properties sysProp = null;

    private ConfigurationLoader() {
        return;
    }
    static {
        // 環境変数を取得
        env = System.getenv();
        // システムプロパティを取得
        sysProp = System.getProperties();
    }
    /**
     * 読み込んでいるプロパティをクリアする。
     */
    public static void cleanProp() {
        prop = new Properties();
    }
    /**
     * 設定の読み込みを行う。
     * @param properties 読み込むプロパティファイルの一覧(絶対パスか「$ASAKUSA_HOME/bulkloader/conf/」のファイル名を指定する)
     * @param doDBPropCheck DBサーバ用プロパティのチェックを行うか
     * @param doHCPropCheck クライアントノード用プロパティのチェックを行うか
     * @throws BulkLoaderSystemException プロパティの中身が不正であった場合
     * @throws IOException ファイル読み込みに失敗した場合
     * @throws IllegalStateException 環境変数が未設定の場合
     */
    public static void init(
            List<String> properties,
            boolean doDBPropCheck,
            boolean doHCPropCheck) throws BulkLoaderSystemException, IOException {

        // 環境変数をチェック
        checkEnv();

        // プロパティを読み込んでチェック・デフォルト値を設定
        loadProperties(properties);
        checkAndSetParam();
        if (doDBPropCheck) {
            checkAndSetParamDB();
        }
        if (doHCPropCheck) {
            checkAndSetParamHC();
        }
    }
    /**
     * 環境変数をチェックする。
     * @throws IllegalStateException 環境変数が不正な場合
     */
    public static void checkEnv() {
        String variableName = Constants.ASAKUSA_HOME;
        checkDirectory(variableName);
        checkDirectory(Constants.THUNDER_GATE_HOME);
    }

    private static void checkDirectory(String variableName) {
        assert variableName != null;
        String variable = ConfigurationLoader.getEnvProperty(variableName);
        if (isEmpty(variable)) {
            System.err.println(MessageFormat.format("環境変数「{0}」が設定されていません。", variableName));
            throw new IllegalStateException(MessageFormat.format("環境変数「{0}」が設定されていません。", variableName));
        }
        File path = new File(variable);
        if (!path.exists()) {
            System.err.println(MessageFormat.format("環境変数「{0}」に設定されたディレクトリが存在しません。ディレクトリ：{1}", variableName, variable));
            throw new IllegalStateException(
                    MessageFormat.format("環境変数「{0}」に設定されたディレクトリが存在しません。ディレクトリ：{1}", variableName, variable));
        }
    }
    /**
     * HadoopClusterのプロパティの必須チェックとデフォルト値を設定する。
     * @throws BulkLoaderSystemException プロパティの中身が不正であった場合
     */
    protected static void checkAndSetParamHC() throws BulkLoaderSystemException {
        // デフォルト値の設定
        // Exportファイルの圧縮有無
        String strCompType = prop.getProperty(Constants.PROP_KEY_EXP_FILE_COMP_TYPE);
        FileCompType compType = FileCompType.find(strCompType);
        if (isEmpty(strCompType)) {
            prop.setProperty(Constants.PROP_KEY_EXP_FILE_COMP_TYPE, Constants.PROP_DEFAULT_EXP_FILE_COMP_TYPE);
        } else if (compType == null) {
            throw new BulkLoaderSystemException(
                    CLASS,
                    MessageIdConst.CMN_PROP_CHECK_ERROR,
                    "Exportファイルの圧縮有無が不正。値：" + null);
        }
        // エクスポート処理で中間TSVファイルを生成する際にTSVファイルの分割サイズ
        String loadMaxSize = prop.getProperty(Constants.PROP_KEY_EXP_LOAD_MAX_SIZE);
        if (isEmpty(loadMaxSize)) {
            prop.setProperty(
                    Constants.PROP_KEY_EXP_LOAD_MAX_SIZE,
                    Constants.PROP_DEFAULT_EXP_LOAD_MAX_SIZE);
        } else {
            if (!isNumber(loadMaxSize, 1)) {
                throw new BulkLoaderSystemException(
                        CLASS,
                        MessageIdConst.CMN_PROP_CHECK_ERROR,
                        "エクスポート処理中間TSVファイルを生成する際にTSVファイルを分割するサイズの設定が不正。設定値：" + loadMaxSize);
            }
        }
        // ワーキングディレクトリを使用するか
        if (isEmpty(prop.getProperty(Constants.PROP_KEY_WORKINGDIR_USE))) {
            prop.setProperty(
                    Constants.PROP_KEY_WORKINGDIR_USE,
                    Constants.PROP_DEFAULT_WORKINGDIR_USE);
        }
        // SequenceFileファイルの圧縮有無
        if (isEmpty(prop.getProperty(Constants.PROP_KEY_IMP_SEQ_FILE_COMP_TYPE))) {
            prop.setProperty(
                    Constants.PROP_KEY_IMP_SEQ_FILE_COMP_TYPE,
                    Constants.PROP_DEFAULT_IMP_SEQ_FILE_COMP_TYPE);
        }

        // 必須チェック
        // HDFSのプロトコルとホスト名
        if (isEmpty(prop.getProperty(Constants.PROP_KEY_HDFS_PROTCOL_HOST))) {
            throw new BulkLoaderSystemException(
                    CLASS,
                    MessageIdConst.CMN_PROP_CHECK_ERROR,
                    "HDFSのプロトコルとホスト名が設定されていない。");
        }
    }
    /**
     * DBサーバのプロパティの必須チェックとデフォルト値を設定する。
     * @throws BulkLoaderSystemException プロパティの中身が不正であった場合
     */
    protected static void checkAndSetParamDB() throws BulkLoaderSystemException {
        // デフォルト値の設定
        // Importファイルの圧縮有無
        String strCompType = prop.getProperty(Constants.PROP_KEY_IMP_FILE_COMP_TYPE);
        FileCompType compType = FileCompType.find(strCompType);
        if (isEmpty(strCompType)) {
            prop.setProperty(
                    Constants.PROP_KEY_IMP_FILE_COMP_TYPE,
                    Constants.PROP_DEFAULT_IMP_FILE_COMP_TYPE);
        } else if (compType == null) {
            throw new BulkLoaderSystemException(
                    CLASS,
                    MessageIdConst.CMN_PROP_CHECK_ERROR,
                    "Importファイルの圧縮有無が不正。値：" + null);
        }
        // Importファイルの圧縮時のバッファサイズ
        String impBufSize = prop.getProperty(Constants.PROP_KEY_IMP_FILE_COMP_BUFSIZE);
        if (isEmpty(impBufSize)) {
            prop.setProperty(
                    Constants.PROP_KEY_IMP_FILE_COMP_BUFSIZE,
                    Constants.PROP_DEFAULT_IMP_FILE_COMP_BUFSIZE);
        } else {
            if (!isNumber(impBufSize, 1)) {
                throw new BulkLoaderSystemException(
                        CLASS,
                        MessageIdConst.CMN_PROP_CHECK_ERROR,
                        "Importファイルの圧縮時のバッファサイズの設定が不正。設定値：" + impBufSize);
            }
        }
        // Importerのリトライ回数
        String impRetryCount = prop.getProperty(Constants.PROP_KEY_IMP_RETRY_COUNT);
        if (isEmpty(impRetryCount)) {
            prop.setProperty(
                    Constants.PROP_KEY_IMP_RETRY_COUNT,
                    Constants.PROP_DEFAULT_IMP_RETRY_COUNT);
        } else {
            if (!isNumber(impRetryCount, 0)) {
                throw new BulkLoaderSystemException(
                        CLASS,
                        MessageIdConst.CMN_PROP_CHECK_ERROR,
                        "Importerのリトライ回数の設定が不正。設定値：" + impRetryCount);
            }
        }
        // Importerのリトライインターバル
        String impRetryInterval = prop.getProperty(Constants.PROP_KEY_IMP_RETRY_INTERVAL);
        if (isEmpty(impRetryInterval)) {
            prop.setProperty(
                    Constants.PROP_KEY_IMP_RETRY_INTERVAL,
                    Constants.PROP_DEFAULT_IMP_RETRY_INTERVAL);
        } else {
            if (!isNumber(impRetryInterval, 0)) {
                throw new BulkLoaderSystemException(
                        CLASS,
                        MessageIdConst.CMN_PROP_CHECK_ERROR,
                        "Importerのリトライインターバルの設定が不正。設定値：" + impRetryInterval);
            }
        }
        // Exportファイルの圧縮時のバッファサイズ
        String expBufSize = prop.getProperty(Constants.PROP_KEY_EXP_FILE_COMP_BUFSIZE);
        if (isEmpty(expBufSize)) {
            prop.setProperty(
                    Constants.PROP_KEY_EXP_FILE_COMP_BUFSIZE,
                    Constants.PROP_DEFAULT_EXP_FILE_COMP_BUFSIZE);
        } else {
            if (!isNumber(expBufSize, 1)) {
                throw new BulkLoaderSystemException(
                        CLASS,
                        MessageIdConst.CMN_PROP_CHECK_ERROR,
                        "Exportファイルの圧縮時のバッファサイズの設定が不正。設定値：" + expBufSize);
            }
        }
        // Exporterのリトライ回数
        String expRetryCount = prop.getProperty(Constants.PROP_KEY_EXP_RETRY_COUNT);
        if (isEmpty(expRetryCount)) {
            prop.setProperty(
                    Constants.PROP_KEY_EXP_RETRY_COUNT,
                    Constants.PROP_DEFAULT_EXP_RETRY_COUNT);
        } else {
            if (!isNumber(expRetryCount, 0)) {
                throw new BulkLoaderSystemException(
                        CLASS,
                        MessageIdConst.CMN_PROP_CHECK_ERROR,
                        "Exporterのリトライ回数の設定が不正。設定値：" + expRetryCount);
            }
        }
        // Exporterのリトライインターバル
        String expRetryInterval = prop.getProperty(Constants.PROP_KEY_EXP_RETRY_INTERVAL);
        if (isEmpty(expRetryInterval)) {
            prop.setProperty(
                    Constants.PROP_KEY_EXP_RETRY_INTERVAL,
                    Constants.PROP_DEFAULT_EXP_RETRY_INTERVAL);
        } else {
            if (!isNumber(expRetryInterval, 0)) {
                throw new BulkLoaderSystemException(
                        CLASS,
                        MessageIdConst.CMN_PROP_CHECK_ERROR,
                        "Exporterのリトライインターバルの設定が不正。設定値：" + expRetryInterval);
            }
        }
        // エクスポート処理でExport対象テーブルにデータをコピーする時の最大レコード数
        String copyMaxRecord = prop.getProperty(Constants.PROP_KEY_EXP_COPY_MAX_RECORD);
        if (isEmpty(copyMaxRecord)) {
            prop.setProperty(
                    Constants.PROP_KEY_EXP_COPY_MAX_RECORD,
                    Constants.PROP_DEFAULT_EXP_COPY_MAX_RECORD);
        } else {
            if (!isNumber(copyMaxRecord, 1)) {
                throw new BulkLoaderSystemException(
                        CLASS,
                        MessageIdConst.CMN_PROP_CHECK_ERROR,
                        "エクスポート処理でExport対象テーブルにデータをコピーする時の最大レコード数の設定が不正。設定値：" + copyMaxRecord);
            }
        }
        // インポート正常終了時のTSVファイル削除有無
        String deleteImportTsv = prop.getProperty(Constants.PROP_KEY_IMPORT_TSV_DELETE);
        TsvDeleteType delImpType = TsvDeleteType.find(deleteImportTsv);
        if (isEmpty(deleteImportTsv)) {
            prop.setProperty(
                    Constants.PROP_KEY_IMPORT_TSV_DELETE,
                    Constants.PROP_DEFAULT_IMPORT_TSV_DELETE);
        } else if (delImpType == null) {
            throw new BulkLoaderSystemException(
                    CLASS,
                    MessageIdConst.CMN_PROP_CHECK_ERROR, "インポート正常終了時のTSVファイル削除有無が不正。値：" + deleteImportTsv);
        }
        // エクスポート正常終了時のTSVファイル削除有無
        String deleteExportTsv = prop.getProperty(Constants.PROP_KEY_EXPORT_TSV_DELETE);
        TsvDeleteType delExpType = TsvDeleteType.find(deleteExportTsv);
        if (isEmpty(deleteExportTsv)) {
            prop.setProperty(
                    Constants.PROP_KEY_EXPORT_TSV_DELETE,
                    Constants.PROP_DEFAULT_EXPORT_TSV_DELETE);
        } else if (delExpType == null) {
            throw new BulkLoaderSystemException(
                    CLASS,
                    MessageIdConst.CMN_PROP_CHECK_ERROR,
                    "エクスポート正常終了時のTSVファイル削除有無が不正。値：" + deleteExportTsv);
        }

        // 必須チェック
        // SSHのパス
        if (isEmpty(prop.getProperty(Constants.PROP_KEY_SSH_PATH))) {
            throw new BulkLoaderSystemException(
                    CLASS,
                    MessageIdConst.CMN_PROP_CHECK_ERROR,
                    "SSHのパスが設定されていません");
        }
        // HDFSのNameノードのIPアドレス又はホスト名
        if (isEmpty(prop.getProperty(Constants.PROP_KEY_NAMENODE_HOST))) {
            throw new BulkLoaderSystemException(
                    CLASS,
                    MessageIdConst.CMN_PROP_CHECK_ERROR,
                    "HDFSのNameノードのホスト名が設定されていません");
        }
        // HDFSのNameノードのユーザー名
        if (isEmpty(prop.getProperty(Constants.PROP_KEY_NAMENODE_USER))) {
            throw new BulkLoaderSystemException(
                    CLASS,
                    MessageIdConst.CMN_PROP_CHECK_ERROR,
                    "SSHのパスが設定されていません");
        }
        // Importファイルを置くディレクトリのトップディレクトリ
        if (isEmpty(prop.getProperty(Constants.PROP_KEY_IMP_FILE_DIR))) {
            throw new BulkLoaderSystemException(
                    CLASS,
                    MessageIdConst.CMN_PROP_CHECK_ERROR,
                    "Importファイルを置くディレクトリが設定されていません");
        }
        // Extractorのシェル名
        if (isEmpty(prop.getProperty(Constants.PROP_KEY_EXT_SHELL_NAME))) {
            throw new BulkLoaderSystemException(
                    CLASS,
                    MessageIdConst.CMN_PROP_CHECK_ERROR,
                    "Extractorのシェル名が設定されていません");
        }
        // エクスポートファイルを置くディレクトリのトップディレクトリ
        if (isEmpty(prop.getProperty(Constants.PROP_KEY_EXP_FILE_DIR))) {
            throw new BulkLoaderSystemException(
                    CLASS,
                    MessageIdConst.CMN_PROP_CHECK_ERROR,
                    "エクスポートファイルを置くディレクトリが設定されていません");
        }
        // Collectorのシェル名
        if (isEmpty(prop.getProperty(Constants.PROP_KEY_COL_SHELL_NAME))) {
            throw new BulkLoaderSystemException(
                    CLASS,
                    MessageIdConst.CMN_PROP_CHECK_ERROR, "Collectorのシェル名が設定されていません");
        }
    }
    /**
     * 共通のプロパティの必須チェックとデフォルト値を設定する。
     */
    protected static void checkAndSetParam() {
        // デフォルト値の設定
        // log4j.xmlのパス
        if (isEmpty(prop.getProperty(Constants.PROP_KEY_LOG_CONF_PATH))) {
            prop.setProperty(Constants.PROP_KEY_LOG_CONF_PATH, Constants.PROP_DEFAULT_LOG_CONF_PATH);
        }
    }
    /**
     * ファイルからプロパティを読み込む。
     * @param properties 読み込むプロパティファイル
     * @throws FileNotFoundException ファイルが存在しない場合
     * @throws IOException ファイルの読み込みに失敗した場合
     */
    private static void loadProperties(List<String> properties) throws IOException {
        assert properties != null;
        FileInputStream fis = null;
        for (String strProp : properties) {
            File propFile = createPropFileName(strProp);
            try {
                fis = new FileInputStream(propFile);
                prop.load(fis);
            } catch (IOException e) {
                System.err.println(
                        "プロパティファイルの読み込みに失敗しました。ファイル名："
                        + propFile.getAbsolutePath());
                e.printStackTrace();
                throw e;
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        // ここで例外が発生した場合は握りつぶす
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    /**
     * DBMSの接続情報を記述したプロパティファイルを読み込む。
     * @param targetName ターゲット名
     * @throws BulkLoaderSystemException 読み込みエラー
     */
    public static void loadJDBCProp(String targetName) throws BulkLoaderSystemException {
        // DBMSの接続情報を記述したプロパティファイルを読み込み
        String propName = targetName + Constants.JDBC_PROP_NAME;
        try {
            loadProperties(Arrays.asList(new String[]{propName}));
        } catch (IOException e) {
            throw new BulkLoaderSystemException(
                    e,
                    CLASS,
                    MessageIdConst.CMN_JDBCCONF_READ_ERROR,
                    propName);
        }

        // 必須チェック
        // JDBCドライバ
        if (isEmpty(prop.getProperty(Constants.PROP_KEY_JDBC_DRIVER))) {
            throw new BulkLoaderSystemException(
                    CLASS,
                    MessageIdConst.CMN_JDBCCONF_CHECK_ERROR,
                    "JDBCドライバが設定されていません");
        }
        // DB接続URL
        if (isEmpty(prop.getProperty(Constants.PROP_KEY_DB_URL))) {
            throw new BulkLoaderSystemException(
                    CLASS,
                    MessageIdConst.CMN_JDBCCONF_CHECK_ERROR,
                    "DB接続URLが設定されていません");
        }
        // DB接続ユーザー
        if (isEmpty(prop.getProperty(Constants.PROP_KEY_DB_USER))) {
            throw new BulkLoaderSystemException(
                    CLASS,
                    MessageIdConst.CMN_JDBCCONF_CHECK_ERROR,
                    "DB接続ユーザーが設定されていません");
        }
        // DB接続ユーザーに対するパスワード
        if (isEmpty(prop.getProperty(Constants.PROP_KEY_DB_PASSWORD))) {
            throw new BulkLoaderSystemException(
                    CLASS,
                    MessageIdConst.CMN_JDBCCONF_CHECK_ERROR,
                    "DB接続ユーザーに対するパスワードが設定されていません");
        }
    }

    /**
     * プロパティファイル名を作成する。
     * プロパティファイル名に絶対パスが指定されている場合は絶対パスのファイル名を使用し、
     * 相対パスが指定されている場合は「$ASAKUSA_HOME/bulkloader/conf/」以下のファイルを使用する
     * @param propFileName ファイル名
     * @return ファイル名のフルパス
     */
    private static File createPropFileName(String propFileName) {
        File tempFile = new File(propFileName);
        if (tempFile.isAbsolute()) {
            return tempFile;
        }
        String applHome = ConfigurationLoader.getEnvProperty(Constants.THUNDER_GATE_HOME);
        File file1 = new File(applHome, Constants.PROP_FILE_PATH);
        File file2 = new File(file1, propFileName);
        return file2;
    }
    /**
     * プロパティから特定の文字列から開始するKeyのリストを列挙する。
     * @param startString 開始文字列
     * @return 設定に含まれ、かつ指定の開始文字列を持つ設定キーの一覧
     */
    public static List<String> getPropStartWithString(String startString) {
        Set<Object> propSet = prop.keySet();
        List<String> list = new ArrayList<String>();

        for (Object o : propSet) {
            String key = (String) o;
            if (key.startsWith(startString)) {
                list.add(key);
            }
        }
        return list;
    }
    /**
     * 引数のkeyリストのうち、プロパティのvalueが空でないkeyを返す。
     * @param list keyのリスト
     * @return プロパティのvalueが空でないkeyのリスト
     */
    public static List<String> getExistValueList(List<String> list) {
        List<String> resultList = new ArrayList<String>();
        if (list == null || list.size() == 0) {
            return resultList;
        }
        int listSize = list.size();
        for (int i = 0; i < listSize; i++) {
            String key = list.get(i);
            String value = prop.getProperty(key);
            if (!isEmpty(value)) {
                resultList.add(key);
            }
        }
        return resultList;
    }
    /**
     * 引数の文字列が空かnullの場合trueを返す。
     * @param str 文字列
     * @return 結果
     */
    private static boolean isEmpty(String str) {
        if (str == null) {
            return true;
        }
        if (str.isEmpty()) {
            return true;
        }
        return false;
    }
    /**
     * 引数が数値である場合trueを返す。
     * @param str 文字列
     * @param min 数値の最小値(最小値より小さい場合NGとする)
     * @return 結果
     */
    private static boolean isNumber(String str, int min) {
        try {
            long parsed = Long.parseLong(str);
            return parsed >= min;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    /**
     * 指定のキーに対応するプロパティを返す。
     * @param key キー
     * @return String 対応するプロパティ、存在しない場合は{@code null}
     */
    public static String getProperty(String key) {
        return prop.getProperty(key);
    }

    /**
     * 指定のキーに対応するシステムプロパティ又は環境変数を返す。
     * システムプロパティに存在する場合はシステムプロパティの値を返し、
     * 存在しない場合は環境変数を検索する。
     * 何れにも存在しない場合はnullを返す。
     *
     * @param key キー
     * @return String 対応するシステムプロパティ又は環境変数、存在しない場合は{@code null}
     */
    public static String getEnvProperty(String key) {
        String strSysProp = sysProp.getProperty(key);
        String strEnv = env.get(key);
        if (strSysProp != null) {
            return strSysProp;
        } else if (strEnv != null) {
            return strEnv;
        } else {
            return null;
        }
    }

    /**
     * このクラスが提供するプロパティを設定する。
     * @param p 設定するプロパティ
     * @deprecated UT用
     */
    @Deprecated
    public static void setProperty(Properties p) {
        prop = p;
    }
    /**
     * このクラスが提供するプロパティ全体のビューを返す。
     * @return プロパティ全体のビュー
     * @deprecated UT用
     */
    @Deprecated
    public static Properties getProperty() {
        return prop;
    }
    /**
     * このクラスが提供するシステムプロパティを設定する。
     * @param p 設定するプロパティ
     * @deprecated UT用
     */
    @Deprecated
    public static void setSysProp(Properties p) {
        sysProp = p;
    }
    /**
     * このクラスが提供する環境変数を設定する。
     * @param m 設定する環境変数の表
     * @deprecated UT用
     */
    @Deprecated
    public static void setEnv(Map<String, String> m) {
        env = m;
    }

}
