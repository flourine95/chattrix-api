package com.chattrix.api.services.social;

import com.chattrix.api.entities.Contact;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BusinessException;
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
            throw BusinessException.badRequest("Cannot add yourself as contact", "BAD_REQUEST");
        }

        User contactUser = userRepository.findById(request.contactUserId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "RESOURCE_NOT_FOUND"));

        if (contactRepository.existsByUserIdAndContactUserId(userId, request.contactUserId)) {
            throw BusinessException.badRequest("Contact already exists", "BAD_REQUEST");
        }

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "RESOURCE_NOT_FOUND"));

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
                .orElseThrow(() -> BusinessException.notFound("Contact not found", "RESOURCE_NOT_FOUND"));

        if (!contact.getUser().getId().equals(userId)) {
            throw BusinessException.badRequest("You do not have access to this contact", "BAD_REQUEST");
        }

        if (request.nickname != null) {
            contact.setNickname(request.nickname);
        }
        if (request.favorite != null) {
            contact.setFavorite(request.favorite);
        }

        contactRepository.save(contact);

        return contactMapper.toResponse(contact);
    }

    @Transactional
    public void deleteContact(Long userId, Long contactId) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> BusinessException.notFound("Contact not found", "RESOURCE_NOT_FOUND"));

        if (!contact.getUser().getId().equals(userId)) {
            throw BusinessException.badRequest("You do not have access to this contact", "BAD_REQUEST");
        }

        contactRepository.delete(contact);
    }
}
