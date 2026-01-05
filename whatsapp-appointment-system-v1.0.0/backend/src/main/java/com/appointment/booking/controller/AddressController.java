package com.appointment.booking.controller;

import com.appointment.booking.model.Address;
import com.appointment.booking.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
@Tag(name = "Address Management", description = "APIs for managing user addresses")
@CrossOrigin(origins = "http://localhost:3000")
public class AddressController {
    
    private final AddressService addressService;
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user addresses")
    public ResponseEntity<?> getUserAddresses(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(addressService.getUserAddresses(userId));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error fetching addresses: " + e.getMessage());
        }
    }

    @GetMapping("/{addressId}")
    @Operation(summary = "Get address by ID")
    public ResponseEntity<Address> getAddress(@PathVariable Long addressId) {
        return ResponseEntity.ok(addressService.getAddress(addressId));
    }
    
    @PostMapping("/user/{userId}")
    @Operation(summary = "Create address")
    public ResponseEntity<?> createAddress(@PathVariable Long userId, @RequestBody Address address) {
        try {
            return ResponseEntity.ok(addressService.createAddress(userId, address));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error creating address: " + e.getMessage());
        }
    }
    
    @PutMapping("/{addressId}")
    @Operation(summary = "Update address")
    public ResponseEntity<Address> updateAddress(@PathVariable Long addressId, @RequestBody Address address) {
        return ResponseEntity.ok(addressService.updateAddress(addressId, address));
    }
    
    @DeleteMapping("/{addressId}")
    @Operation(summary = "Delete address")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long addressId) {
        addressService.deleteAddress(addressId);
        return ResponseEntity.ok().build();
    }
}
