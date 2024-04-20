package utils;


import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.style.Styler;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileInputStream;
import java.io.IOException;
import org.knowm.xchart.*;
import java.awt.Color;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;




public class ModalShipComparison extends ListenerAdapter {
    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (event.getModalId().equals("shipcomparison-modal")) {
            event.deferReply(true).queue();
            String ourShipName = event.getValue("our-ship-name").getAsString();
            String enemyShipName = event.getValue("enemy-ship-name").getAsString();

            Ship ourShip = getShipDataFromExcel(ourShipName);
            Ship enemyShip = getShipDataFromExcel(enemyShipName);

            if (ourShip == null || enemyShip == null) {
                event.getHook().sendMessage("Bitte an Lori melden rawr").setEphemeral(true).queue();
                return;
            }
            String radarChartImagePath = generateRadarChart(ourShip, enemyShip);
            File file = new File(radarChartImagePath);

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("Ergebnisse");
            embed.setImage("attachment://" + radarChartImagePath);
            embed.setColor(Color.DARK_GRAY);

            FileUpload fileUpload = FileUpload.fromData(file,file.getName());

            event.getHook().sendFiles(Collections.singleton(fileUpload))
                 .addEmbeds(embed.build());
        }
    }
    private Ship getShipDataFromExcel(String shipName) {
        String filePath = "C:/Users/Lorenzo Giacomelli/Desktop/SC/BCRBot-master/src/main/resources/Database Ships.xlsx"; 
        try (FileInputStream file = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            Row headerRow = rowIterator.next();
            Map<String, Integer> columnIndex = new HashMap<>();
            for (Cell cell : headerRow) {
                columnIndex.put(cell.getStringCellValue(), cell.getColumnIndex());
            }
            
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (row.getCell(columnIndex.get("Name")).getStringCellValue().equalsIgnoreCase(shipName)) {
                    double hp = row.getCell(columnIndex.get("HP")).getNumericCellValue();
                    double maxspeed = row.getCell(columnIndex.get("Max Speed")).getNumericCellValue();
                    double hydrogenfuel = row.getCell(columnIndex.get("Hydrogen Fuel")).getNumericCellValue();
                    double ifcsDiff = row.getCell(columnIndex.get("IFCS Diff")).getNumericCellValue();
                    return new Ship(hp, maxspeed, hydrogenfuel, ifcsDiff);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    
    private String generateRadarChart(Ship ourShip, Ship enemyShip) {
        final String[] categories = {"HP", "Max Speed", "Hydrogen Fuel", "IFCS Diff"};
        final double[] dataOurShip = {ourShip.hp, ourShip.maxspeed, ourShip.hydrogenfuel, ourShip.ifcsDiff};
        final double[] dataEnemyShip = {enemyShip.hp, enemyShip.maxspeed, enemyShip.hydrogenfuel, enemyShip.ifcsDiff};
    
        RadarChart chart = new RadarChartBuilder()
                .width(600)
                .height(400)
                .title("Ship Comparison")
                .build();

        normalizeData(dataOurShip, dataEnemyShip);
        chart.setVariableLabels(categories);
        chart.addSeries("Our Ship", dataOurShip);
        chart.addSeries("Enemy Ship", dataEnemyShip);

        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);
        chart.getStyler().setHasAnnotations(true);
        chart.getStyler().setSeriesColors(new Color[]{new Color(0, 0, 255, 128), new Color(255, 0, 0, 128)});

        try {
            String outputPath = "C:/Users/Lorenzo Giacomelli/Desktop/SC/BCRBot-master/Fertig.png"; 
            BitmapEncoder.saveBitmap(chart, outputPath, BitmapEncoder.BitmapFormat.PNG);
            return outputPath;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void normalizeData(double[] dataOurShip, double[] dataEnemyShip) {
        double max = 0;
        for (int i = 0; i < dataOurShip.length; i++) {
            max = Math.max(max, dataOurShip[i]);
            max = Math.max(max, dataEnemyShip[i]);
        }
        for (int i = 0; i < dataOurShip.length; i++) {
            dataOurShip[i] /= max;
            dataEnemyShip[i] /= max;
        }
    }

    static class Ship{
        double hp, maxspeed, hydrogenfuel, ifcsDiff;
        public Ship(double hp, double maxspeed, double hydrogenfuel, double ifcsDiff){
            this.hp = hp;
            this.maxspeed = maxspeed;
            this.hydrogenfuel = hydrogenfuel;
            this.ifcsDiff = ifcsDiff;
        }

    }
}