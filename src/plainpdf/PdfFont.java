package plainpdf;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

/**
 * A list of fonts that can be used with TextToPdf.
 *
 * @author adam
 * @date April 15, 2013.
 */
public enum PdfFont {

    /** Helvetica-based fonts. */
    HELVETICA(PDType1Font.HELVETICA),
    HELVETICA_BOLD(PDType1Font.HELVETICA_BOLD),
    HELVETICA_ITALIC(PDType1Font.HELVETICA_OBLIQUE),
    HELVETICA_BOLD_ITALIC(PDType1Font.HELVETICA_BOLD_OBLIQUE),

    /** Courier-based fonts. */
    COURIER(PDType1Font.COURIER),
    COURIER_BOLD(PDType1Font.COURIER_BOLD),
    COURIER_ITALIC(PDType1Font.COURIER_OBLIQUE),
    COURIER_BOLD_ITALIC(PDType1Font.COURIER_BOLD_OBLIQUE),

    /** Times-based fonts. */
    TIMES(PDType1Font.TIMES_ROMAN),
    TIMES_BOLD(PDType1Font.TIMES_BOLD),
    TIMES_ITALIC(PDType1Font.TIMES_ITALIC),
    TIMES_BOLD_ITALIC(PDType1Font.TIMES_BOLD_ITALIC);
    
    /** The font value. */
    private final PDType1Font m_font;

    /**
     * Private constructor.
     * @param font the font used in this PdfFont.
     */
    private PdfFont(PDType1Font font){
        m_font = font;
    }

    /**
     * Returns the font used for this PdfFont.
     * @return the font used for this PdfFont.
     */
    PDType1Font getFont(){
        return m_font;
    }

}
