package plainpdf;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;

/**
 * A class that represents a PDF file.  This class aids in assembling a PDF document from the agenda items.
 *
 * @author adam
 * @date Feb 3, 2013
 * Last Updated:  February 5, 2013.
 */
public class Pdf {

    /** The document. */
    private PDDocument m_doc;

    /** The current page that this Pdf is writing to. */
    private PDPage m_currentPage;

    /** The content stream for the current page. */
    private PDPageContentStream m_contentStream;

    /** The X-Coordinate of the location to write the next line of text. */
    private float m_currentX;

    /** The Y-Coordinate of the location to write the next line of text. */
    private float m_currentY;

    /** The maximum X-value for the page, ie its width. */
    private float m_pgWidth;

    /** The maximum Y value for the page, ie its height. */
    private float m_pgHeight;

    /** The width of the side margins. */
    private static final int SIDE_MARGIN = 70;

    /** The height of the Top and Bottom Margins. */
    private static final int TOP_BOTTOM_MARGINS = 80;


    /** Constructs a new, blank PDF document. */
    public Pdf(){
        try {
            // Initialize the Document
            m_doc = new PDDocument();
            m_currentPage = new PDPage();
            m_doc.addPage(m_currentPage);

            // Calculate the page height.
            m_pgWidth  = m_currentPage.getTrimBox().getWidth() - (2 * SIDE_MARGIN);
            m_pgHeight = m_currentPage.getTrimBox().getHeight();

            // Figure out the page position
            resetCurrentXandY();

            // Initialize a Content Stream for writing to the current page.
            m_contentStream = new PDPageContentStream(m_doc, m_currentPage);
            m_contentStream.beginText();
            m_contentStream.moveTextPositionByAmount(m_currentX, m_currentY);

        } catch (IOException ex) {
            Logger.getLogger(Pdf.class.getName()).log(Level.SEVERE, "Error initializing PDF document.", ex);
        }

    }

    /**
     * Renders a single line of text to the document.  If necessary this method will wrap the text of the line
     * repeatedly.
     *
     * @param line the String of text to render to the PDF.
     * @param font the font style to render this line in.
     * @param fontSize the size to render the font.
     *
     * @return true if the call resulted in a line being written to the file.
     * @throws IOException if an error occurs rendering the line to the pdf file.
     */
     public boolean renderLine(String line, PdfFont font, int fontSize) throws IOException {
        String toRender = line.trim();

        if (getStringWidth(toRender, font.getFont(), fontSize) > m_pgWidth){
            String[] words = line.split(" ");
            StringBuilder lineToRender = new StringBuilder();
            for (int word = 0; word < words.length; word++){
                if (getStringWidth(lineToRender.toString().trim(), font.getFont(), fontSize) < m_pgWidth){
                    // 
                    if (words[word].equals(words[words.length - 1])) {
                        writeLine(lineToRender.toString().trim(), font, fontSize);
                    } else {
                        lineToRender.append(words[word]);
                        lineToRender.append(" ");                        
                    }
                } else {
                    int index = lineToRender.toString().trim().lastIndexOf(" ");
                    String fittedLine = lineToRender.toString().substring(0, index);
                    writeLine(fittedLine.trim(), font, fontSize);
                    
                    // Recursively call this for the remaining text.
                    renderLine(line.substring(fittedLine.length() + 1), font, fontSize);
                    return true;
                }
             }
            
        } else {
            writeLine(toRender, font, fontSize);
            return true;
        }

        return false;
    }
    
     /**
      * Actually performs the work of writing the text to the PDF file.
      * 
      * @param text the text to write - It is assumed that this text fits on a line width-wise.
      * @param font the font to render the text in.
      * @param fontSize the size of the font to render the text with.
      * @param offset the vertical offset calculated for this font and size.
      * 
      * @throws IOException if an error occurs writing out the text to the PDF.
      */
    private void writeLine(String text, PdfFont font, int fontSize) throws IOException{
        float offset = getFontOffset(font.getFont(), fontSize);
        m_contentStream.setFont(font.getFont(), fontSize);
        m_contentStream.moveTextPositionByAmount(0, offset);
        m_contentStream.drawString(text);
        m_currentY = m_currentY + offset;
        checkPage();  
    }

    /**
     * Inserts a blank line of the height of the supplied font.
     *
     * @param font the font face to use.
     * @param fontSize the size (ie 12 for 12pt, or 32 for 32pt).
     * @throws IOException  
     */
    public void insertBlankLine(PdfFont font, int fontSize) throws IOException{
        float offset = getFontOffset(font.getFont(), fontSize);
        m_contentStream.moveTextPositionByAmount(0, offset);
        m_currentY = m_currentY + offset;
    }

