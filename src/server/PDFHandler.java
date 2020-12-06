package server;

import common.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;

public class PDFHandler {
    private PDDocument pdfFile;
    private File file;
    private FileUtils fileUtils;

    public PDFHandler(File file){
        this.file = file;
        this.fileUtils = new FileUtils();
    }

    /**
     * Load the pdf file
     */
    public void loadDocument(){
        try {
            this.pdfFile = new PDDocument();
            this.pdfFile = PDDocument.load(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add signatures and timestamp to a new page of the pdf file
     * @param signatures
     * @param timestampList
     */
    public void addSignature(List<String> signatures, List<Long> timestampList) {
        PDPage pdPage = new PDPage();
        pdfFile.addPage(pdPage);
        try {
            PDPageContentStream contentStream = new PDPageContentStream(pdfFile, pdPage);

            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
            contentStream.setLeading(14.5f);
            contentStream.newLineAtOffset(30, 725);

            contentStream.showText("Signatures:");
            contentStream.newLine();
            contentStream.newLine();

            Iterator<String> it1 = signatures.iterator();
            Iterator<Long> it2 = timestampList.iterator();
            while (it1.hasNext() && it2.hasNext()) {
                String ts = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss z").format(new Timestamp(it2.next()));
                String signatureLine = "Name: " + it1.next() + "         Date: " + ts;
                contentStream.showText(signatureLine);
                contentStream.newLine();
            }

            System.out.println("Content added");
            contentStream.endText();

            contentStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Save to file
     * @return
     */
    public File savePDFFile() {
        File signedFile = null;
        try {
            String name = file.getName();
            String substring = name.substring(0, name.lastIndexOf('.'));
            signedFile = new File(file.getParent(), substring + "_signed.pdf");
            pdfFile.save(signedFile);
            pdfFile.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return signedFile;
    }
}
