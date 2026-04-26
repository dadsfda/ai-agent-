package com.wyp.agent.tools;

import com.itextpdf.kernel.pdf.PdfArray;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.wyp.agent.constant.FileConstant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

class PDFGenerationToolTest {

    @Test
    void generatePDFShouldEmbedReadableChineseFont() throws Exception {
        PDFGenerationTool tool = new PDFGenerationTool();
        String fileName = "pdf-generation-test.pdf";
        String content = "Wuhan one day tour";

        String result = tool.generatePDF(fileName, content);
        Path pdfPath = Path.of(FileConstant.FILE_SAVE_DIR, "pdf", fileName);

        Assertions.assertTrue(Files.exists(pdfPath), "PDF file should be created");
        Assertions.assertTrue(result.startsWith("PDF_READY:"), "Result should expose a downloadable payload: " + result);
        Assertions.assertTrue(result.contains("/files/pdf/pdf-generation-test.pdf"), "Result should contain the download path: " + result);

        try (PdfDocument pdfDocument = new PdfDocument(new PdfReader(pdfPath.toString()))) {
            String extractedText = PdfTextExtractor.getTextFromPage(pdfDocument.getPage(1));
            Assertions.assertTrue(extractedText.contains("Wuhan one day tour"), "PDF should contain readable text: " + extractedText);
            Assertions.assertTrue(hasEmbeddedFont(pdfDocument.getPage(1)), "PDF should embed a usable font file");
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
