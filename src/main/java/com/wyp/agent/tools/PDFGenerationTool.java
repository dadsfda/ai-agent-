package com.wyp.agent.tools;

import cn.hutool.core.io.FileUtil;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.wyp.agent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * PDF 生成工具
 */
public class PDFGenerationTool {

    static final String PDF_READY_PREFIX = "PDF_READY:";

    private static final List<Path> CJK_FONT_CANDIDATES = List.of(
            Path.of("C:/Windows/Fonts/simhei.ttf"),
            Path.of("C:/Windows/Fonts/simsunb.ttf"),
            Path.of("C:/Windows/Fonts/Deng.ttf")
    );

    @Tool(description = "Generate a PDF file with given content", returnDirect = false)
    public String generatePDF(
            @ToolParam(description = "Name of the file to save the generated PDF") String fileName,
            @ToolParam(description = "Content to be included in the PDF") String content) {
        String fileDir = FileConstant.FILE_SAVE_DIR + "/pdf";
        String filePath = fileDir + "/" + fileName;
        try {
            FileUtil.mkdir(fileDir);
            try (PdfWriter writer = new PdfWriter(filePath);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {
                PdfFont font = createPdfFont();
                document.setFont(font);
                Paragraph paragraph = new Paragraph(content);
                document.add(paragraph);
            }
            return buildDownloadPayload(fileName);
        } catch (IOException e) {
            return "Error generating PDF: " + e.getMessage();
        }
    }

    String buildDownloadPayload(String fileName) {
        String encodedFileName = UriUtils.encodePathSegment(fileName, StandardCharsets.UTF_8);
        return PDF_READY_PREFIX + "{\"fileName\":\"" + escapeJson(fileName)
                + "\",\"downloadPath\":\"/files/pdf/" + encodedFileName + "\"}";
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private PdfFont createPdfFont() throws IOException {
        for (Path fontPath : CJK_FONT_CANDIDATES) {
            if (Files.exists(fontPath)) {
                return PdfFontFactory.createFont(
                        fontPath.toString(),
                        PdfEncodings.IDENTITY_H,
                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
                );
            }
        }
        return PdfFontFactory.createFont("Helvetica");
    }
}