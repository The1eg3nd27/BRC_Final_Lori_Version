package event;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.RadarChart;
import org.knowm.xchart.RadarChartBuilder;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.IOException;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ShipComparison extends ListenerAdapter{
    static class Ship{
        double hp, maxspeed, hydrogenfuel, ifcsDiff;
        public Ship(double hp, double maxspeed, double hydrogenfuel, double ifcsDiff){
            this.hp = hp;
            this.maxspeed = maxspeed;
            this.hydrogenfuel = hydrogenfuel;
            this.ifcsDiff = ifcsDiff;
        }
    }
    
    public static void main (String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Geb dein Schiff ein: ");
        String ourShipName = scanner.nextLine();
        System.out.print("Geb den Namen des gegnerischen Schiff: ");
        String enemyShipName = scanner.nextLine();
        try {
            FileInputStream file = new FileInputStream(new File("C:/Users/Lorenzo Giacomelli/Desktop/SC/BCRBot-master/src/main/resources/Database Ships.xlsx"));
            Workbook workbook = new XSSFWorkbook(file);
            Sheet sheet = workbook.getSheetAt(0);

            Map<String, Integer> columnIndex = extractColumnIndices(sheet.getRow(0));
            
            Ship ourShip = findShipByName(sheet, ourShipName, columnIndex);
            Ship enemyShip = findShipByName(sheet, enemyShipName, columnIndex);
            
            workbook.close();
            file.close();

            if (ourShip != null && enemyShip != null){
                compareShips(ourShip, enemyShip);
                createRadarChart(ourShip, enemyShip);
            }

            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Map<String, Integer>extractColumnIndices(Row headerRow){
        Map<String,Integer> columnIndex = new HashMap<>();
        for (Cell cell : headerRow){
            columnIndex.put(cell.getStringCellValue(), cell.getColumnIndex());
        }
        return columnIndex;
    }

    private static Ship findShipByName(Sheet sheet, String shipName, Map<String, Integer>columnIndex ){
        for (int r =1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row != null) {
                String currentShipName = row.getCell(0).getStringCellValue().trim();
                if (shipName.equals(currentShipName)) {
                    return extractShipData(row, columnIndex);
                }
            }
        }
        return null;
    }

    private static double getNumericCellValue(Cell cell){
        if (cell == null) {
            throw new IllegalArgumentException("Cell nichts ");
        }
        try {
            if (cell.getCellType() == CellType.STRING){
                String value = cell.getStringCellValue();
                try {
                    return Double.parseDouble(value);
                } catch(NumberFormatException e) {
                    throw new IllegalArgumentException("Will numeric finden aber finde was anderes :/ : " + value + " hier " + cell.getAddress().formatAsString());
                }
            } else if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getNumericCellValue();
            } else {
                throw new IllegalArgumentException("der typ geht nicht lul: " + cell.getCellType() + " hier " + cell.getAddress().formatAsString());
            }
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("kann nicht lesen " + cell.getAddress().formatAsString() + "wegen dem hier: " + e.getMessage());
        }
    }




    private static Ship extractShipData(Row row, Map<String, Integer > columnIndex) {
        double hp = getNumericCellValue(row.getCell(columnIndex.get("HP")));
        double maxspeed = getNumericCellValue(row.getCell(columnIndex.get("Max speed")));
        double hydrogenfuel = getNumericCellValue(row.getCell(columnIndex.get("Hydrogen capacity")));
        double pitchmax = getNumericCellValue(row.getCell(columnIndex.get("IFCS pitch max")));
        double yawmax = getNumericCellValue(row.getCell(columnIndex.get("IFCS yaw max")));
        double rollmax = getNumericCellValue(row.getCell(columnIndex.get("IFCS roll max")));

        double ifcsDiff = Math.abs((pitchmax + yawmax + rollmax) / 3);
        return new Ship(hp, maxspeed, hydrogenfuel, ifcsDiff);
    }




    private static void compareShips(Ship ourShip, Ship enemyShip) {
        //System.out.print("Comparison HP: " + (ourShip.hp > enemyShip.hp ? 1 : 0));
        //System.out.print("Comparison max speed: "+ (ourShip.maxspeed > enemyShip.maxspeed ? 1:0));
        //System.out.print("Comparison hydrogenfuel: " + (ourShip.hydrogenfuel > enemyShip.hydrogenfuel ? 1:0));
        //System.out.print("Comparison ifcsDiff: " + (ourShip.ifcsDiff > enemyShip.ifcsDiff ? 1:0));
        System.out.print("info: " + ourShip.hp + " " + ourShip.maxspeed + " " + ourShip.hydrogenfuel + " " + ourShip.ifcsDiff +" " + enemyShip.ifcsDiff + " " + enemyShip.hp + " " + enemyShip.maxspeed + " " + enemyShip.hydrogenfuel);
        
    }

    private static void createRadarChart(Ship ourShip, Ship enemyShip) {

        final double AVG_HP = 40924.70;
        final double AVG_MAX_SPEED = 1112.57;
        final double AVG_HYDROGEN_FUEL = 1189956.12;
        final double AVG_IFCS = 65;

        double[] dataOurShip = {
            ourShip.hp / AVG_HP,
            ourShip.maxspeed / AVG_MAX_SPEED,
            ourShip.hydrogenfuel / AVG_HYDROGEN_FUEL,
            ourShip.ifcsDiff / AVG_IFCS
        };

        double[] dataEnemyShip = {
            enemyShip.hp / AVG_HP,
            enemyShip.maxspeed / AVG_MAX_SPEED,
            enemyShip.hydrogenfuel / AVG_HYDROGEN_FUEL,
            enemyShip.ifcsDiff / AVG_IFCS
        };

        double maxNormalizedValue = Math.max(
            Arrays.stream(dataOurShip).max().orElse(1),
            Arrays.stream(dataEnemyShip).max().orElse(1)
        );

        for (int i =0; i <dataOurShip.length;i++){
            dataOurShip[i]=dataOurShip[i] / maxNormalizedValue;
            dataEnemyShip[i]=dataEnemyShip[i] / maxNormalizedValue;

        }

        
        RadarChart chart = new RadarChartBuilder()
            .width(600)
            .height(400)
            .title("Bin der beste")
            .build();

        
        chart.setVariableLabels(new String[]{"HP", "Max Speed", "Hydrogen Fuel", "IFCS Diff"});
        Color ourShipColor = new Color (0, 0, 255, 128);
        Color enemyShipColor = new Color (255, 0, 0, 128);

        chart.addSeries("Our Ship", dataOurShip).setFillColor(ourShipColor);
        chart.addSeries("Enemy Ship", dataEnemyShip).setFillColor(enemyShipColor);

        chart.getStyler().setToolTipsEnabled(false);
        chart.getStyler().setHasAnnotations(true);
        chart.getStyler().setSeriesColors(new Color[]{ourShipColor, enemyShipColor});


        try {
            
            String outputPath = "C:/Users/Lorenzo Giacomelli/Desktop/SC/Fertig.png";
            BitmapEncoder.saveBitmap(chart, outputPath, BitmapEncoder.BitmapFormat.PNG);
            System.out.println("Radar chart saved to " + outputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
                
    }
    

}