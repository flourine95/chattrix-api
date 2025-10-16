package com.chattrix.api.resources;

import com.chattrix.api.entities.User;
import com.chattrix.api.filters.Secured;
import com.chattrix.api.filters.UserPrincipal;
import com.chattrix.api.requests.AddContactRequest;
import com.chattrix.api.requests.UpdateContactRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.ContactResponse;
import com.chattrix.api.services.ContactService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;

@Path("/v1/contacts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class ContactResource {

    @Inject
    private ContactService contactService;

    @GET
    public Response getContacts(@Context SecurityContext securityContext) {
        User currentUser = getCurrentUser(securityContext);
        List<ContactResponse> contacts = contactService.getContacts(currentUser.getId());
        return Response.ok(ApiResponse.success(contacts, "Contacts retrieved successfully")).build();
    }

    @GET
    @Path("/favorites")
    public Response getFavoriteContacts(@Context SecurityContext securityContext) {
        User currentUser = getCurrentUser(securityContext);
        List<ContactResponse> contacts = contactService.getFavoriteContacts(currentUser.getId());
        return Response.ok(ApiResponse.success(contacts, "Favorite contacts retrieved successfully")).build();
    }

    @POST
    public Response addContact(@Context SecurityContext securityContext, @Valid AddContactRequest request) {
        User currentUser = getCurrentUser(securityContext);
        ContactResponse contact = contactService.addContact(currentUser.getId(), request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(contact, "Contact added successfully"))
                .build();
    }

    @PUT
    @Path("/{contactId}")
    public Response updateContact(
            @Context SecurityContext securityContext,
            @PathParam("contactId") Long contactId,
            @Valid UpdateContactRequest request) {
        User currentUser = getCurrentUser(securityContext);
        ContactResponse contact = contactService.updateContact(currentUser.getId(), contactId, request);
        return Response.ok(ApiResponse.success(contact, "Contact updated successfully")).build();
    }

    @DELETE
    @Path("/{contactId}")
    public Response deleteContact(@Context SecurityContext securityContext, @PathParam("contactId") Long contactId) {
        User currentUser = getCurrentUser(securityContext);
        contactService.deleteContact(currentUser.getId(), contactId);
        return Response.ok(ApiResponse.success(null, "Contact deleted successfully")).build();
    }

    private User getCurrentUser(SecurityContext securityContext) {
        UserPrincipal userPrincipal = (UserPrincipal) securityContext.getUserPrincipal();
        return userPrincipal.user();
    }
}
