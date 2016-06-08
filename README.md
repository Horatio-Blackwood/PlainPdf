PlainPdf
========
PlainPdf is the easiest, simplest tool to build text only PDF documents.  

PlainPdf a teeny little project made up of just two files, Pdf.java and PdfFont.java.  PlainPdf makes it extremely easy to generate a PDF file, without having to deal with all that nastiness of manually assembling the file.  PlainPdf will automatically determine when lines need to be wrapped and when a new page must be inserted based on the currently selected font and size.

You simply feed it text using the renderLine() method.  You can add any length of string and the library takes care of the rest.  If the line needs to be wrapped, it is wrapped.  If a second page is required, another page is added for you.

Are plain text documents simply not awesome enough for you?  Try PlainPdf.
