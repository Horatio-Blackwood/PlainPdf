package plainpdf;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

/**
 * A list of fonts that can be used with TextToPdf.
 *
 * @author adam
 * @date Feb 6, 2013.
 */
public enum PdfFont {

    HELVETICA(PDType1Font.HELVETICA),
    HELVETICA_BOLD(PDType1Font.HELVETICA_BOLD),
    HELVETICA_OBLIQUE(PDType1Font.HELVETICA_OBLIQUE),
    HELVETICA_BOLD_OBLIQUE(PDType1Font.HELVETICA_BOLD_OBLIQUE),

    COURIER(PDType1Font.COURIER),
    COURIER_BOLD(PDType1Font.COURIER_BOLD),
    COURIER_BOLD_OBLIQUE(PDType1Font.COURIER_BOLD_OBLIQUE),
    COURIER_OBLIQUE(PDType1Font.COURIER_OBLIQUE),

    TIMES(PDType1Font.TIMES_ROMAN),
    TIMES_BOLD(PDType1Font.TIMES_BOLD),
    TIMES_ITALIC(PDType1Font.TIMES_ITALIC),
    TIMES_BOLD_ITALIC(PDType1Font.TIMES_BOLD_ITALIC);


    private PDType1Font m_font;

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
