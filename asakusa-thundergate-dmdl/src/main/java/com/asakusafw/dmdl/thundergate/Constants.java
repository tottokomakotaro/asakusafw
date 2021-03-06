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

import java.nio.charset.Charset;

/**
 * 定数表。
 */
public final class Constants {

    /**
     * バージョン番号。
     */
    public static final String VERSION = "0.2.0";

    /**
     * ファイルを出力する際のエンコーディング。
     */
    public static final Charset OUTPUT_ENCODING = Charset.forName("UTF-8");

    /**
     * テーブルのソース名。
     */
    public static final String SOURCE_TABLE = "table";

    /**
     * ビューのソース名。
     */
    public static final String SOURCE_VIEW = "view";

    /**
     * JDBCの設定ファイル内に記述する、JDBC Driverのキー。
     */
    public static final String K_JDBC_DRIVER = "jdbc.driver";

    /**
     * JDBCの設定ファイル内に記述する、JDBC URLのキー。
     */
    public static final String K_JDBC_URL = "jdbc.url";

    /**
     * JDBCの設定ファイル内に記述する、ログインユーザー名のキー。
     */
    public static final String K_JDBC_USER = "jdbc.user";

    /**
     * JDBCの設定ファイル内に記述する、パスワードのキー。
     */
    public static final String K_JDBC_PASSWORD = "jdbc.password";

    /**
     * JDBC設定ファイル内に記述するデータベース名。
     */
    public static final String K_DATABASE_NAME = "database.name";

    /**
     * The name extension of DMDL (includes dot).
     */
    public static final String DMDL_LIKE_EXTENSION = ".dmdl";

    private Constants() {
        return;
    }
}
