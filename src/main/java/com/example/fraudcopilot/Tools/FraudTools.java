package com.example.fraudcopilot.Tools;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.example.fraudcopilot.model.Transaction;
import com.example.fraudcopilot.model.TravelCheck;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;

import com.example.fraudcopilot.model.CustomerAnalysis;
import com.example.fraudcopilot.model.TransactionLeg;

@Component
public class FraudTools {

    // Synthetic data standing in for a real transactions database
    private final List<Transaction> transactions = List.of(
            new Transaction("T1", "C1001", 30.00, "Cafe Coffee Day",
                    "Hyderabad", LocalDateTime.now().minusMinutes(130)),
            new Transaction("T2", "C1001", 4800.00, "Luxury Watches DXB",
                    "Dubai", LocalDateTime.now().minusMinutes(10)),
            new Transaction("T3", "C2002", 55.00, "Grocery Mart",
                    "Mumbai", LocalDateTime.now().minusMinutes(60)));

    // city -> {latitude, longitude}
    private final Map<String, double[]> cities = Map.of(
            "Hyderabad", new double[] {17.3850, 78.4867},
            "Dubai", new double[] {25.2048, 55.2708},
            "Mumbai", new double[] {19.0760, 72.8777},
            "London", new double[] {51.5074, -0.1278});

    // The description is how the model decides when to use it
    // @Tool = The LLM's doesnot access to run java code . It wants Assistant to get Access from the Methods which is Tool . it tells the LLMs you can use this method 
    @Tool(description = "Get the recent card transactions for a customer, "
            + "including amount, merchant, city and time. "
            + "Use this whenever you need transaction facts.")
    
    public List<Transaction> getRecentTransactions(
    		//ToolParam= explains the argument. “for example C1001” teaches the model the expected ID format.
            @ToolParam(description = "Customer id, for example C1001") String customerId) {
        return transactions.stream()
                .filter(t -> t.customerId().equals(customerId))
                .toList();
    }

    @Tool(description = "Check whether a customer could physically travel between "
            + "two cities in the given number of minutes. "
            + "Use this only for hypothetical questions about two named cities.")
    public TravelCheck checkTravelFeasibility(
            @ToolParam(description = "City of the earlier transaction") String fromCity,
            @ToolParam(description = "City of the later transaction") String toCity,
            @ToolParam(description = "Minutes between the two transactions") int minutesBetween) {
        return travel(fromCity, toCity, minutesBetween);
    }

    private TravelCheck travel(String fromCity, String toCity, long minutes) {
        double[] a = cities.get(fromCity);
        double[] b = cities.get(toCity);
        if (a == null || b == null || minutes <= 0) {
            throw new IllegalArgumentException("Unknown city or invalid time");
        }
        double km = haversineKm(a[0], a[1], b[0], b[1]);
        double speed = km / (minutes / 60.0);
        return new TravelCheck(fromCity, toCity,
                Math.round(km * 10) / 10.0,
                Math.round(speed * 10) / 10.0,
                speed <= 900);
    }
    
    @Tool(description = "Analyze a customer's recent card activity in one step. "
            + "Returns their transactions in time order, the minutes between "
            + "consecutive transactions in different cities, a travel feasibility "
            + "verdict for each city change, and fraud flags. "
            + "Use this FIRST for any request to investigate or assess a customer.")
    public CustomerAnalysis analyzeCustomerActivity(
            @ToolParam(description = "Customer id, for example C1001") String customerId) {

        List<Transaction> txs = transactions.stream()
                .filter(t -> t.customerId().equals(customerId))
                .sorted(Comparator.comparing(Transaction::time))
                .toList();

        List<TransactionLeg> legs = new ArrayList<>();
        List<String> flags = new ArrayList<>();

        if (txs.isEmpty()) {
            flags.add("NO_TRANSACTIONS_FOUND");
            return new CustomerAnalysis(customerId, 0, 0.0, txs, legs, flags);
        }

        for (int i = 1; i < txs.size(); i++) {
            Transaction prev = txs.get(i - 1);
            Transaction cur = txs.get(i);
            if (prev.city().equals(cur.city())) {
                continue;
            }
            if (!cities.containsKey(prev.city()) || !cities.containsKey(cur.city())) {
                flags.add("UNKNOWN_CITY_ON_" + cur.id());
                continue;
            }
            long minutes = Math.max(1,
                    Duration.between(prev.time(), cur.time()).toMinutes());
            TravelCheck check = travel(prev.city(), cur.city(), minutes);
            legs.add(new TransactionLeg(prev.id(), cur.id(), minutes, check));
            if (!check.physicallyPossible()) {
                flags.add("IMPOSSIBLE_TRAVEL_" + prev.id() + "_TO_" + cur.id());
            }
        }

        double total = Math.round(txs.stream()
                .mapToDouble(Transaction::amount).sum() * 100) / 100.0;
        if (txs.stream().anyMatch(t -> t.amount() >= 1000)) {
            flags.add("HIGH_VALUE_TRANSACTION");
        }
        if (total > 5000) {
            flags.add("EXPOSURE_ABOVE_5000_ESCALATE");
        }
        return new CustomerAnalysis(customerId, txs.size(), total, txs, legs, flags);
    }

    // The haversine formula computes the great-circle distance between two points on a sphere (the shortest path over the Earth’s surface) from their latitudes and longitudes.
    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371;
        // Math.toRadians(...) converts degrees to radians, because Java’s trig functions need radians.
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.asin(Math.sqrt(h));
    }
    
    
}