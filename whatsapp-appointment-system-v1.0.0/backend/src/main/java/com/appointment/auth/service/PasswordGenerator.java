package com.appointment.auth.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * UTILITY CLASS - For generating BCrypt password hashes
 * 
 * This is NOT used in the actual application authentication flow.
 * Use this to generate hashed passwords for:
 * - Database seed scripts
 * - Testing purposes
 * - Creating admin users manually
 * 
 * HOW TO USE:
 * 1. Change the rawPassword value below to your desired password
 * 2. Run this class: mvn exec:java -Dexec.mainClass="com.appointment.auth.service.PasswordGenerator"
 * 3. Copy the "Encoded:" hash from the output
 * 4. Use that hash in your database INSERT statements
 */
public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        
        // ⬇️ CHANGE THIS TO YOUR DESIRED PASSWORD ⬇️
        String rawPassword = "1234";
        
        String encodedPassword = encoder.encode(rawPassword);
        System.out.println("=====================================");
        System.out.println("Raw Password: " + rawPassword);
        System.out.println("Encoded Hash: " + encodedPassword);
        System.out.println("Verification: " + encoder.matches(rawPassword, encodedPassword));
        System.out.println("=====================================");
        System.out.println("\nCopy the hash above to use in SQL INSERT statements.");
    }
}
