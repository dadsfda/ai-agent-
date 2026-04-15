package com.yupi.yuaiagent.tools;

import com.itextpdf.kernel.pdf.PdfArray;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.yupi.yuaiagent.constant.FileConstant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

class PDFGenerationToolTest {

    @Test
    void generatePDFShouldEmbedReadableChineseFont() throws Exception {
        PDFGenerationTool tool = new PDFGenerationTool();
        String fileName = "pdf-generation-test.pdf";
        String content = "武汉一日游规划：黄鹤楼、东湖、户部巷";

        String result = tool.generatePDF(fileName, content);
        Path pdfPath = Path.of(FileConstant.FILE_SAVE_DIR, "pdf", fileName);

        Assertions.assertTrue(Files.exists(pdfPath), "PDF 文件应当被创建");
        Assertions.assertTrue(result.contains("PDF generated successfully"), "生成结果应提示成功，实际为: " + result);

        try (PdfDocument pdfDocument = new PdfDocument(new PdfReader(pdfPath.toString()))) {
            String extractedText = PdfTextExtractor.getTextFromPage(pdfDocument.getPage(1));
            Assertions.assertTrue(extractedText.contains("武汉一日游规划"), "PDF 不应为空白，实际提取内容为: " + extractedText);
            Assertions.assertTrue(hasEmbeddedFont(pdfDocument.getPage(1)), "PDF 应嵌入可渲染的字体文件，否则不同查看器可能显示为空白");
        }
    }

    private boolean hasEmbeddedFont(com.itextpdf.kernel.pdf.PdfPage page) {
        PdfDictionary fonts = page.getResources().getResource(PdfName.Font);
        if (fonts == null) {
            return false;
        }
        for (PdfName fontName : fonts.keySet()) {
            PdfDictionary fontDictionary = fonts.getAsDictionary(fontName);
            if (fontDictionary == null) {
                continue;
            }
            if (hasEmbeddedFontFile(fontDictionary)) {
                return true;
            }
            PdfArray descendantFonts = fontDictionary.getAsArray(PdfName.DescendantFonts);
            if (descendantFonts == null) {
                continue;
            }
            for (int i = 0; i < descendantFonts.size(); i++) {
                PdfDictionary descendantFont = descendantFonts.getAsDictionary(i);
                if (descendantFont != null && hasEmbeddedFontFile(descendantFont)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasEmbeddedFontFile(PdfDictionary fontDictionary) {
        PdfDictionary fontDescriptor = fontDictionary.getAsDictionary(PdfName.FontDescriptor);
        if (fontDescriptor == null) {
            return false;
        }
        return fontDescriptor.getAsStream(PdfName.FontFile) != null
                || fontDescriptor.getAsStream(PdfName.FontFile2) != null
                || fontDescriptor.getAsStream(PdfName.FontFile3) != null;
    }
}