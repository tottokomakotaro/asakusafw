==========================
ドキュメンテーションの構成
==========================

プロジェクトの構成
==================
ドキュメンテーションのプロジェクトを`<root>/docs`に配置する。

ルートディレクトリの構成
------------------------
プロジェクトのルートディレクトリは、以下のような構成とする

..  list-table:: プロジェクトディレクトリの構成
    :widths: 1 5
    :header-rows: 1

    * - パス 
      - 内容 
    * - source/
      - ドキュメントのソースファイルを格納するディレクトリ
    * - build/
      - ビルド結果を格納するディレクトリ (自動生成)
    * - README
      - プロジェクトについての解説
    * - LICENSE
      - ライセンス情報
    * - make.bat
      - Windows環境でドキュメントをビルドするバッチ
    * - Makefile
      - Unix系環境でドキュメントをビルドするmakeファイル

ソースディレクトリの構成
------------------------
プロジェクトのソースディレクトリは、以下のような構成とする。

..  list-table:: ソースディレクトリの構成
    :widths: 2 5
    :header-rows: 1

    * - パス 
      - 内容 
    * - _static
      - 静的ファイルを配置するディレクトリ
    * - conf.py
      - ドキュメントの構成設定情報
    * - index.rst
      - プロジェクトのマスタードキュメント
    * - `component`/index.rst
      - コンポーネントごとのマスタードキュメント

コンポーネント
==============
ソースディレクトリ以下にコンポーネントごとにディレクトリを作成し、関連するドキュメントを配置する。

..  list-table:: コンポーネントの例
    :widths: 1 4 10
    :header-rows: 1
    
    * - パス
      - コンポーネント
      - 内容
    * - ./
      - フレームワーク
      - 思想の話やインデックス
    * - application/
      - アプリケーション開発
      - バッチアプリケーションのビルド手順等
    * - dsl/
      - Asakusa DSL
      - 各種DSLおよびコンパイラ
    * - thundergate/
      - ThunderGate
      - ThunderGate
    * - testing/
      - Test Driver
      - テストドライバ
    * - dmdl/
      - DMDL
      - DMDLおよびDMDLコンパイラ
    * - documentation/
      - Documentation
      - ドキュメントの書き方等 (内部向け)

ドキュメントの形式
==================
ドキュメントは Sphinx_ でビルド可能な reStructuredText_ 形式で記述し、拡張子は `*.rst` とする。

..  _Sphinx : http://sphinx.pocoo.org/
..  _reStructuredText : http://docutils.sourceforge.net/rst.html

日本語
------

基本的には「ですます」で記述し、仕様書等は「だである」で記述する。

ドキュメントのターゲット
------------------------
以下のうち誰を対象とするかを想定すること。

* User (U): Asakusa Frameworkを利用してバッチアプリケーションを開発する人
* Administrator (A): Asakusa Frameworkを利用して開発されたバッチアプリケーションを運用する人
* Manager (M): Asakusa Frameworkを利用してバッチアプリケーションを開発させる人
* Developer (D): Asakusa Frameworkそのものを読んだり、拡張ポイントを利用して拡張したりする人
* Insider (I): Asakusa Frameworkそのものを開発する人

ドキュメントファイルの命名規則
------------------------------
ファイル名の規則は以下のとおり。

* 英語ドキュメントは `<ドキュメント名>.rst`
* 英語以外は `<ドキュメント名>_<言語名>.rst`
* ドキュメント名は小文字アルファベット、数字、ハイフンのみから構成
* 同じ内容で言語の異なるドキュメント名は一致させる

標準的なドキュメント名
----------------------
ありえそうなドキュメントの例。
下記に該当するドキュメントは、可能な限り名前をそろえる。下記に該当しないドキュメントは、命名規則の範囲で自由に名前をつけてよい。

..  list-table:: 標準的なドキュメント名
    :widths: 3 2 10
    :header-rows: 1

    * - ドキュメント名
      - 想定対象
      - 内容
    * - index
      - 全員
      - モジュールの概要や他のドキュメントの参照
    * - faq
      - 全員
      - FAQ的なもの
    * - start-guide
      - U, M
      - 最も簡素な方法でモジュールを利用する手順
    * - user-guide
      - U
      - バッチアプリケーション開発時に必要なモジュールの公開機能を可能な限り網羅したマニュアル
    * - developer-guide
      - D
      - Asakusa Frameworkの拡張ポイントを利用した拡張ガイド
    * - administrator-guide
      - U, A
      - 運用に関するマニュアル
    * - with-X
      - U, Aなど
      - 他のコンポーネントXとの連携方法
    * - [X-]specification
      - I
      - モジュール[のX (language, extensionなど)]に関連する設計書または仕様書

