package com.chattrix.api.resources.social;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.requests.AddContactRequest;
import com.chattrix.api.requests.UpdateContactRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.ContactService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/contacts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class ContactResource {

    @Inject private ContactService contactService;
    @Inject private UserContext userContext;

    @GET
    public Response getContacts() {
        var contacts = contactService.getContacts(userContext.getCurrentUserId());
        return Response.ok(ApiResponse.success(contacts, "Contacts retrieved successfully")).build();
    }

    @GET
    @Path("/favorites")
    public Response getFavoriteContacts() {
        var contacts = contactService.getFavoriteContacts(userContext.getCurrentUserId());
        return Response.ok(ApiResponse.success(contacts, "Favorite contacts retrieved successfully")).build();
    }

    @POST
    public Response addContact(@Valid AddContactRequest request) {
        var contact = contactService.addContact(userContext.getCurrentUserId(), request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(contact, "Contact added successfully")).build();
    }

    @PUT
    @Path("/{contactId}")
    public Response updateContact(
            @PathParam("contactId") Long contactId,
            @Valid UpdateContactRequest request) {
        var contact = contactService.updateContact(userContext.getCurrentUserId(), contactId, request);
        return Response.ok(ApiResponse.success(contact, "Contact updated successfully")).build();
    }

    @DELETE
    @Path("/{contactId}")
    public Response deleteContact(@PathParam("contactId") Long contactId) {
        contactService.deleteContact(userContext.getCurrentUserId(), contactId);
        return Response.ok(ApiResponse.success(null, "Contact deleted successfully")).build();
    }
}