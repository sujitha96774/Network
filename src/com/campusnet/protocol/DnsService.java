package com.campusnet.protocol;

import java.util.HashMap;
import java.util.Map;

public class DnsService {
    private final Map<String, String> records = new HashMap<>();

    public DnsService() {
        // Default Campus Domain Name records
        records.put("portal.campus.edu", "10.0.1.50");
        records.put("library.campus.edu", "10.0.1.51");
        records.put("dns.campus.edu", "10.0.1.10");
        records.put("cloud.remote.com", "198.51.100.10");
        records.put("google.com", "8.8.8.8");
    }

    public void addRecord(String domain, String ip) {
        records.put(domain.toLowerCase(), ip);
    }

    public String resolve(String domain) {
        if (domain == null) return null;
        return records.get(domain.toLowerCase());
    }

    public Map<String, String> getRecords() {
        return records;
    }
}
