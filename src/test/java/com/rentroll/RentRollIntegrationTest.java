package com.rentroll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RentRollIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Root route should redirect unauthenticated users to login")
    void testUnauthenticatedRedirectToLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Login page should be publicly accessible without authentication")
    void testLoginPageAccessible() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Protected endpoint /properties should redirect unauthenticated user to login")
    void testProtectedPropertiesRedirect() throws Exception {
        mockMvc.perform(get("/properties"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "admin@rentroll.in", roles = {"ADMIN"})
    @DisplayName("Admin role should have authorized access to /users")
    void testAdminCanAccessUserManagement() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "landlord@rentroll.in", roles = {"LANDLORD"})
    @DisplayName("Landlord role should be forbidden from accessing /users")
    void testLandlordCannotAccessUserManagement() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());
    }
}

