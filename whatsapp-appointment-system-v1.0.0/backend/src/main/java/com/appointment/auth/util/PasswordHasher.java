package com.appointment.auth.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class to generate BCrypt password hashes
 * Run this as a main method to generate password hashes for database updates
 */
public class PasswordHasher {
    
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        if (args.length == 0) {
            System.out.println("Usage: java PasswordHasher <password>");
            System.out.println("Example: java PasswordHasher admin");
            System.out.println("\nGenerating hash for 'admin':");
            String hash = encoder.encode("admin");
            System.out.println("Password: admin");
            System.out.println("BCrypt Hash: " + hash);
            System.out.println("\nGenerating hash for 'admin123':");
            String hash2 = encoder.encode("admin123");
            System.out.println("Password: admin123");
            System.out.println("BCrypt Hash: " + hash2);
        } else {
            String password = args[0];
            String hash = encoder.encode(password);
            System.out.println("Password: " + password);
            System.out.println("BCrypt Hash: " + hash);
            System.out.println("\nSQL Update Command:");
            System.out.println("UPDATE users SET password = '" + hash + "' WHERE username = 'admin';");
        }
    }
}

