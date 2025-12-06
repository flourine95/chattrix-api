package com.chattrix.api.services.social;

import com.chattrix.api.entities.Contact;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BadRequestException;
import com.chattrix.api.exceptions.ResourceNotFoundException;
import com.chattrix.api.repositories.ContactRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.requests.SendFriendRequestRequest;
import com.chattrix.api.responses.FriendRequestResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class FriendRequestService {

    @Inject
    private ContactRepository contactRepository;

    @Inject
    private UserRepository userRepository;

    @Transactional
    public FriendRequestResponse sendFriendRequest(Long senderId, SendFriendRequestRequest request) {
        if (senderId.equals(request.receiverUserId)) {
            throw new BadRequestException("Cannot send friend request to yourself");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

        User receiver = userRepository.findById(request.receiverUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));

        Optional<Contact> existingContact = contactRepository.findByUserIdAndContactUserId(senderId, request.receiverUserId);
        if (existingContact.isPresent()) {
            Contact contact = existingContact.get();
            if (contact.getStatus() == Contact.ContactStatus.PENDING) {
                throw new BadRequestException("Friend request already sent");
            } else if (contact.getStatus() == Contact.ContactStatus.ACCEPTED) {
                throw new BadRequestException("Already friends");
            } else if (contact.getStatus() == Contact.ContactStatus.BLOCKED) {
                throw new BadRequestException("Cannot send friend request to blocked user");
            }
        }

        Contact contact = new Contact();
        contact.setUser(sender);
        contact.setContactUser(receiver);
        contact.setStatus(Contact.ContactStatus.PENDING);
        contact.setNickname(request.nickname);
        contact.setRequestedAt(Instant.now());

        contactRepository.save(contact);

        return mapToFriendRequestResponse(contact, receiver);
    }

    @Transactional
    public FriendRequestResponse acceptFriendRequest(Long userId, Long requestId) {
        Contact request = contactRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));

        if (!request.getContactUser().getId().equals(userId)) {
            throw new BadRequestException("You are not the receiver of this request");
        }

        if (request.getStatus() != Contact.ContactStatus.PENDING) {
            throw new BadRequestException("Friend request is not pending");
        }

        request.setStatus(Contact.ContactStatus.ACCEPTED);
        request.setAcceptedAt(Instant.now());
        contactRepository.save(request);

        Contact reverseContact = new Contact();
        reverseContact.setUser(request.getContactUser());
        reverseContact.setContactUser(request.getUser());
        reverseContact.setStatus(Contact.ContactStatus.ACCEPTED);
        reverseContact.setAcceptedAt(Instant.now());
        contactRepository.save(reverseContact);

        return mapToFriendRequestResponse(request, request.getUser());
    }

    @Transactional
    public void rejectFriendRequest(Long userId, Long requestId) {
        Contact request = contactRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));

        if (!request.getContactUser().getId().equals(userId)) {
            throw new BadRequestException("You are not the receiver of this request");
        }

        if (request.getStatus() != Contact.ContactStatus.PENDING) {
            throw new BadRequestException("Friend request is not pending");
        }

        request.setStatus(Contact.ContactStatus.REJECTED);
        request.setRejectedAt(Instant.now());
        contactRepository.save(request);
    }

    @Transactional
    public void cancelFriendRequest(Long userId, Long requestId) {
        Contact request = contactRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));

        if (!request.getUser().getId().equals(userId)) {
            throw new BadRequestException("You are not the sender of this request");
        }

        if (request.getStatus() != Contact.ContactStatus.PENDING) {
            throw new BadRequestException("Friend request is not pending");
        }

        contactRepository.delete(request);
    }

    public List<FriendRequestResponse> getPendingRequestsReceived(Long userId) {
        List<Contact> requests = contactRepository.findPendingRequestsReceived(userId);
        return requests.stream()
                .map(contact -> mapToFriendRequestResponse(contact, contact.getUser()))
                .toList();
    }

    public List<FriendRequestResponse> getPendingRequestsSent(Long userId) {
        List<Contact> requests = contactRepository.findPendingRequestsSent(userId);
        return requests.stream()
                .map(contact -> mapToFriendRequestResponse(contact, contact.getContactUser()))
                .toList();
    }

    private FriendRequestResponse mapToFriendRequestResponse(Contact contact, User otherUser) {
        FriendRequestResponse response = new FriendRequestResponse();
        response.setId(contact.getId());
        response.setUserId(otherUser.getId());
        response.setUsername(otherUser.getUsername());
        response.setFullName(otherUser.getFullName());
        response.setAvatarUrl(otherUser.getAvatarUrl());
        response.setStatus(contact.getStatus().name());
        response.setNickname(contact.getNickname());
        response.setOnline(otherUser.isOnline());
        response.setRequestedAt(contact.getRequestedAt());
        response.setAcceptedAt(contact.getAcceptedAt());
        response.setRejectedAt(contact.getRejectedAt());
        return response;
    }
}

