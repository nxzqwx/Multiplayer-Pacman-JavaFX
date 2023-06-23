import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class XMLGenerator {
    public static void main(String[] args) {
        String path = "C:\\Users\\greda\\Desktop\\ISTE-121\\maymester\\group project\\gp1\\src";
        List<String> javaFiles = getJavaFiles(path);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();

            Element rootElement = doc.createElement("JavaFiles");
            doc.appendChild(rootElement);

            for (String javaFile : javaFiles) {
                Element fileElement = doc.createElement("File");
                rootElement.appendChild(fileElement);

                Element nameElement = doc.createElement("Name");
                nameElement.appendChild(doc.createTextNode(javaFile));
                fileElement.appendChild(nameElement);
            }

            // Output the XML to a file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("java_files.xml"));
            transformer.transform(source, result);

            System.out.println("XML file generated successfully!");
        } catch (ParserConfigurationException | TransformerException e) {
            e.printStackTrace();
        }
    }

    private static List<String> getJavaFiles(String path) {
        List<String> javaFiles = new ArrayList<>();

        File directory = new File(path);
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".java")) {
                    javaFiles.add(file.getName());
                }
            }
        }

        return javaFiles;
    }
}
