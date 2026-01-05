package com.appointment.booking.service;

import com.appointment.booking.model.Address;
import com.appointment.booking.repository.AddressRepository;
import com.appointment.auth.repository.UserRepository;
import com.appointment.auth.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {
    
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    
    public List<Address> getUserAddresses(Long userId) {
        return addressRepository.findByUserId(userId);
    }

    public Address getAddress(Long addressId) {
        return addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found with id: " + addressId));
    }
    
    @Transactional
    public Address createAddress(Long userId, Address address) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        address.setUserId(userId);
        
        // If this is the first address, make it primary
        List<Address> existing = addressRepository.findByUserId(userId);
        if (existing.isEmpty()) {
            address.setIsPrimary(true);
        } else if (Boolean.TRUE.equals(address.getIsPrimary())) {
            // If new address is primary, unset others
            existing.forEach(a -> {
                if (Boolean.TRUE.equals(a.getIsPrimary())) {
                    a.setIsPrimary(false);
                    addressRepository.save(a);
                }
            });
        }
        
        return addressRepository.save(address);
    }
    
    @Transactional
    public Address updateAddress(Long addressId, Address addressDetails) {
        Address address = getAddress(addressId);
        
        address.setAddressLine1(addressDetails.getAddressLine1());
        address.setAddressLine2(addressDetails.getAddressLine2());
        address.setCity(addressDetails.getCity());
        address.setState(addressDetails.getState());
        address.setPostalCode(addressDetails.getPostalCode());
        address.setCountry(addressDetails.getCountry());
        
        if (Boolean.TRUE.equals(addressDetails.getIsPrimary()) && !Boolean.TRUE.equals(address.getIsPrimary())) {
             // Unset others
             List<Address> existing = addressRepository.findByUserId(address.getUserId());
             existing.stream()
                     .filter(a -> !a.getId().equals(addressId) && Boolean.TRUE.equals(a.getIsPrimary()))
                     .forEach(a -> {
                         a.setIsPrimary(false);
                         addressRepository.save(a);
                     });
             address.setIsPrimary(true);
        } else if (addressDetails.getIsPrimary() != null) {
            address.setIsPrimary(addressDetails.getIsPrimary());
        }
        
        return addressRepository.save(address);
    }
    
    public void deleteAddress(Long addressId) {
        addressRepository.deleteById(addressId);
    }
}
