package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import services.GlossaryService;
import services.ModelImportService;
import views.html.index;

import java.util.List;

public class Application extends Controller {

    public static Result index() {
        return ok(index.render("Your new application is ready."));
    }

    public static Result importModel() {
        String content = request().body().asText();
//        System.out.println(content);
        String appName = "R";
        String appDesc = "Revolution R";
        String output;
        try {
            output = ModelImportService.importModel(appName, appDesc, content);
        } catch (Exception e) {
            e.printStackTrace();
            return badRequest();
        }
        return ok(output);
    }

    public static Result deleteModel(String appName) {
        ModelImportService.deleteModel(appName);
        return ok();
    }

    public static Result glossary() {
        try {
            return ok(makeString(GlossaryService.findAll(), '\n'));
        } catch (Exception e) {
            e.printStackTrace();
            return badRequest(e.getMessage());
        }
    }

    private static String makeString(List<String> list, char sep) {
        StringBuilder out = new StringBuilder();
        for (int i = 0, n = list.size(); i < n; i++) {
//            out.append(list.get(i)).append(sep);
            out.append(list.get(i));
            if (i < n - 1) {
                out.append(sep);
            }
        }
        return out.toString();
    }

}
