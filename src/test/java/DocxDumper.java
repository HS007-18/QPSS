import org.apache.poi.xwpf.usermodel.*;
import java.io.FileInputStream;
import java.util.List;

public class DocxDumper {
    public static void main(String[] args) throws Exception {
        try (FileInputStream fis = new FileInputStream("C:\\Users\\DHARANI DHARAN V\\Downloads\\Input Format.docx");
             XWPFDocument doc = new XWPFDocument(fis)) {
            List<IBodyElement> bodyElements = doc.getBodyElements();
            for (IBodyElement element : bodyElements) {
                if (element instanceof XWPFParagraph) {
                    System.out.println("PARAGRAPH: " + ((XWPFParagraph) element).getText());
                } else if (element instanceof XWPFTable) {
                    System.out.println("TABLE:");
                    for (XWPFTableRow row : ((XWPFTable) element).getRows()) {
                        System.out.print("  ROW: ");
                        for (XWPFTableCell cell : row.getTableCells()) {
                            System.out.print("[" + cell.getText() + "] ");
                        }
                        System.out.println();
                    }
                }
            }
        }
    }
}
