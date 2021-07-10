package com.example.virtual_fridge;

import android.content.Context;

// Class used to convert between units and abbreviations of unit names
public class UnitsHelper {

    Context context;
    String[] unitsMetric;
    String[] unitsImperial;
    String[] unitsMetricAbbrev;
    String[] unitsImperialAbbrev;

    // constructor
    public UnitsHelper(Context context) {
        this.context = context;

        unitsMetric = context.getResources().getStringArray(R.array.units_array_metric);
        unitsMetricAbbrev = context.getResources().getStringArray(R.array.units_array_metric_abbrev);

        unitsImperial = context.getResources().getStringArray(R.array.units_array_imperial);
        unitsImperialAbbrev = context.getResources().getStringArray(R.array.units_array_imperial_abbrev);

    }

    // method to convert from full unit name, e.g. Ounces to abbreviation, e.g. oz
    public String convertToAbbreviation(boolean isImperial, String fullUnitName) {

        if (!isImperial) {
            for (int i = 0; i < unitsMetric.length; i++) {
                if (unitsMetric[i].toLowerCase().equals(fullUnitName.toLowerCase())) {
                    return unitsMetricAbbrev[i];
                }
            }
        } else {
            for (int i = 0; i < unitsImperial.length; i++) {
                if (unitsImperial[i].toLowerCase().equals(fullUnitName.toLowerCase())) {
                    return unitsImperialAbbrev[i];
                }
            }
        }
        return "";
    }

    // method converts abbreviated unit name, e.g. oz to full name, e.g. Ounces
    public String convertFromAbbreviation(boolean isImperial, String unitAbbrev) {

        if (!isImperial) {
            for (int i = 0; i < unitsMetricAbbrev.length; i++) {
                if (unitsMetricAbbrev[i].toLowerCase().equals(unitAbbrev.toLowerCase())) {
                    return unitsMetric[i];
                }
            }
        } else {
            for (int i = 0; i < unitsImperialAbbrev.length; i++) {
                if (unitsImperialAbbrev[i].toLowerCase().equals(unitAbbrev.toLowerCase())) {
                    return unitsImperial[i];
                }
            }
        }
        // edge case error?
        return "noUnit";
    }

    // method converts metric units to imperial
    public String[] toImperial(float quantity, String metricUnit) {

        try {
            switch (metricUnit.toLowerCase()) {
                case "kilograms":
                    // to pounds
                    return new String[]{String.valueOf(quantity * 2.2), "Pounds", "lbs"};
                case "grams":
                    // to ounces
                    return new String[]{String.valueOf(quantity / 28.4), "Ounces", "oz"};
                case "litres":
                    // to gallons
                    return new String[]{String.valueOf(quantity / 3.8), "Gallons", "gal"};
                case "millilitres":
                    // to fluid ounces
                    return new String[]{String.valueOf(quantity / 29.6), "Fluid Ounces", "floz"};
                case "centilitres":
                    // to cups
                    return new String[]{String.valueOf(quantity / 23.7), "Cups", "c"};
                default:
                    try {
                        return new String[]{String.valueOf(quantity), metricUnit, convertToAbbreviation(false, metricUnit)};
                    } catch (ArrayIndexOutOfBoundsException aiobe) {
                        return new String[]{String.valueOf(quantity), metricUnit, metricUnit};
                    }
            }
        } catch (NullPointerException npe) {
            return new String[]{String.valueOf(quantity), metricUnit, metricUnit};
        }
    }

    // method converts imperial units to metric
    public String[] toMetric(float quantity, String imperialUnit) {

        try {
            switch (imperialUnit.toLowerCase()) {
                case "pounds":
                    // to kilograms
                    return new String[]{String.valueOf(quantity / 2.2), "Kilograms", "kg"};
                case "ounces":
                    // to grams
                    return new String[]{String.valueOf(quantity * 28.4), "Grams", "g"};
                case "gallons":
                    // to litres
                    return new String[]{String.valueOf(quantity * 3.8), "Litres", "l"};
                case "fluid ounces":
                    // to millilitres
                    return new String[]{String.valueOf(quantity * 29.6), "Millilitres", "ml"};
                case "cups":
                    // to centilitres
                    return new String[]{String.valueOf(quantity * 23.7), "Centilitres", "cl"};
                default:
                    try {
                        return new String[]{String.valueOf(quantity), imperialUnit, convertToAbbreviation(true, imperialUnit)};
                    } catch (ArrayIndexOutOfBoundsException aiobe) {
                        return new String[]{String.valueOf(quantity), imperialUnit, imperialUnit};
                    }
            }
        } catch (NullPointerException ne) {
            return new String[]{String.valueOf(quantity), imperialUnit, imperialUnit};
        }
    }
}
