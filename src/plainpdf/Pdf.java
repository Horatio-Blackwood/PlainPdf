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
 * Last Updated:  April 15, 2013.
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

    /** The default font size to use if nothing else is specified. */
    private int m_defaultFontSize;

    /** The default font to use if no other font is specified. */
    private PdfFont m_defaultFont;

    /** Constructs a new, blank PDF document with a default font of plain Helvetica and a default font size of 12. */
    public Pdf(){
        this(PdfFont.HELVETICA, 12);
    }

    /**
     * Constructs a new, blank PDF document with the supplied default font and size.
     *
     * @param defaultFont the default font to use when rendering text, unless another font is specified.  Must not be
     * null.
     * @param defaultFontSize the default font size to use when rendering text, unless another font size is specified.  If
     * this value is less than or equal to 0, an IllegalArgumentException is thrown.
     *
     * @throws IllegalArgumentException if the defaultFont parameter is null or if the parameter 'size' is less than or
     * equal to zero.
     */
    public Pdf(PdfFont defaultFont, int defaultFontSize){
        if (defaultFont == null){
            throw new IllegalArgumentException("Parameter 'defaultFont' cannot be null.");
        }
        if (defaultFontSize <= 0){
            throw new IllegalArgumentException("Parameter 'size' must not be less than or equal to zero.");
        }
        m_defaultFont = defaultFont;
        m_defaultFontSize = defaultFontSize;

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
     * Draws the supplied line of text to this PDF document.  If necessary, this method call will wrap the text and add
     * additional pages depending on the length of the line of text added.
     *
     * @param line the text to render to the file.
     *
     * @throws IOException if an error occurs writing the text to the PDF.
     */
    public void renderLine(String line) throws IOException{
        renderLine(line, m_defaultFont, m_defaultFontSize);
    }

    /**
     * Draws the supplied line of text to this PDF document.  If necessary, this method call will wrap the text and add
     * additional pages depending on the length of the line of text added.
     *
     * @param line the text to render to the file.
     * @param fontSize the font size to render the text, (ie 12 point, 14 point etc).
     *
     * @throws IOException if an error occurs writing the text to the PDF.
     */
    public void renderLine(String line, int fontSize) throws IOException{
        renderLine(line, m_defaultFont, fontSize);
    }

    /**
     * Renders a single line of text to the document.  If necessary this method will wrap the text of the line
     * repeatedly.
     *
     * @param line the String of text to render to the PDF.
     * @param font the font style to render this line in.
     * @param fontSize the font size to render the text, (ie 12 point, 14 point etc).
     *
     * @throws IOException if an error occurs rendering the line to the pdf file.
     * @throws IllegalArgumentException if the provided fontSize is less than or equal to zero.
     */
     public void renderLine(String line, PdfFont font, int fontSize) throws IOException {
        if (fontSize <= 0){
            throw new IllegalArgumentException("Parameter 'fontSize' must not be less than or equal to zero.");
        }
        String toRender = line.trim();
        if (getStringWidth(toRender, font.getFont(), fontSize) > m_pgWidth){
            String[] words = line.split(" ");
            StringBuilder lineToRender = new StringBuilder();

            render:
            for (int word = 0; word < words.length; word++){
                if (getStringWidth(lineToRender.toString().trim(), font.getFont(), fontSize) < m_pgWidth){
                    if (words[word].equals(words[words.length - 1])) {
                        writeLine(lineToRender.toString().trim(), font, fontSize);
                        break render;
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
                    break render;
                }
             }

        } else {
            writeLine(toRender, font, fontSize);
        }            
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
     * Sets the default font for this PDF document.
     * @param font the font to set as the new default for this document.
     *
     * @throws NullPointerException if the supplied font is null.
     */
    public void setDefaultFont(PdfFont font){
        if (font == null){
            throw new NullPointerException("Cannot set default font to null.");
        }
        m_defaultFont = font;
    }


    /**
     * Sets the default font size for this PDF document.
     * @param size the size to set as the new default for this document.
     *
     * @throws NullPointerException if the supplied font size is null.
     */
    public void setDefaultFontSize(int size){
        if (size <= 0){
            throw new IllegalArgumentException("Parameter 'size' must not be less than or equal to zero.");
        }
        m_defaultFontSize = size;
    }


    /**
     * Inserts a blank line based on the size of the default font.
     * @throws IOException if an error occurs inserting a blank line to the PDF.
     */
    public void insertBlankLine() throws IOException{
        insertBlankLine(m_defaultFont, m_defaultFontSize);
    }

    /**
     * Inserts a blank line based on the size of the default font.
     * @param size the size of this font.
     * @throws IOException if an error occurs inserting a blank line to the PDF.
     */
    public void insertBlankLine(int size) throws IOException{
        if (size <= 0){
            throw new IllegalArgumentException("Parameter 'size' must not be less than or equal to zero.");
        }
        insertBlankLine(m_defaultFont, size);
    }

    /**
     * Inserts a blank line of the height of the supplied font.
     *
     * @param font the font face to use.
     * @param fontSize the size (ie 12 for 12pt, or 32 for 32pt).
     *
     * @throws IOException if an error occurs inserting a blank line to the PDF.
     */
    public void insertBlankLine(PdfFont font, int fontSize) throws IOException{
        if (fontSize <= 0){
            throw new IllegalArgumentException("Parameter 'fontSize' must not be less than or equal to zero.");
        }
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
        Pdf pdf = new Pdf(PdfFont.HELVETICA, 10);
        pdf.renderLine("How to use PlainPdf", PdfFont.HELVETICA, 24);
        pdf.insertBlankLine(12);
        pdf.renderLine("Using PlainPDF is very simple.  Simply instantiate a PDF object, give it some lines of text, "
                + "and save it.  PlainPdf handles line wrapping and appending additional pages as needed.  You don't "
                + "need to keep track of page sizes.  No calculating text widths in arcane units of measure.  No "
                + "dynamically handling page adds based on the pixel height of your text.  Just instantiate, render, "
                + "and save.");
        pdf.insertBlankLine(12);
        pdf.renderLine("That's it.");
        pdf.insertBlankLine(12);

        pdf.renderLine("// A Simple Example - Hello_World.pdf", PdfFont.COURIER_BOLD, 12);
        pdf.setDefaultFont(PdfFont.COURIER);
        pdf.renderLine("Pdf pdf = new Pdf(PdfFont.TIMES, 12);");
        pdf.renderLine("pdf.renderLine(\"Hello, world!\");");
        pdf.renderLine("pdf.saveAs(\"Hello_World.pdf\");");
        pdf.insertBlankLine(12);

        pdf.renderLine("// A Simple Example", PdfFont.COURIER_BOLD, 12);
        pdf.renderLine("Pdf pdf = new Pdf(PdfFont.HELVETICA, 12);");
        pdf.renderLine("pdf.renderLine(\"My Life Story\", PdfFont.HELVETICA_BOLD, 24);");
        pdf.renderLine("pdf.insertBlankLine();");
        pdf.insertBlankLine(12);
        pdf.renderLine("pdf.renderLine(\"I was born in the 80s.\");");
        pdf.renderLine("pdf.renderLine(\"I grew up in the 90s.\");");
        pdf.renderLine("pdf.renderLine(\"I wrote a Java library called PlainPdf.\");");
        pdf.renderLine("pdf.renderLine(\"I am not yet dead.\");");
        pdf.insertBlankLine(12);
        pdf.renderLine("pdf.saveAs(\"my_life.pdf\");");

        pdf.insertBlankLine(12);
        pdf.insertBlankLine(12);

        pdf.setDefaultFont(PdfFont.HELVETICA);
        pdf.renderLine("This document was created using PlainPdf.");
        pdf.renderLine("https://github.com/Horatio-Blackwood/PlainPdf");

        pdf.saveAs("quick-start.pdf");
    }
}
