import java.lang.reflect.*;
import java.util.*;
import com.example.taskmanager.service.ProductService;
public class TestFallbackPdf {
  public static void main(String[] args) throws Exception {
    ProductService ps = new ProductService(null, null, null);
    Method m = ProductService.class.getDeclaredMethod("buildFallbackPdf", List.class);
    m.setAccessible(true);
    byte[] pdf = (byte[]) m.invoke(ps, List.of(Map.of("sku", "A","name","B","category","C","description","D","supplier","E")));
    System.out.println("len=" + pdf.length + " prefix=" + new String(pdf, 0, 8));
  }
}
