import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static final Logger LOGGER = LogManager.getLogger(Main.class);
    private static Map<String, String> lines = new TreeMap<>();
    private static List<String> stations = new ArrayList<>();
    private static List<String> connections = new ArrayList<>();
    private static JSONObject jObjMain = new JSONObject();

    public static void main(String[] args) {
        try {
            File file = new File("src/main/resources/metro.html");
            Document doc = Jsoup.parse(file, "UTF-8");
            Element table = doc.select("table").get(3);
            List<Element> rows = table.select("tr");

            rows.stream().skip(1).forEach(el -> {
                Elements tdTags = el.select("td");
                String lineNumber = tdTags.first().select("span.sortkey").first().text();
                String lineName = tdTags.first().select("span").attr("title");
                String stationName = tdTags.get(1).select("a").first().text();
                String lineColor = tdTags.first().attr("style");
                lineColor = lineColor.substring(lineColor.indexOf("#"), lineColor.indexOf("#") + 7);

                lines.put(lineNumber, lineName.concat("\t").concat(lineColor));
                stations.add(lineNumber.concat("\t").concat(stationName));
                connections.add(stationName.concat("  ").concat(tdTags.get(3).select("span").text()));

                //второй вариант присвоения цвета и пробная печать результата
//                Matcher matcher = Pattern.compile("#\\S{6}").matcher(lineColor);
//                matcher.find();
//                System.out.println(lineNumber + " - " + lineName + " - " + lineColor + " - " + stationName);
            });

            createJSONStations();
            createJSONLines();
            createJSONConnections();
            Files.write(Paths.get("src/main/resources/metro.json"), jObjMain.toJSONString().getBytes());

            //выводим на экран линии со списком станций
            printLinesFromJson();

        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private static void createJSONStations() {
        JSONObject jObj = new JSONObject();

        lines.keySet().forEach(o -> {               //номера линий
            JSONArray jArr = new JSONArray();

            stations.forEach(e -> {                 //добавляем станции в массив JSON, если № линий совпадают
                String[] tmp = e.split("\t");
                if (tmp[0].equals(o)) {
                    jArr.add(tmp[1]);
                }
            });
            jObj.put(o, jArr);
        });

        jObjMain.put("stations", jObj);
    }

    private static void createJSONLines() {
        JSONArray jArr = new JSONArray();

        lines.keySet().forEach(o -> {
            JSONObject jObjLine = new JSONObject();
            String[] tmp = lines.get(o).split("\t");

            if (tmp.length != 2) {
                LOGGER.error("Отсутствует имя или цвет");

            }else {
                jObjLine.put("number", o);
                jObjLine.put("name", tmp[0]);
                jObjLine.put("color", tmp[1].trim());
                jArr.add(jObjLine);
            }
        });

        jObjMain.put("lines", jArr);
    }

    private static void createJSONConnections() {
        JSONArray jArr = new JSONArray();

        connections.forEach(o -> {                  //все станции
            JSONArray tmpArr = new JSONArray();
            JSONObject tmpObj = new JSONObject();
            String[] tmp = o.split("  ");

            if (tmp.length > 1) {                   //далее станции с пересадками
                tmpObj.put("station", tmp[0]);
                JSONArray array = new JSONArray();

                Arrays.stream(tmp).skip(1).forEach(el -> array.add(el.trim()));

                tmpObj.put("connection_lines", array);
                tmpArr.add(tmpObj);
                jArr.add(tmpArr);
            }
        });

        jObjMain.put("connections", jArr);
    }

    private static String getJsonFile()
    {
        StringBuilder builder = new StringBuilder();
        try {
            List<String> lines = Files.readAllLines(Paths.get("src/main/resources/metro.json"));
            lines.forEach(line -> builder.append(line));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return builder.toString();
    }

    private static void printLinesFromJson() {
        try {
            Map<String, String> lineNumbersAndNames = new TreeMap<>();
            JSONParser parser = new JSONParser();
            JSONObject jsonData = (JSONObject) parser.parse(getJsonFile());
            JSONObject linesObj = (JSONObject) jsonData.get("stations");

            JSONArray linesArr = (JSONArray) jsonData.get("lines");

            for (int i = 0; i < linesArr.size(); i++) {
                JSONObject obj = (JSONObject) linesArr.get(i);
                String lineNumber;
                String lineName;

                lineNumber = obj.get("number").toString();
                lineName = obj.get("name").toString();
                lineNumbersAndNames.put(lineNumber, lineName);
            }

            linesObj.forEach((o, o2) -> {           //о - номер линии, о2 - станции
                JSONArray tmpArr = (JSONArray) o2;
                String lineName = "";

                for (String key : lineNumbersAndNames.keySet()) {
                    if (o.equals(key)) {
                        lineName = lineNumbersAndNames.get(key);
                    }
                }

                System.out.println(o + " - " + lineName + ". Всего станций: " + tmpArr.size());
            });

        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }
}
