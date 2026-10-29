import java.io.*;
import java.util.*;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.*;
import org.springframework.core.io.ClassPathResource;

public class CheckReport {
  public static void main(String[] args) throws Exception {
    try {
      // 1. Cargar el recurso desde src/main/resources/
      ClassPathResource resource = new ClassPathResource("reports/inventory-report.jrxml");
      
      // 2. Leer bytes una sola vez
      byte[] templateBytes;
      try (InputStream is = resource.getInputStream()) {
        templateBytes = is.readAllBytes();
      }
      System.out.println("Template loaded: " + templateBytes.length);

      // 3. Pasar un nuevo stream a JasperCompileManager
      JasperReport report;
      try (InputStream byteArrayStream = new ByteArrayInputStream(templateBytes)) {
        report = JasperCompileManager.compileReport(byteArrayStream);
      }

      // 4. Llenar y exportar el reporte
      List<Map<String, ?>> rows = List.of(
          Map.of("sku", "A", "name", "B", "category", "C", "description", "D", "supplier", "E")
      );
      
      JasperPrint print = JasperFillManager.fillReport(report, new HashMap<>(), new JRMapCollectionDataSource(rows));
      byte[] pdf = JasperExportManager.exportReportToPdf(print);
      
      System.out.println("PDF bytes: " + pdf.length);

    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }
}