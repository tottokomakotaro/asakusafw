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
package com.asakusafw.testdata.generator.excel;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.IndexedColors;

/**
 * Keeps cell styles.
 * @since 0.2.0
 */
public class WorkbookInfo {

    final HSSFWorkbook workbook;

    private final HSSFCellStyle commonStyle;

    final HSSFCellStyle titleStyle;

    final HSSFCellStyle lockedStyle;

    final HSSFCellStyle optionsStyle;

    final HSSFCellStyle dataStyle;

    final HSSFCellStyle dateDataStyle;

    final HSSFCellStyle timeDataStyle;

    final HSSFCellStyle datetimeDataStyle;

    /**
     * Creates a new instance.
     * @param workbook target workbook
     * @throws IllegalArgumentException if some parameters were {@code null}
     */
    public WorkbookInfo(HSSFWorkbook workbook) {
        if (workbook == null) {
            throw new IllegalArgumentException("workbook must not be null"); //$NON-NLS-1$
        }
        this.workbook = workbook;

        HSSFFont font = workbook.createFont();
        // font.setFontName("ＭＳ ゴシック");

        commonStyle = workbook.createCellStyle();
        commonStyle.setFont(font);
        commonStyle.setBorderTop(CellStyle.BORDER_THIN);
        commonStyle.setBorderBottom(CellStyle.BORDER_THIN);
        commonStyle.setBorderLeft(CellStyle.BORDER_THIN);
        commonStyle.setBorderRight(CellStyle.BORDER_THIN);

        titleStyle = workbook.createCellStyle();
        titleStyle.cloneStyleFrom(commonStyle);
        titleStyle.setLocked(true);
        titleStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
        titleStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        titleStyle.setAlignment(CellStyle.ALIGN_CENTER);

        lockedStyle = workbook.createCellStyle();
        lockedStyle.cloneStyleFrom(commonStyle);
        lockedStyle.setLocked(true);
        lockedStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
        lockedStyle.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
        lockedStyle.setAlignment(CellStyle.ALIGN_CENTER);

        optionsStyle = workbook.createCellStyle();
        optionsStyle.cloneStyleFrom(commonStyle);
        optionsStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
        optionsStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());

        CreationHelper helper = workbook.getCreationHelper();
        DataFormat df = helper.createDataFormat();

        dataStyle = workbook.createCellStyle();
        dataStyle.cloneStyleFrom(commonStyle);

        dateDataStyle = workbook.createCellStyle();
        dateDataStyle.cloneStyleFrom(commonStyle);
        dateDataStyle.setDataFormat(df.getFormat("yyyy-mm-dd"));

        timeDataStyle = workbook.createCellStyle();
        timeDataStyle.cloneStyleFrom(commonStyle);
        timeDataStyle.setDataFormat(df.getFormat("hh:mm:ss"));

        datetimeDataStyle = workbook.createCellStyle();
        datetimeDataStyle.cloneStyleFrom(commonStyle);
        datetimeDataStyle.setDataFormat(df.getFormat("yyyy-mm-dd hh:mm:ss"));
    }
}
