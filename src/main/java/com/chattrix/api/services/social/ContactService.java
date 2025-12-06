package com.chattrix.api.services.social;

import com.chattrix.api.entities.Contact;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BadRequestException;
import com.chattrix.api.exceptions.ResourceNotFoundException;
import com.chattrix.api.mappers.ContactMapper;
import com.chattrix.api.repositories.ContactRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.requests.AddContactRequest;
import com.chattrix.api.requests.UpdateContactRequest;
import com.chattrix.api.responses.ContactResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class ContactService {

    @Inject
    private ContactRepository contactRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private ContactMapper contactMapper;

    public List<ContactResponse> getContacts(Long userId) {
        List<Contact> contacts = contactRepository.findByUserId(userId);
        return contactMapper.toResponseList(contacts);
    }

    public List<ContactResponse> getFavoriteContacts(Long userId) {
        List<Contact> contacts = contactRepository.findFavoritesByUserId(userId);
        return contactMapper.toResponseList(contacts);
    }

    @Transactional
    public ContactResponse addContact(Long userId, AddContactRequest request) {
        if (request.contactUserId.equals(userId)) {
            throw new BadRequestException("Cannot add yourself as contact");
        }

        User contactUser = userRepository.findById(request.contactUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (contactRepository.existsByUserIdAndContactUserId(userId, request.contactUserId)) {
            throw new BadRequestException("Contact already exists");
        }

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Contact contact = new Contact();
        contact.setUser(currentUser);
        contact.setContactUser(contactUser);
        contact.setNickname(request.nickname);
        contactRepository.save(contact);

        return contactMapper.toResponse(contact);
    }

    @Transactional
    public ContactResponse updateContact(Long userId, Long contactId, UpdateContactRequest request) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));

        if (!contact.getUser().getId().equals(userId)) {
            throw new BadRequestException("You do not have access to this contact");
        }

        if (request.nickname != null) {
            contact.setNickname(request.nickname);
        }
        if (request.isFavorite != null) {
            contact.setFavorite(request.isFavorite);
        }

        contactRepository.save(contact);

        return contactMapper.toResponse(contact);
    }

    @Transactional
    public void deleteContact(Long userId, Long contactId) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));

        if (!contact.getUser().getId().equals(userId)) {
            throw new BadRequestException("You do not have access to this contact");
        }

        contactRepository.delete(contact);
    }
}