    /**
     * Saves the file to disk.
     *
     * @param filename the name of the file that will be created by this save operation
     * .
     * @throws IOException if an error occurs during saving.
     * @throws COSVisitorException if an error occurs during saving.
     */
    public void saveAs(String filename) throws IOException, COSVisitorException{
        m_contentStream.endText();
        m_contentStream.close();
        m_doc.save(filename);
        m_doc.close();
    }

    
    /**
     * Returns the vertical offset distance (the height) of the given font.
     *
     * @param font the font to measure the offset for.
     * @param fontSize the size in the font, for example 12pt, 36pt.
     *
     * @return the vertical offset distance (the height) of the given font.
     */
    private float getFontOffset(PDFont font, int fontSize){
        return -1 * (font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000 * fontSize);
    }

    
    /**
     * Checks the current page position vs the bottom margin to see if its time to create a new page.
     * If a new page is required, a new one is initialized and all associated variables updated.
     */
    private void checkPage(){
        if (m_currentY < TOP_BOTTOM_MARGINS * 2){
            newPage();
        }
    }

    
    /**
     * Returns the width in page units of the string using the font and font size specified.
     *
     * @param str The string to measure the width of.
     * @param font the font style to use.
     * @param fontSize the size of the font, (ie 12pt, 16pt etc).
     *
     * @return The width in page units of the string using the font and font size specified.
     * @throws IOException if an error occurs calculating the width.
     */
    private float getStringWidth(String str, PDFont font, int fontSize) throws IOException{
        return (font.getStringWidth(str) / 1000 * fontSize);
    }

    
    /** Resets the current X, Y text positions to their default values (the upper left corner of the text area). */
    private void resetCurrentXandY(){
        m_currentX = SIDE_MARGIN;
        m_currentY = m_pgHeight - TOP_BOTTOM_MARGINS;
    }

    
    /** Initializes a new page for this PDF document. */
    private void newPage(){
        try {
            // Close out old content stream.
            m_contentStream.endText();
            m_contentStream.close();

            // Generate a new page.
            m_currentPage = new PDPage();
            m_doc.addPage(m_currentPage);
            resetCurrentXandY();

            // Create new stream.
            m_contentStream = new PDPageContentStream(m_doc, m_currentPage);
            m_contentStream.beginText();
            m_contentStream.moveTextPositionByAmount(m_currentX, m_currentY);

        } catch (IOException ex) {
            Logger.getLogger(Pdf.class.getName()).log(Level.SEVERE, "Error initializing content stream for new page.", ex);
        }
    }
    
    /**
     * Call this method to generate a PDF that describes how to use PlainPdf.
     * 
     * @throws IOException if an error occurs creating or writing the file.
     * @throws COSVisitorException  if an error occurs creating or writing the file.
     */
    public static void documentation() throws IOException, COSVisitorException {
        Pdf pdf = new Pdf();
        pdf.renderLine("How to use PlainPdf", PdfFont.HELVETICA, 24);
        pdf.insertBlankLine(PdfFont.HELVETICA, 12);
        pdf.renderLine("Using PlainPDF is very simple.  Simple instantiate a PDF object, give it some lines of text, "
                + "and save it.  PlainPdf handles line wrapping and appending additional pages as needed.  You don't "
                + "need to keep track of page sizes.  No calculating text widths in arcane units of measure.  No "
                + "dynamically handling page adds based on the pixel height of your text.  Just instantiate, render, "
                + "and save.",
                PdfFont.HELVETICA, 10);
        pdf.insertBlankLine(PdfFont.HELVETICA, 12);
        pdf.renderLine("That's it.", PdfFont.HELVETICA, 10);
        pdf.insertBlankLine(PdfFont.HELVETICA, 12);
        
        pdf.renderLine("// A Simple Example - Hello_World.pdf", PdfFont.COURIER_BOLD, 12);
        pdf.renderLine("Pdf pdf = new Pdf();", PdfFont.COURIER, 10);
        pdf.renderLine("pdf.renderLine(\"Hello, world!\", PdfFont.TIMES, 12);", PdfFont.COURIER, 10);
        pdf.renderLine("pdf.saveAs(\"Hello_World.pdf\");", PdfFont.COURIER, 10);
        pdf.insertBlankLine(PdfFont.COURIER, 12);
        
        pdf.renderLine("// A Simple Example", PdfFont.COURIER_BOLD, 12);
        pdf.renderLine("Pdf pdf = new Pdf();", PdfFont.COURIER, 10);
        pdf.renderLine("pdf.renderLine(\"My Life Story\", PdfFont.HELVETICA_BOLD, 24);", PdfFont.COURIER, 10);
        pdf.renderLine("pdf.insertBlankLine(PdfFont.HELVETICA, 12);", PdfFont.COURIER, 10);
        pdf.insertBlankLine(PdfFont.HELVETICA, 12);
        pdf.renderLine("pdf.renderLine(\"I was born in the 80s.\", PdfFont.HELVETICA, 12);", PdfFont.COURIER, 10);
        pdf.renderLine("pdf.renderLine(\"I grew up in the 90s.\", PdfFont.HELVETICA, 12);", PdfFont.COURIER, 10);
        pdf.renderLine("pdf.renderLine(\"I wrote a Java library called PlainPdf.\", PdfFont.HELVETICA, 12);", PdfFont.COURIER, 10);
        pdf.renderLine("pdf.renderLine(\"I am not yet dead.\", PdfFont.HELVETICA, 12);", PdfFont.COURIER, 10);
        pdf.insertBlankLine(PdfFont.HELVETICA, 12);
        pdf.renderLine("pdf.saveAs(\"my_life.pdf\");", PdfFont.COURIER, 10);
        
        pdf.insertBlankLine(PdfFont.HELVETICA, 12);
        pdf.insertBlankLine(PdfFont.HELVETICA, 12);
        
        pdf.renderLine("This document was created using PlainPdf.", PdfFont.HELVETICA, 10);
        pdf.renderLine("https://github.com/Horatio-Blackwood/PlainPdf", PdfFont.HELVETICA, 10);
        
        pdf.saveAs("quick-start.pdf");
    }
}