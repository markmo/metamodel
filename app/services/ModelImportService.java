package services;

import org.dmg.pmml.*;
import org.jpmml.model.JAXBUtil;

import javax.xml.bind.JAXBException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.file.Files;

/**
 * Created by markmo on 19/11/2014.
 */
public class ModelImportService {

    public static String importModel(String appName, String appDesc, String content) throws Exception {
        // appName = "R";
        // appDesc = "Revolution R";
        PMML pmml = readPMML(content);
        File tempFile = writeExtendedDataSourceFile(appName, appDesc, pmml);
        submitToIGC(tempFile);
        File tempFile2 = writeExtensionMappingDocument(pmml);
        submitExtendedMappingDocumentToIGC(tempFile2);
        System.out.println(new String(Files.readAllBytes(tempFile2.toPath())));
        return new String(Files.readAllBytes(tempFile.toPath()));
    }

    public static void deleteModel(String appName) {
        deleteFromIGC(appName);
    }

    private static PMML readPMML(String content) throws JAXBException {
        Source src = new StreamSource(new StringReader(content));
        return JAXBUtil.unmarshalPMML(src);
    }

    private static File writeExtendedDataSourceFile(String appName, String appDesc, PMML pmml) throws Exception {
        File tempFile = File.createTempFile("ExtendedDataSource", ".csv");
        PrintWriter writer = new PrintWriter(tempFile);

        writer.println("+++ Application - begin +++");
        writer.println("Name,Description");
        writer.println(String.format("%s,%s", appName, appDesc));
        writer.println("+++ Application - end +++");

        for (Model model : pmml.getModels()) {
            String objectType = model.getModelName();
            writer.println();
            writer.println("+++ Object Type - begin +++");
            writer.println("Name,Application,Description");
            writer.println(String.format("%s,%s,%s", objectType, appName, objectType));
            writer.println("+++ Object Type - end +++");

            String method = model.getFunctionName().value();
            writer.println();
            writer.println("+++ Method- begin +++");
            writer.println("Name,Application,ObjectType,Description");
            writer.println(String.format("%s,%s,%s,%s", method, appName, objectType, method));
            writer.println("+++ Method - end +++");

            writer.println();
            writer.println("+++ Input Parameter - begin +++");
            writer.println("Name,Application,ObjectType,Method,Description");
            for (MiningField field : model.getMiningSchema().getMiningFields()) {
                if (field.getUsageType().equals(FieldUsageType.ACTIVE)) {
                    writer.println(String.format("%s,%s,%s,%s,%s",
                            field.getName(),
                            appName,
                            objectType,
                            method,
                            field.getName()
                    ));
                }
            }
            writer.println("+++ Input Parameter - end +++");

            writer.println();
            writer.println("+++ Output Value - begin +++");
            writer.println("Name,Application,ObjectType,Method,Description");
            for (OutputField outputField : model.getOutput().getOutputFields()) {
                writer.println(String.format("%s,%s,%s,%s,%s",
                        outputField.getName().getValue(),
                        appName,
                        objectType,
                        method,
                        outputField.getFeature().value()
                ));
            }
            writer.println("+++ Output Value - end +++");
        }

        writer.close();

        return tempFile;
    }

    private static File writeExtensionMappingDocument(PMML pmml) throws Exception {
        File tempFile = File.createTempFile("ExtensionMappingDocument", ".csv");
        PrintWriter writer = new PrintWriter(tempFile);
        writer.println("Name,Source Columns,Target Columns,Rule,Function,Specification Description,Model Algorithm");
        for (Model model : pmml.getModels()) {
            for (MiningField field : model.getMiningSchema().getMiningFields()) {
                String fieldName = field.getName().getValue();
                writer.println(String.format("%s,%s,%s,%s,%s,%s,%s",
                        fieldName,
                        fieldName,
                        fieldName,
                        field.getUsageType(),
                        model.getFunctionName().value(),
                        "",
                        model.getModelName()
                ));
            }
        }

        writer.close();

        return tempFile;
    }

    private static void submitToIGC(File tempFile) {
        // must use the java command at /opt/IBM/InformationServer/jdk/jre/bin
        // not the system java otherwise:
        // ERROR: unable to connect to ISF server: 10.64.116.72
        //  Reason: SSL_TLSv2 SSLContext not available
        System.out.println("About to submit to IGC");
        System.out.println("From temp file: " + tempFile.getAbsolutePath());
        System.out.println("Run command:");
        String command = String.format("/opt/IBM/InformationServer/Clients/istools/cli/istool.sh workbench extension source import -domain 10.64.116.72:9445 -username isadmin -password Nbc_iis1234 -filename %s -output /tmp/import.log -overwrite", tempFile.getAbsolutePath());
        System.out.println(command);
        try {
            // TODO silently failing when using launcher jar
//            main(new String[]{
//                    "workbench", "extension", "source", "import",
//                    "-domain", "10.64.116.72:9445",
//                    //                "-domain", "119.81.7.66:9445",
//                    "-username", "isadmin", "-password", "Nbc_iis1234",
//                    "-filename", tempFile.getAbsolutePath(),
//                    "-output", "/tmp/import.log",
//                    "-overwrite"
//            });
            // Run shell script instead
            Process p = Runtime.getRuntime().exec(command);
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    private static void deleteFromIGC(String appName) {
        System.out.println("About to delete information asset from IGC");
        System.out.println("Run command:");
        String command = String.format("/opt/IBM/InformationServer/Clients/istools/cli/istool.sh workbench extension source delete -domain 10.64.116.72:9445 -username isadmin -password Nbc_iis1234 -pattern %s -type Application -output /tmp/delete.log", appName);
        System.out.println(command);
        try {
            Process p = Runtime.getRuntime().exec(command);
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    private static void submitExtendedMappingDocumentToIGC(File tempFile) {
        System.out.println("About to submit to IGC");
        System.out.println("From temp file: " + tempFile.getAbsolutePath());
        System.out.println("Run command:");
        String command = String.format("/opt/IBM/InformationServer/Clients/istools/cli/istool.sh workbench extension mapping import -domain 10.64.116.72:9445 -username isadmin -password Nbc_iis1234 -filename %s -output /tmp/extended_mapping_document_import.log -overwrite -description %s -srcprefix %s -trgprefix %s",
                tempFile.getAbsolutePath(),
                "Mapping of Teradata Source Columns to Model Parameters",
                "10.66.15.10.sysdba.LAB_ACP.v_Customer_Profile2",
                "R.General_Regression_Model.regression"
        );
        System.out.println(command);
        try {
            Process p = Runtime.getRuntime().exec(command);
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }
}
