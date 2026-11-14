package edu.cse3230.ums.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * INTEGRATION TESTS 1-3: the whole application (security filters + controllers +
 * repository + in-memory H2 database). The demo users (admin, teacher, student)
 * are created by DataSeeder when the application context starts.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ---- helper: log in and return the JWT ----
    private String loginAndGetToken(String username, String password) throws Exception {
        String json = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    // Integration test 1: authentication
    @Test
    void login_returnsToken_forValidCredentials_and401_forWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    // Integration test 2: authorization (role-based access control)
    @Test
    void roles_areEnforced_noToken401_wrongRole403_rightRole200() throws Exception {
        String teacherToken = loginAndGetToken("teacher", "teacher123");
        String studentToken = loginAndGetToken("student", "student123");

        // no token at all -> 401
        mockMvc.perform(get("/api/teachers"))
                .andExpect(status().isUnauthorized());

        // teacher is not allowed to see admins -> 403
        mockMvc.perform(get("/api/admins").header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden());

        // teacher may manage teachers -> 200
        mockMvc.perform(get("/api/teachers").header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk());

        // student may NOT manage teachers -> 403, but may manage students -> 200
        mockMvc.perform(get("/api/teachers").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/students").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());
    }

    // Integration test 3: full CRUD cycle as admin
    @Test
    void admin_canCreateReadUpdateAndDeleteATeacher() throws Exception {
        String auth = "Bearer " + loginAndGetToken("admin", "admin123");

        // CREATE -> 201, password is never returned
        String created = mockMvc.perform(post("/api/teachers")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"crud-teacher\",\"password\":\"pass123\","
                                + "\"name\":\"Dr. Test\",\"email\":\"t@uni.edu\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.username").value("crud-teacher"))
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(created, "$.id");

        // READ
        mockMvc.perform(get("/api/teachers/" + id.longValue()).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dr. Test"));

        // UPDATE
        mockMvc.perform(put("/api/teachers/" + id.longValue())
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dr. Updated\",\"email\":\"new@uni.edu\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dr. Updated"))
                .andExpect(jsonPath("$.email").value("new@uni.edu"));

        // DELETE -> 204, then the teacher is gone -> 404
        mockMvc.perform(delete("/api/teachers/" + id.longValue()).header("Authorization", auth))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/teachers/" + id.longValue()).header("Authorization", auth))
                .andExpect(status().isNotFound());
    }
}
