package plainpdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    /** A list to cache words removed when calculating line lengths for word wrap. */
    private List<String> m_tailWordCache = new ArrayList<>();


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
    public boolean renderLine(String line, PdfFont font, int fontSize) throws IOException{
        String toRender = line.trim();
        float offset = getFontOffset(font.getFont(), fontSize);

        if (getStringWidth(toRender, font.getFont(), fontSize) > m_pgWidth){
            int index = toRender.lastIndexOf(" ");
            String head = toRender.substring(0, index);
            m_tailWordCache.add(toRender.substring(index).trim());

            // Call render on the timmed header.  If this call results in a write, (ie if its short enough), then
            // call renderLine on the tail.
            if (renderLine(head, font, fontSize)){
                renderLine(getCachedTail(), font, fontSize);
            }
        } else {
            m_contentStream.setFont(font.getFont(), fontSize);
            m_contentStream.moveTextPositionByAmount(0, offset);
            m_contentStream.drawString(toRender);
            m_currentY = m_currentY + offset;
            checkPage();
            return true;
        }

        return false;
    }

    /**
     * Inserts a blank line of the height of the supplied font.
     *
     * @param font the font face to use.
     * @param fontSize the size (ie 12pt, 32pt).
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
    }

    /** Closes this file for modifications. */
    public void close() throws IOException{
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
     * A helper method that takes the words stored in the tail words cache and assembles them in the proper order as
     * the entire tail.
     *
     * @return the complete tail cache.
     */
    private String getCachedTail(){
        StringBuilder bldr = new StringBuilder();
        Collections.reverse(m_tailWordCache);
        for (String word : m_tailWordCache){
            bldr.append(word);
            bldr.append(" ");
        }
        m_tailWordCache.clear();

        return bldr.toString().trim();
    }

    public static void main(String[] blargs) throws IOException, COSVisitorException{
        Pdf pdf = new Pdf();

        pdf.renderLine("Here is the a line, 24pt.", PdfFont.COURIER, 24);
        pdf.renderLine("Here is the a line, 18pt.", PdfFont.COURIER_BOLD, 18);
        pdf.renderLine("Here is the a line, 16pt.", PdfFont.COURIER_BOLD_OBLIQUE, 16);
        pdf.renderLine("Here is the a line, 12pt.", PdfFont.COURIER_OBLIQUE, 12);

        pdf.renderLine("Here is the a line, 24pt.", PdfFont.HELVETICA, 24);
        pdf.renderLine("Here is the a line, 18pt.", PdfFont.HELVETICA_BOLD, 18);
        pdf.renderLine("Here is the a line, 16pt.", PdfFont.HELVETICA_BOLD_OBLIQUE, 16);
        pdf.renderLine("Here is the a line, 12pt.", PdfFont.HELVETICA_OBLIQUE, 12);


        pdf.renderLine("Here is the a line, 24pt.", PdfFont.TIMES, 24);
        pdf.renderLine("Here is the a line, 18pt.", PdfFont.TIMES_BOLD, 18);
        pdf.renderLine("Here is the a line, 16pt.", PdfFont.TIMES_BOLD_ITALIC, 16);
        pdf.renderLine("Here is the a line, 12pt.", PdfFont.TIMES_ITALIC, 12);

        pdf.saveAs("file.pdf");
        pdf.close();
    }
}